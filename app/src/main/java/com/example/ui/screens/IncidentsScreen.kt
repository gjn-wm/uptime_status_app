package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.IncidentDto
import com.example.data.model.MonitorSummaryDto
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.theme.LocalStatusColors
import com.example.ui.viewmodel.IncidentStatusFilter
import com.example.ui.viewmodel.IncidentsUiState
import com.example.util.TimeFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentsScreen(
  uiState: IncidentsUiState,
  monitors: List<MonitorSummaryDto>,
  onRefresh: () -> Unit,
  onSelectMonitorFilter: (Int?) -> Unit,
  onSelectStatusFilter: (IncidentStatusFilter) -> Unit,
  onIncidentClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current

  LaunchedEffect(Unit) {
    if (uiState.incidents.isEmpty() && !uiState.isLoading) {
      onRefresh()
    }
  }

  if (uiState.isLoading && uiState.incidents.isEmpty()) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
          text = "Aggregating 30-day incident log…",
          style = MaterialTheme.typography.bodyMedium,
          color = statusColors.muted,
        )
      }
    }
    return
  }

  val filtered = uiState.filteredIncidents

  PullToRefreshBox(
    isRefreshing = uiState.isLoading,
    onRefresh = onRefresh,
    modifier = modifier.fillMaxSize(),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("incidents_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(key = "incident_summary") {
        val totalDowntimeSec = filtered.sumOf { it.durationSeconds }
        val ongoingCount = filtered.count { it.ongoing }
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, statusColors.border),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            IncidentStatColumn(
              label = "Events (30d)",
              value = "${filtered.size}",
              accent = MaterialTheme.colorScheme.onSurface,
            )
            IncidentStatColumn(
              label = "Ongoing",
              value = "$ongoingCount",
              accent = if (ongoingCount > 0) statusColors.down else statusColors.up,
            )
            IncidentStatColumn(
              label = "Downtime",
              value = TimeFormatters.formatDurationSeconds(totalDowntimeSec),
              accent = statusColors.degraded,
            )
          }
        }
      }

      item(key = "status_filter_row") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(IncidentStatusFilter.entries) { filter ->
            FilterChip(
              selected = uiState.statusFilter == filter,
              onClick = { onSelectStatusFilter(filter) },
              label = { Text(filter.label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
          }
        }
      }

      if (monitors.isNotEmpty()) {
        item(key = "monitor_filter_row") {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "all_monitors") {
              FilterChip(
                selected = uiState.selectedMonitorId == null,
                onClick = { onSelectMonitorFilter(null) },
                label = { Text(stringResource(R.string.filter_all)) },
              )
            }
            items(monitors, key = { it.id }) { monitor ->
              FilterChip(
                selected = uiState.selectedMonitorId == monitor.id,
                onClick = {
                  val next = if (uiState.selectedMonitorId == monitor.id) null else monitor.id
                  onSelectMonitorFilter(next)
                },
                label = { Text(monitor.name) },
              )
            }
          }
        }
      }

      if (uiState.errorMessage != null) {
        item(key = "incidents_error") {
          ErrorStateCard(message = uiState.errorMessage, onRetry = onRefresh)
        }
      }

      if (filtered.isEmpty()) {
        item(key = "incidents_empty") {
          EmptyStateCard(
            title = stringResource(R.string.empty_incidents_title),
            subtitle = stringResource(R.string.empty_incidents_subtitle),
            isHealthy = true,
          )
        }
      } else {
        items(filtered, key = { it.id }) { incident ->
          IncidentFeedCard(
            incident = incident,
            onClick = { onIncidentClick(incident.monitorId) },
          )
        }
      }

      item(key = "bottom_space") { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}

@Composable
private fun IncidentStatColumn(
  label: String,
  value: String,
  accent: androidx.compose.ui.graphics.Color,
) {
  val statusColors = LocalStatusColors.current
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      color = accent,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = statusColors.muted,
    )
  }
}

@Composable
private fun IncidentFeedCard(
  incident: IncidentDto,
  onClick: () -> Unit,
) {
  val statusColors = LocalStatusColors.current
  val accent = if (incident.ongoing) statusColors.down else statusColors.up
  val container = if (incident.ongoing) statusColors.downContainer else statusColors.upContainer
  val icon = if (incident.ongoing) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .testTag("incident_card_${incident.id}")
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, statusColors.border),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(color = container, shape = CircleShape) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.padding(9.dp).size(20.dp),
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = incident.monitor,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            color = if (incident.ongoing) statusColors.downContainer else statusColors.surfaceElevated,
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text =
                if (incident.ongoing) "ONGOING"
                else incident.duration.ifBlank {
                  TimeFormatters.formatDurationSeconds(incident.durationSeconds)
                },
              style = MaterialTheme.typography.labelSmall,
              color = if (incident.ongoing) statusColors.down else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }

        if (incident.reason.isNotBlank()) {
          Text(
            text = incident.reason,
            style = MaterialTheme.typography.labelMedium,
            color = statusColors.degraded,
          )
        }

        Text(
          text =
            "Started ${TimeFormatters.formatShortDateTime(incident.startedAt)} · ${TimeFormatters.formatRelativeAgo(incident.startedAt)}",
          style = MaterialTheme.typography.bodySmall,
          color = statusColors.muted,
        )
      }

      Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = "Inspect ${incident.monitor}",
        tint = statusColors.muted,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}
