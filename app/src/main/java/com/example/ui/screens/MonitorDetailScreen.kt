package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.IncidentDto
import com.example.data.model.MonitorDetailDto
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.ResponseTimeChart
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.StatusPulseDot
import com.example.ui.components.Uptime90DayBars
import com.example.ui.components.statusColorFor
import com.example.ui.theme.LocalStatusColors
import com.example.ui.viewmodel.MonitorDetailUiState
import com.example.util.TimeFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorDetailScreen(
  monitorId: Int,
  uiState: MonitorDetailUiState,
  onLoadDetail: (Int, String) -> Unit,
  onBack: () -> Unit,
) {
  LaunchedEffect(monitorId) {
    if (uiState.monitorId != monitorId || uiState.detail == null) {
      onLoadDetail(monitorId, uiState.selectedRange)
    }
  }

  val statusColors = LocalStatusColors.current

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = uiState.detail?.name ?: "Monitor #$monitorId",
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("detail_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.action_back),
            )
          }
        },
        actions = {
          IconButton(
            onClick = { onLoadDetail(monitorId, uiState.selectedRange) },
            modifier = Modifier.testTag("detail_refresh_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = stringResource(R.string.action_refresh),
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = statusColors.muted,
          ),
      )
    },
  ) { innerPadding ->
    MonitorDetailContent(
      monitorId = monitorId,
      uiState = uiState,
      onRangeSelected = { range -> onLoadDetail(monitorId, range) },
      onRetry = { onLoadDetail(monitorId, uiState.selectedRange) },
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorDetailContent(
  monitorId: Int,
  uiState: MonitorDetailUiState,
  onRangeSelected: (String) -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current

  LaunchedEffect(monitorId) {
    if (uiState.monitorId != monitorId) {
      onRangeSelected(uiState.selectedRange)
    }
  }

  if (uiState.isLoading && uiState.detail == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
    return
  }

  val detail = uiState.detail
  if (detail == null) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
      ErrorStateCard(
        message = uiState.errorMessage ?: "Unable to load monitor telemetry",
        onRetry = onRetry,
      )
    }
    return
  }

  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("monitor_detail_list"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "hero") {
      MonitorHeroCard(detail)
    }

    item(key = "uptime_grid") {
      UptimeMetricsGrid(detail)
    }

    item(key = "uptime_90d") {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColors.border),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = stringResource(R.string.label_uptime_history),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Uptime90DayBars(
            daily = detail.daily,
            barHeight = 32.dp,
            showEdgeLabels = true,
          )
        }
      }
    }

    item(key = "response_chart") {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("response_chart_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColors.border),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.label_response_time),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            val ranges = listOf("24h", "7d", "30d")
            SingleChoiceSegmentedButtonRow {
              ranges.forEachIndexed { idx, range ->
                SegmentedButton(
                  selected = uiState.selectedRange == range,
                  onClick = { onRangeSelected(range) },
                  shape = SegmentedButtonDefaults.itemShape(index = idx, count = ranges.size),
                  modifier = Modifier.testTag("range_button_$range"),
                ) {
                  Text(text = range, style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            LatencyStatBox(
              label = stringResource(R.string.label_avg),
              value = TimeFormatters.formatLatencyMs(detail.responseSummary.avg),
              modifier = Modifier.weight(1f),
            )
            LatencyStatBox(
              label = stringResource(R.string.label_min),
              value = TimeFormatters.formatLatencyMs(detail.responseSummary.min),
              modifier = Modifier.weight(1f),
            )
            LatencyStatBox(
              label = stringResource(R.string.label_max),
              value = TimeFormatters.formatLatencyMs(detail.responseSummary.max),
              modifier = Modifier.weight(1f),
            )
          }

          ResponseTimeChart(
            points = detail.responseTimes,
            downtimeWindows = detail.downtime,
          )
        }
      }
    }

    item(key = "incidents_header") {
      Text(
        text = stringResource(R.string.label_recent_incidents),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 4.dp),
      )
    }

    if (detail.incidents.isEmpty()) {
      item(key = "no_incidents") {
        EmptyStateCard(
          title = stringResource(R.string.empty_incidents_title),
          subtitle = stringResource(R.string.empty_incidents_subtitle),
          isHealthy = true,
        )
      }
    } else {
      items(detail.incidents, key = { it.id }) { incident ->
        MonitorIncidentItemCard(incident)
      }
    }

    item(key = "footer_space") { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun MonitorHeroCard(detail: MonitorDetailDto) {
  val statusColors = LocalStatusColors.current
  val accent = statusColorFor(detail.status)
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          StatusPulseDot(status = detail.status, size = 12.dp)
          Column {
            Text(
              text = detail.name,
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${detail.type.uppercase()} · checked every ${detail.intervalSeconds}s",
              style = MaterialTheme.typography.bodySmall,
              color = statusColors.muted,
            )
          }
        }
        StatusPillBadge(status = detail.status, label = detail.statusLabel)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        val sinceText = TimeFormatters.formatDurationSince(detail.statusSince)
        val checkedText = TimeFormatters.formatRelativeAgo(detail.lastCheckAt)
        Text(
          text =
            if (sinceText.isNotEmpty()) "State held $sinceText · Last check $checkedText"
            else "Last check $checkedText",
          style = MaterialTheme.typography.bodySmall,
          color = statusColors.muted,
        )
        Text(
          text = TimeFormatters.formatLatencyMs(detail.lastResponseMs),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
private fun UptimeMetricsGrid(detail: MonitorDetailDto) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      UptimeMetricTile(
        label = stringResource(R.string.label_24h),
        value = detail.uptime24h,
        modifier = Modifier.weight(1f),
      )
      UptimeMetricTile(
        label = stringResource(R.string.label_7d),
        value = detail.uptime7d,
        modifier = Modifier.weight(1f),
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      UptimeMetricTile(
        label = stringResource(R.string.label_30d),
        value = detail.uptime30d,
        modifier = Modifier.weight(1f),
      )
      UptimeMetricTile(
        label = stringResource(R.string.label_90d),
        value = detail.uptime,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun UptimeMetricTile(
  label: String,
  value: Double,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current
  val tone =
    when {
      value >= 99.9 -> statusColors.up
      value >= 95.0 -> statusColors.degraded
      else -> statusColors.down
    }

  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, statusColors.border),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = TimeFormatters.formatUptimePct(value),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = tone,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = statusColors.muted,
      )
    }
  }
}

@Composable
private fun LatencyStatBox(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current
  Surface(
    modifier = modifier,
    color = statusColors.surfaceElevated,
    shape = RoundedCornerShape(10.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = statusColors.muted,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
fun MonitorIncidentItemCard(
  incident: IncidentDto,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current
  val accent = if (incident.ongoing) statusColors.down else statusColors.up
  val container = if (incident.ongoing) statusColors.downContainer else statusColors.upContainer
  val icon = if (incident.ongoing) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
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
          modifier = Modifier.padding(8.dp).size(18.dp),
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (incident.ongoing) "Ongoing outage" else "Resolved · ${incident.duration}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = TimeFormatters.formatShortDateTime(incident.startedAt),
            style = MaterialTheme.typography.labelSmall,
            color = statusColors.muted,
          )
        }
        if (incident.reason.isNotBlank()) {
          Text(
            text = incident.reason,
            style = MaterialTheme.typography.labelSmall,
            color = statusColors.muted,
          )
        }
      }
    }
  }
}
