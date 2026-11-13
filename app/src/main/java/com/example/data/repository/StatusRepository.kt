package com.example.data.repository

import com.example.data.model.IncidentDto
import com.example.data.model.MonitorDetailDto
import com.example.data.model.StatusOverviewDto
import com.example.data.remote.StatusApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class StatusRepository(private val api: StatusApiService = StatusApiService.create()) {

  suspend fun fetchOverview(): Result<StatusOverviewDto> =
    runCatching { api.getStatusOverview() }

  suspend fun fetchMonitorDetail(monitorId: Int, range: String): Result<MonitorDetailDto> =
    runCatching { api.getMonitorDetail(monitorId = monitorId, range = range) }

  suspend fun fetchAllIncidents(monitorIds: List<Int>): Result<List<IncidentDto>> =
    runCatching {
      coroutineScope {
        monitorIds
          .distinct()
          .map { id ->
            async {
              runCatching { api.getMonitorDetail(monitorId = id, range = "30d").incidents }
                .getOrDefault(emptyList())
            }
          }
          .awaitAll()
          .flatten()
          .distinctBy { it.id }
          .sortedByDescending { it.startedAt }
      }
    }
}
