package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.IncidentDto
import com.example.data.model.MonitorDetailDto
import com.example.data.model.MonitorSummaryDto
import com.example.data.model.StatusOverviewDto
import com.example.data.preferences.ThemePreference
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.StatusRepository
import com.example.notifications.OutageNotificationManager
import com.example.util.AlertAudioPlayer
import com.example.worker.StatusMonitorScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OverviewFilter(val label: String) {
  ALL("All"),
  OPERATIONAL("Operational"),
  ISSUES("Issues"),
}

enum class IncidentStatusFilter(val label: String) {
  ALL("All"),
  ONGOING("Ongoing"),
  RESOLVED("Resolved"),
}

data class OverviewUiState(
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val overview: StatusOverviewDto? = null,
  val errorMessage: String? = null,
  val searchQuery: String = "",
  val filter: OverviewFilter = OverviewFilter.ALL,
  val countdownSeconds: Int = 60,
  val lastSyncLocalMillis: Long = 0L,
  val selectedMonitorIdForSplit: Int? = null,
) {
  val filteredMonitors: List<MonitorSummaryDto>
    get() {
      val list = overview?.monitors.orEmpty()
      return list.filter { monitor ->
        val matchesQuery =
          searchQuery.isBlank() ||
            monitor.name.contains(searchQuery, ignoreCase = true) ||
            monitor.group.contains(searchQuery, ignoreCase = true)
        val matchesFilter =
          when (filter) {
            OverviewFilter.ALL -> true
            OverviewFilter.OPERATIONAL -> monitor.status.equals("up", ignoreCase = true)
            OverviewFilter.ISSUES -> !monitor.status.equals("up", ignoreCase = true)
          }
        matchesQuery && matchesFilter
      }
    }

  val aggregate90dUptime: Double?
    get() {
      val list = overview?.monitors.orEmpty()
      if (list.isEmpty()) return null
      return list.map { it.uptime }.average()
    }
}

data class IncidentsUiState(
  val isLoading: Boolean = false,
  val incidents: List<IncidentDto> = emptyList(),
  val errorMessage: String? = null,
  val selectedMonitorId: Int? = null,
  val statusFilter: IncidentStatusFilter = IncidentStatusFilter.ALL,
) {
  val filteredIncidents: List<IncidentDto>
    get() =
      incidents.filter { incident ->
        val matchesMonitor = selectedMonitorId == null || incident.monitorId == selectedMonitorId
        val matchesStatus =
          when (statusFilter) {
            IncidentStatusFilter.ALL -> true
            IncidentStatusFilter.ONGOING -> incident.ongoing
            IncidentStatusFilter.RESOLVED -> !incident.ongoing
          }
        matchesMonitor && matchesStatus
      }
}

data class MonitorDetailUiState(
  val monitorId: Int? = null,
  val isLoading: Boolean = false,
  val detail: MonitorDetailDto? = null,
  val selectedRange: String = "24h",
  val errorMessage: String? = null,
)

class StatusViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = StatusRepository()
  private val prefsRepository = UserPreferencesRepository(application)

  val userPreferences: StateFlow<UserPreferences> =
    prefsRepository.preferences.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = UserPreferences(),
    )

  private val _overviewState = MutableStateFlow(OverviewUiState())
  val overviewState: StateFlow<OverviewUiState> = _overviewState.asStateFlow()

  private val _incidentsState = MutableStateFlow(IncidentsUiState())
  val incidentsState: StateFlow<IncidentsUiState> = _incidentsState.asStateFlow()

  private val _detailState = MutableStateFlow(MonitorDetailUiState())
  val detailState: StateFlow<MonitorDetailUiState> = _detailState.asStateFlow()

  private var pollingJob: Job? = null
  private var detailJob: Job? = null

  init {
    OutageNotificationManager.ensureChannel(application)
    refreshOverview(isManual = false)
    observePollingCadence()
    observeBackgroundWorkerSchedule()
  }

  private fun observeBackgroundWorkerSchedule() {
    viewModelScope.launch {
      userPreferences
        .map { it.pushNotificationsEnabled to it.backgroundIntervalMinutes }
        .distinctUntilChanged()
        .collectLatest { (enabled, intervalMinutes) ->
          StatusMonitorScheduler.syncSchedule(
            context = getApplication(),
            enabled = enabled,
            intervalMinutes = intervalMinutes,
          )
        }
    }
  }

  private fun observePollingCadence() {
    viewModelScope.launch {
      userPreferences
        .map { it.refreshIntervalSeconds }
        .distinctUntilChanged()
        .collectLatest { interval ->
          pollingJob?.cancel()
          if (interval <= 0) {
            _overviewState.update { it.copy(countdownSeconds = 0) }
            return@collectLatest
          }
          pollingJob =
            launch {
              var remaining = interval
              while (true) {
                _overviewState.update { it.copy(countdownSeconds = remaining) }
                delay(1_000L)
                remaining--
                if (remaining <= 0) {
                  refreshOverview(isManual = false)
                  val activeDetailId = _detailState.value.monitorId
                  if (activeDetailId != null) {
                    loadMonitorDetail(activeDetailId, _detailState.value.selectedRange, silent = true)
                  }
                  remaining = interval
                }
              }
            }
        }
    }
  }

  fun refreshOverview(isManual: Boolean = true) {
    viewModelScope.launch {
      _overviewState.update {
        it.copy(
          isLoading = it.overview == null,
          isRefreshing = isManual || it.overview != null,
          errorMessage = null,
        )
      }
      repository
        .fetchOverview()
        .onSuccess { data ->
          handleMonitorTransitions(data)
          val defaultSplitId =
            _overviewState.value.selectedMonitorIdForSplit ?: data.monitors.firstOrNull()?.id
          val resetCountdown = userPreferences.value.refreshIntervalSeconds
          _overviewState.update {
            it.copy(
              isLoading = false,
              isRefreshing = false,
              overview = data,
              errorMessage = null,
              lastSyncLocalMillis = System.currentTimeMillis(),
              countdownSeconds = if (resetCountdown > 0) resetCountdown else 0,
              selectedMonitorIdForSplit = defaultSplitId,
            )
          }
          if (_incidentsState.value.incidents.isEmpty() && !_incidentsState.value.isLoading) {
            loadIncidents(data.monitors.map { it.id })
          }
        }
        .onFailure { err ->
          _overviewState.update {
            it.copy(
              isLoading = false,
              isRefreshing = false,
              errorMessage = err.localizedMessage ?: "Connection failed",
            )
          }
        }
    }
  }

  private suspend fun handleMonitorTransitions(data: StatusOverviewDto) {
    val prefs = prefsRepository.preferences.first()
    val previousStatuses = prefs.lastKnownMonitorStatuses
    val newStatuses = data.monitors.associate { it.id to it.status.lowercase() }

    val newlyDownMonitors =
      data.monitors.filter { monitor ->
        val curr = monitor.status.lowercase()
        val prev = previousStatuses[monitor.id]
        curr == "down" && prev != "down"
      }

    val recoveredMonitors =
      data.monitors.filter { monitor ->
        val curr = monitor.status.lowercase()
        val prev = previousStatuses[monitor.id]
        curr == "up" && prev == "down"
      }

    if (newlyDownMonitors.isNotEmpty()) {
      if (prefs.alertSoundEnabled) {
        viewModelScope.launch { AlertAudioPlayer.playOutageChime() }
      }
      if (prefs.pushNotificationsEnabled) {
        val appContext = getApplication<Application>()
        for (monitor in newlyDownMonitors) {
          val reason =
            runCatching {
              repository.fetchMonitorDetail(monitor.id, "24h").getOrNull()?.incidents?.firstOrNull()?.reason
            }.getOrNull()
          OutageNotificationManager.notifyServerDown(appContext, monitor, reason)
        }
      }
    }

    if (recoveredMonitors.isNotEmpty() && prefs.pushNotificationsEnabled && prefs.notifyOnRecovery) {
      val appContext = getApplication<Application>()
      for (monitor in recoveredMonitors) {
        OutageNotificationManager.notifyServerRecovered(appContext, monitor)
      }
    }

    prefsRepository.saveLastKnownMonitorStatuses(newStatuses)
  }

  fun updateSearchQuery(query: String) {
    _overviewState.update { it.copy(searchQuery = query) }
  }

  fun updateOverviewFilter(filter: OverviewFilter) {
    _overviewState.update { it.copy(filter = filter) }
  }

  fun selectMonitorForSplit(monitorId: Int) {
    _overviewState.update { it.copy(selectedMonitorIdForSplit = monitorId) }
    loadMonitorDetail(monitorId, _detailState.value.selectedRange)
  }

  fun loadIncidents(explicitMonitorIds: List<Int>? = null) {
    viewModelScope.launch {
      val ids =
        explicitMonitorIds ?: _overviewState.value.overview?.monitors?.map { it.id }.orEmpty()
      if (ids.isEmpty()) {
        val overviewResult = repository.fetchOverview()
        val fetchedIds = overviewResult.getOrNull()?.monitors?.map { it.id }.orEmpty()
        if (fetchedIds.isEmpty()) {
          _incidentsState.update {
            it.copy(
              isLoading = false,
              errorMessage = overviewResult.exceptionOrNull()?.localizedMessage ?: "Unable to load monitors",
            )
          }
          return@launch
        }
        fetchIncidentsForIds(fetchedIds)
      } else {
        fetchIncidentsForIds(ids)
      }
    }
  }

  private suspend fun fetchIncidentsForIds(ids: List<Int>) {
    _incidentsState.update { it.copy(isLoading = true, errorMessage = null) }
    repository
      .fetchAllIncidents(ids)
      .onSuccess { list ->
        _incidentsState.update {
          it.copy(isLoading = false, incidents = list, errorMessage = null)
        }
      }
      .onFailure { err ->
        _incidentsState.update {
          it.copy(isLoading = false, errorMessage = err.localizedMessage ?: "Failed to load incidents")
        }
      }
  }

  fun setIncidentMonitorFilter(monitorId: Int?) {
    _incidentsState.update { it.copy(selectedMonitorId = monitorId) }
  }

  fun setIncidentStatusFilter(filter: IncidentStatusFilter) {
    _incidentsState.update { it.copy(statusFilter = filter) }
  }

  fun loadMonitorDetail(
    monitorId: Int,
    range: String = _detailState.value.selectedRange,
    silent: Boolean = false,
  ) {
    detailJob?.cancel()
    detailJob =
      viewModelScope.launch {
        val isSameMonitor = _detailState.value.monitorId == monitorId
        _detailState.update {
          it.copy(
            monitorId = monitorId,
            selectedRange = range,
            isLoading = !silent && (!isSameMonitor || it.detail == null),
            detail = if (isSameMonitor) it.detail else null,
            errorMessage = null,
          )
        }
        repository
          .fetchMonitorDetail(monitorId, range)
          .onSuccess { detail ->
            _detailState.update {
              it.copy(isLoading = false, detail = detail, errorMessage = null)
            }
          }
          .onFailure { err ->
            _detailState.update {
              it.copy(
                isLoading = false,
                errorMessage = err.localizedMessage ?: "Unable to load telemetry",
              )
            }
          }
      }
  }

  fun setThemePreference(theme: ThemePreference) {
    viewModelScope.launch { prefsRepository.setThemePreference(theme) }
  }

  fun setRefreshIntervalSeconds(seconds: Int) {
    viewModelScope.launch { prefsRepository.setRefreshIntervalSeconds(seconds) }
  }

  fun setAlertSoundEnabled(enabled: Boolean) {
    viewModelScope.launch {
      prefsRepository.setAlertSoundEnabled(enabled)
      if (enabled) {
        AlertAudioPlayer.playOutageChime()
      }
    }
  }

  fun setPushNotificationsEnabled(enabled: Boolean) {
    viewModelScope.launch { prefsRepository.setPushNotificationsEnabled(enabled) }
  }

  fun setNotifyOnRecovery(enabled: Boolean) {
    viewModelScope.launch { prefsRepository.setNotifyOnRecovery(enabled) }
  }

  fun setBackgroundIntervalMinutes(minutes: Long) {
    viewModelScope.launch { prefsRepository.setBackgroundIntervalMinutes(minutes) }
  }

  fun dismissNotificationBanner() {
    viewModelScope.launch { prefsRepository.setNotificationBannerDismissed(true) }
  }

  fun testAlertChime() {
    viewModelScope.launch { AlertAudioPlayer.playOutageChime() }
  }

  fun sendTestPushNotification() {
    val firstMonitor = _overviewState.value.overview?.monitors?.firstOrNull()
    OutageNotificationManager.sendTestOutageAlert(
      context = getApplication(),
      sampleMonitorId = firstMonitor?.id ?: 2,
      sampleName = firstMonitor?.name ?: "ITMSU",
    )
  }
}
