package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyUptimeDto
import com.example.data.model.DowntimeWindowDto
import com.example.data.model.IncidentDto
import com.example.data.model.MonitorSummaryDto
import com.example.data.model.ResponsePointDto
import com.example.data.model.StatusOverviewDto
import com.example.ui.theme.LocalStatusColors
import com.example.util.TimeFormatters
import kotlin.math.roundToInt

@Composable
fun statusColorFor(status: String): Color {
  val colors = LocalStatusColors.current
  return when (status.lowercase()) {
    "up", "operational" -> colors.up
    "up-partial" -> colors.upPartial
    "down", "major" -> colors.down
    "degraded", "warning", "partial" -> colors.degraded
    "maintenance" -> colors.maintenance
    else -> colors.noData
  }
}

@Composable
fun statusContainerColorFor(status: String): Color {
  val colors = LocalStatusColors.current
  return when (status.lowercase()) {
    "up", "operational" -> colors.upContainer
    "down", "major" -> colors.downContainer
    "degraded", "warning", "partial" -> colors.degradedContainer
    "maintenance" -> colors.maintenanceContainer
    else -> colors.surfaceElevated
  }
}

@Composable
fun StatusPulseDot(
  status: String,
  modifier: Modifier = Modifier,
  size: Dp = 10.dp,
  animate: Boolean = true,
) {
  val color = statusColorFor(status)
  val transition = rememberInfiniteTransition(label = "status_pulse")
  val pulseScale by
    transition.animateFloat(
      initialValue = 1f,
      targetValue = 2.1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "pulse_scale",
    )
  val pulseAlpha by
    transition.animateFloat(
      initialValue = 0.45f,
      targetValue = 0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "pulse_alpha",
    )

  Box(modifier = modifier.size(size * 2.2f), contentAlignment = Alignment.Center) {
    if (animate && status.lowercase() in setOf("up", "down", "degraded", "warning")) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
          color = color.copy(alpha = pulseAlpha),
          radius = (size.toPx() / 2f) * pulseScale,
        )
      }
    }
    Box(
      modifier =
        Modifier.size(size).clip(CircleShape).background(color)
    )
  }
}

@Composable
fun StatusPillBadge(
  status: String,
  label: String,
  modifier: Modifier = Modifier,
) {
  val accent = statusColorFor(status)
  val container = statusContainerColorFor(status)
  Surface(
    modifier = modifier,
    color = container,
    shape = RoundedCornerShape(999.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(accent))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = accent,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroStatusBanner(
  overview: StatusOverviewDto,
  aggregate90dUptime: Double?,
  latestIncident: IncidentDto?,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current
  val accent = statusColorFor(overview.status)

  Card(
    modifier = modifier.fillMaxWidth().testTag("hero_status_banner"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Image(
        painter = painterResource(id = R.drawable.img_noc_banner),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = if (statusColors.isDark) 0.38f else 0.10f,
        modifier = Modifier.matchParentSize(),
      )
      Box(
        modifier =
          Modifier.matchParentSize()
            .background(
              Brush.horizontalGradient(
                colors =
                  listOf(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    accent.copy(alpha = 0.14f),
                  )
              )
            )
      )

      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
          ) {
            StatusPulseDot(status = overview.status, size = 14.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = overview.headline,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  "Synced ${TimeFormatters.formatLocalClock(overview.updatedAt)} · ${overview.monitors.size} endpoints",
                style = MaterialTheme.typography.bodySmall,
                color = statusColors.muted,
              )
            }
          }
          if (aggregate90dUptime != null) {
            Surface(
              color = statusContainerColorFor(overview.status),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End,
              ) {
                Text(
                  text = TimeFormatters.formatUptimePct(aggregate90dUptime),
                  style = MaterialTheme.typography.titleMedium,
                  color = accent,
                )
                Text(
                  text = stringResource(R.string.label_90d_uptime),
                  style = MaterialTheme.typography.labelSmall,
                  color = statusColors.muted,
                )
              }
            }
          }
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          MetricMiniPill(
            label = "${overview.counts.up} Up",
            color = statusColors.up,
            container = statusColors.upContainer,
          )
          if (overview.counts.down > 0) {
            MetricMiniPill(
              label = "${overview.counts.down} Down",
              color = statusColors.down,
              container = statusColors.downContainer,
            )
          }
          if (overview.counts.maintenance > 0) {
            MetricMiniPill(
              label = "${overview.counts.maintenance} Maint",
              color = statusColors.maintenance,
              container = statusColors.maintenanceContainer,
            )
          }
          if (overview.counts.paused > 0) {
            MetricMiniPill(
              label = "${overview.counts.paused} Paused",
              color = statusColors.muted,
              container = statusColors.surfaceElevated,
            )
          }
          if (latestIncident != null) {
            val ageText = TimeFormatters.formatRelativeAgo(latestIncident.startedAt)
            val durText = latestIncident.duration.ifBlank {
              TimeFormatters.formatDurationSeconds(latestIncident.durationSeconds)
            }
            MetricMiniPill(
              label = "Latest incident $ageText · $durText",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              container = statusColors.surfaceElevated,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MetricMiniPill(
  label: String,
  color: Color,
  container: Color,
) {
  Surface(
    color = container,
    shape = RoundedCornerShape(8.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
  }
}

@Composable
fun Uptime90DayBars(
  daily: List<DailyUptimeDto>,
  modifier: Modifier = Modifier,
  barHeight: Dp = 26.dp,
  showEdgeLabels: Boolean = true,
) {
  val statusColors = LocalStatusColors.current
  var selectedIndex by remember(daily) { mutableIntStateOf(-1) }
  val displayBars = remember(daily) { daily.takeLast(90) }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    if (displayBars.isEmpty()) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(statusColors.noData.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "No daily uptime records yet",
          style = MaterialTheme.typography.labelSmall,
          color = statusColors.muted,
        )
      }
      return@Column
    }

    val colorMap =
      displayBars.map { entry ->
        when (entry.level.lowercase()) {
          "up" -> statusColors.up
          "up-partial" -> statusColors.upPartial
          "degraded", "warning" -> statusColors.degraded
          "down" -> statusColors.down
          "maintenance" -> statusColors.maintenance
          else -> statusColors.noData
        }
      }

    Canvas(
      modifier =
        Modifier.fillMaxWidth()
          .height(barHeight)
          .pointerInput(displayBars) {
            detectTapGestures { offset ->
              val idx = ((offset.x / size.width) * displayBars.size).toInt().coerceIn(0, displayBars.lastIndex)
              selectedIndex = if (selectedIndex == idx) -1 else idx
            }
          }
          .pointerInput(displayBars) {
            detectHorizontalDragGestures(
              onDragEnd = {},
              onHorizontalDrag = { change, _ ->
                change.consume()
                val idx =
                  ((change.position.x / size.width) * displayBars.size)
                    .toInt()
                    .coerceIn(0, displayBars.lastIndex)
                selectedIndex = idx
              },
            )
          }
    ) {
      val count = displayBars.size
      val gap = 1.6.dp.toPx()
      val totalGap = gap * (count - 1).coerceAtLeast(0)
      val barWidth = ((size.width - totalGap) / count).coerceAtLeast(1.5f)
      val radius = (barWidth / 2f).coerceAtMost(3.dp.toPx())

      for (i in displayBars.indices) {
        val x = i * (barWidth + gap)
        val isSelected = i == selectedIndex
        val topOffset = if (isSelected) 0f else 2.dp.toPx()
        val drawHeight = size.height - topOffset
        drawRoundRect(
          color = if (selectedIndex >= 0 && !isSelected) colorMap[i].copy(alpha = 0.6f) else colorMap[i],
          topLeft = Offset(x, topOffset),
          size = Size(barWidth, drawHeight),
          cornerRadius = CornerRadius(radius, radius),
        )
      }
    }

    AnimatedVisibility(visible = selectedIndex in displayBars.indices) {
      val entry = displayBars.getOrNull(selectedIndex)
      if (entry != null) {
        val levelLabel =
          when (entry.level.lowercase()) {
            "up" -> "Operational"
            "up-partial" -> "Minor dip"
            "degraded", "warning" -> "Degraded"
            "down" -> "Outage"
            "maintenance" -> "Maintenance"
            else -> "No records"
          }
        val pctText = entry.uptime?.let { TimeFormatters.formatUptimePct(it) } ?: "—"
        Surface(
          color = statusColors.surfaceElevated,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, statusColors.border),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = entry.date,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "$levelLabel · $pctText",
              style = MaterialTheme.typography.labelSmall,
              color = statusColorFor(entry.level),
            )
          }
        }
      }
    }

    if (showEdgeLabels) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = displayBars.first().date,
          style = MaterialTheme.typography.labelSmall,
          color = statusColors.muted,
        )
        Text(
          text = "Today",
          style = MaterialTheme.typography.labelSmall,
          color = statusColors.muted,
        )
      }
    }
  }
}

@Composable
fun MonitorSummaryCard(
  monitor: MonitorSummaryDto,
  isSelected: Boolean = false,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current
  val accent = statusColorFor(monitor.status)
  val borderColor =
    if (isSelected) MaterialTheme.colorScheme.primary else statusColors.border

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("monitor_card_${monitor.id}")
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isSelected) statusColors.surfaceElevated else MaterialTheme.colorScheme.surface
      ),
    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          StatusPulseDot(status = monitor.status, size = 10.dp)
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = monitor.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Surface(
                color = statusColors.surfaceElevated,
                shape = RoundedCornerShape(6.dp),
              ) {
                Text(
                  text = monitor.type.uppercase(),
                  style = MaterialTheme.typography.labelSmall,
                  color = statusColors.muted,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
            val sinceText = TimeFormatters.formatDurationSince(monitor.statusSince)
            val checkedAgo = TimeFormatters.formatRelativeAgo(monitor.lastCheckAt)
            Text(
              text =
                if (sinceText.isNotEmpty()) "${monitor.statusLabel} $sinceText · checked $checkedAgo"
                else "Checked $checkedAgo",
              style = MaterialTheme.typography.bodySmall,
              color = statusColors.muted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = TimeFormatters.formatUptimePct(monitor.uptime),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = accent,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = null,
                tint = statusColors.muted,
                modifier = Modifier.size(12.dp),
              )
              Text(
                text = TimeFormatters.formatLatencyMs(monitor.lastResponseMs),
                style = MaterialTheme.typography.labelSmall,
                color = statusColors.muted,
              )
            }
          }
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Open ${monitor.name} details",
            tint = statusColors.muted,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Uptime90DayBars(
        daily = monitor.daily,
        barHeight = 20.dp,
        showEdgeLabels = false,
      )
    }
  }
}

@Composable
fun ResponseTimeChart(
  points: List<ResponsePointDto>,
  downtimeWindows: List<DowntimeWindowDto>,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current
  val lineColor = statusColors.up
  val downColor = statusColors.down
  val gridColor = statusColors.border

  var touchedIndex by remember(points) { mutableIntStateOf(-1) }

  if (points.isEmpty()) {
    Box(
      modifier =
        modifier
          .fillMaxWidth()
          .height(170.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(statusColors.surfaceElevated),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "No latency samples available for this window",
        style = MaterialTheme.typography.bodySmall,
        color = statusColors.muted,
      )
    }
    return
  }

  val validMs = points.mapNotNull { it.ms }
  val maxMs = (validMs.maxOrNull() ?: 100.0).coerceAtLeast(10.0) * 1.12
  val minMs = (validMs.minOrNull() ?: 0.0).coerceAtLeast(0.0) * 0.85
  val span = (maxMs - minMs).coerceAtLeast(1.0)

  val tStart = TimeFormatters.parseUtcMillis(points.first().t) ?: 0L
  val tEnd = TimeFormatters.parseUtcMillis(points.last().t) ?: (tStart + 1L)
  val tSpan = (tEnd - tStart).coerceAtLeast(1L)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(180.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(statusColors.surfaceElevated.copy(alpha = 0.55f))
          .border(1.dp, statusColors.border, RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
      Canvas(
        modifier =
          Modifier.fillMaxSize()
            .pointerInput(points) {
              detectTapGestures { offset ->
                val idx = ((offset.x / size.width) * points.size).toInt().coerceIn(0, points.lastIndex)
                touchedIndex = if (touchedIndex == idx) -1 else idx
              }
            }
            .pointerInput(points) {
              detectHorizontalDragGestures { change, _ ->
                change.consume()
                val idx =
                  ((change.position.x / size.width) * points.size)
                    .toInt()
                    .coerceIn(0, points.lastIndex)
                touchedIndex = idx
              }
            }
      ) {
        val w = size.width
        val h = size.height

        // Dashed horizontal grid lines
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        for (row in 0..3) {
          val y = h * (row / 3f)
          drawLine(
            color = gridColor.copy(alpha = 0.65f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dash,
          )
        }

        // Downtime shaded vertical bands
        for (window in downtimeWindows) {
          val ws = TimeFormatters.parseUtcMillis(window.start) ?: continue
          val we = TimeFormatters.parseUtcMillis(window.end) ?: tEnd
          val x1 = (((ws - tStart).toFloat() / tSpan.toFloat()) * w).coerceIn(0f, w)
          val x2 = (((we - tStart).toFloat() / tSpan.toFloat()) * w).coerceIn(x1 + 3.dp.toPx(), w)
          drawRect(
            color = downColor.copy(alpha = 0.22f),
            topLeft = Offset(x1, 0f),
            size = Size((x2 - x1).coerceAtLeast(3.dp.toPx()), h),
          )
        }

        // Build latency line + fill paths
        val strokePath = Path()
        val fillPath = Path()
        var started = false
        var lastX = 0f

        points.forEachIndexed { index, pt ->
          val x =
            if (points.size <= 1) w / 2f
            else (index.toFloat() / points.lastIndex.toFloat()) * w
          val msVal = pt.ms
          if (msVal != null && pt.failed == 0) {
            val normalizedY = ((msVal - minMs) / span).toFloat().coerceIn(0f, 1f)
            val y = h - (normalizedY * h)
            if (!started) {
              strokePath.moveTo(x, y)
              fillPath.moveTo(x, h)
              fillPath.lineTo(x, y)
              started = true
            } else {
              strokePath.lineTo(x, y)
              fillPath.lineTo(x, y)
            }
            lastX = x
          } else if (pt.failed > 0) {
            drawLine(
              color = downColor.copy(alpha = 0.7f),
              start = Offset(x, 0f),
              end = Offset(x, h),
              strokeWidth = 2.dp.toPx(),
            )
            if (started) {
              fillPath.lineTo(lastX, h)
              fillPath.close()
              started = false
            }
          }
        }

        if (started) {
          fillPath.lineTo(lastX, h)
          fillPath.close()
        }

        drawPath(
          path = fillPath,
          brush =
            Brush.verticalGradient(
              colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.01f))
            ),
        )
        drawPath(
          path = strokePath,
          color = lineColor,
          style =
            Stroke(
              width = 2.2.dp.toPx(),
              cap = StrokeCap.Round,
              join = StrokeJoin.Round,
            ),
        )

        // Touch crosshair
        if (touchedIndex in points.indices) {
          val pt = points[touchedIndex]
          val x =
            if (points.size <= 1) w / 2f
            else (touchedIndex.toFloat() / points.lastIndex.toFloat()) * w
          drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1.2.dp.toPx(),
          )
          val msVal = pt.ms
          if (msVal != null) {
            val normalizedY = ((msVal - minMs) / span).toFloat().coerceIn(0f, 1f)
            val y = h - (normalizedY * h)
            drawCircle(
              color = if (pt.failed > 0) downColor else lineColor,
              radius = 5.dp.toPx(),
              center = Offset(x, y),
            )
            drawCircle(
              color = Color.White,
              radius = 2.5.dp.toPx(),
              center = Offset(x, y),
            )
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = TimeFormatters.formatChartTimestamp(points.first().t),
        style = MaterialTheme.typography.labelSmall,
        color = statusColors.muted,
      )
      val selectedPoint = points.getOrNull(touchedIndex)
      if (selectedPoint != null) {
        val msLabel =
          if (selectedPoint.failed > 0) "Failed check"
          else "${selectedPoint.ms?.roundToInt() ?: 0} ms"
        Text(
          text = "${TimeFormatters.formatChartTimestamp(selectedPoint.t)} · $msLabel",
          style = MaterialTheme.typography.labelSmall,
          color = if (selectedPoint.failed > 0) downColor else lineColor,
        )
      } else {
        Text(
          text = "Tap or drag chart to inspect",
          style = MaterialTheme.typography.labelSmall,
          color = statusColors.muted,
        )
      }
      Text(
        text = TimeFormatters.formatChartTimestamp(points.last().t),
        style = MaterialTheme.typography.labelSmall,
        color = statusColors.muted,
      )
    }
  }
}

@Composable
fun ErrorStateCard(
  message: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current
  Card(
    modifier = modifier.fillMaxWidth().testTag("error_state_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, statusColors.down.copy(alpha = 0.4f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        color = statusColors.downContainer,
        shape = CircleShape,
      ) {
        Icon(
          imageVector = Icons.Rounded.CloudOff,
          contentDescription = null,
          tint = statusColors.down,
          modifier = Modifier.padding(14.dp).size(28.dp),
        )
      }
      Text(
        text = stringResource(R.string.error_network_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = statusColors.muted,
      )
      Button(
        onClick = onRetry,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier = Modifier.testTag("retry_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Refresh,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.action_retry))
      }
    }
  }
}

@Composable
fun EmptyStateCard(
  title: String,
  subtitle: String,
  isHealthy: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalStatusColors.current
  val icon = if (isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline
  val accent = if (isHealthy) statusColors.up else statusColors.muted
  val container = if (isHealthy) statusColors.upContainer else statusColors.surfaceElevated

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, statusColors.border),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(color = container, shape = CircleShape) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.padding(14.dp).size(28.dp),
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = statusColors.muted,
      )
    }
  }
}
