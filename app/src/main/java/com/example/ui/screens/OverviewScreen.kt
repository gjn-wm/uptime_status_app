package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnnouncementDto
import com.example.data.model.IncidentDto
import com.example.data.model.MaintenanceDto
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.HeroStatusBanner
import com.example.ui.components.MonitorSummaryCard
import com.example.ui.theme.LocalStatusColors
import com.example.ui.viewmodel.MonitorDetailUiState
import com.example.ui.viewmodel.OverviewFilter
import com.example.ui.viewmodel.OverviewUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
  uiState: OverviewUiState,
  detailState: MonitorDetailUiState,
  latestIncident: IncidentDto?,
  onRefresh: () -> Unit,
  onSearchChange: (String) -> Unit,
  onFilterChange: (OverviewFilter) -> Unit,
  onMonitorClick: (Int) -> Unit,
  onSelectSplitMonitor: (Int) -> Unit,
  onDetailRangeSelected: (Int, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWideSplitLayout = maxWidth >= 840.dp

    if (uiState.isLoading && uiState.overview == null) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          Text(
            text = "Connecting to status.gjnwm.dpdns.org…",
            style = MaterialTheme.typography.bodyMedium,
            color = statusColors.muted,
          )
        }
      }
      return@BoxWithConstraints
    }

    val listContent: @Composable (Modifier) -> Unit = { listMod ->
      PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = listMod,
      ) {
        LazyColumn(
          modifier = Modifier.fillMaxSize().testTag("overview_monitor_list"),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          val overview = uiState.overview
          if (overview != null) {
            item(key = "hero_banner") {
              HeroStatusBanner(
                overview = overview,
                aggregate90dUptime = uiState.aggregate90dUptime,
                latestIncident = latestIncident,
              )
            }

            if (overview.announcements.isNotEmpty()) {
              items(overview.announcements, key = { "ann_${it.id ?: it.hashCode()}" }) { ann ->
                AnnouncementBannerCard(ann)
              }
            }

            if (overview.maintenance.isNotEmpty()) {
              items(overview.maintenance, key = { "maint_${it.id ?: it.hashCode()}" }) { maint ->
                MaintenanceBannerCard(maint)
              }
            }

            item(key = "search_and_filter") {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = uiState.searchQuery,
                  onValueChange = onSearchChange,
                  placeholder = {
                    Text(
                      text = stringResource(R.string.search_monitors_hint),
                      style = MaterialTheme.typography.bodyMedium,
                    )
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Rounded.Search,
                      contentDescription = null,
                      tint = statusColors.muted,
                    )
                  },
                  trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                      IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                          imageVector = Icons.Rounded.Close,
                          contentDescription = "Clear search",
                          tint = statusColors.muted,
                        )
                      }
                    }
                  },
                  singleLine = true,
                  shape = RoundedCornerShape(14.dp),
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = MaterialTheme.colorScheme.primary,
                      unfocusedBorderColor = statusColors.border,
                      focusedContainerColor = MaterialTheme.colorScheme.surface,
                      unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                  modifier = Modifier.fillMaxWidth().testTag("search_monitors_input"),
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(OverviewFilter.entries) { filter ->
                      val selected = uiState.filter == filter
                      FilterChip(
                        selected = selected,
                        onClick = { onFilterChange(filter) },
                        label = { Text(text = filter.label) },
                        colors =
                          FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                          ),
                        modifier = Modifier.testTag("overview_filter_${filter.name.lowercase()}"),
                      )
                    }
                  }

                  Surface(
                    onClick = onRefresh,
                    color = statusColors.surfaceElevated,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, statusColors.border),
                    modifier = Modifier.testTag("countdown_refresh_chip"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                      )
                      Text(
                        text =
                          if (uiState.countdownSeconds > 0) "${uiState.countdownSeconds}s"
                          else "Manual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }

            if (uiState.errorMessage != null) {
              item(key = "inline_error") {
                ErrorStateCard(message = uiState.errorMessage, onRetry = onRefresh)
              }
            }

            val monitors = uiState.filteredMonitors
            if (monitors.isEmpty()) {
              item(key = "empty_monitors") {
                EmptyStateCard(
                  title = stringResource(R.string.empty_monitors_title),
                  subtitle = stringResource(R.string.empty_monitors_subtitle),
                  isHealthy = false,
                )
              }
            } else {
              items(monitors, key = { it.id }) { monitor ->
                MonitorSummaryCard(
                  monitor = monitor,
                  isSelected = isWideSplitLayout && uiState.selectedMonitorIdForSplit == monitor.id,
                  onClick = {
                    if (isWideSplitLayout) onSelectSplitMonitor(monitor.id)
                    else onMonitorClick(monitor.id)
                  },
                )
              }
            }

            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(12.dp)) }
          } else if (uiState.errorMessage != null) {
            item(key = "fatal_error") {
              ErrorStateCard(message = uiState.errorMessage, onRetry = onRefresh)
            }
          }
        }
      }
    }

    if (isWideSplitLayout) {
      Row(modifier = Modifier.fillMaxSize()) {
        listContent(Modifier.weight(0.46f).fillMaxHeight())
        val selectedId = uiState.selectedMonitorIdForSplit
        if (selectedId != null) {
          MonitorDetailContent(
            monitorId = selectedId,
            uiState = detailState,
            onRangeSelected = { range -> onDetailRangeSelected(selectedId, range) },
            onRetry = { onDetailRangeSelected(selectedId, detailState.selectedRange) },
            modifier = Modifier.weight(0.54f).fillMaxHeight(),
          )
        }
      }
    } else {
      listContent(Modifier.fillMaxSize())
    }
  }
}

@Composable
private fun AnnouncementBannerCard(announcement: AnnouncementDto) {
  val statusColors = LocalStatusColors.current
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = statusColors.maintenanceContainer),
    border = BorderStroke(1.dp, statusColors.maintenance.copy(alpha = 0.4f)),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Icon(
        imageVector = Icons.Rounded.Campaign,
        contentDescription = null,
        tint = statusColors.maintenance,
        modifier = Modifier.size(20.dp),
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = announcement.title ?: stringResource(R.string.label_announcements),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        val body = announcement.message ?: announcement.body
        if (!body.isNullOrBlank()) {
          Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun MaintenanceBannerCard(maintenance: MaintenanceDto) {
  val statusColors = LocalStatusColors.current
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = statusColors.degradedContainer),
    border = BorderStroke(1.dp, statusColors.degraded.copy(alpha = 0.4f)),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Icon(
        imageVector = Icons.Rounded.Construction,
        contentDescription = null,
        tint = statusColors.degraded,
        modifier = Modifier.size(20.dp),
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = maintenance.title ?: stringResource(R.string.label_maintenance),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        val desc = maintenance.message ?: maintenance.description
        if (!desc.isNullOrBlank()) {
          Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
