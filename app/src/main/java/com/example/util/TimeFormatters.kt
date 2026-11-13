package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object TimeFormatters {

  private val isoFormats =
    listOf(
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )

  fun parseUtcMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    val normalized =
      iso.replace(Regex("""(\.\d{3})\d+Z$"""), "$1Z")
    for (pattern in isoFormats) {
      try {
        val sdf = SimpleDateFormat(pattern, Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }
        val parsed = sdf.parse(normalized)
        if (parsed != null) return parsed.time
      } catch (_: Exception) {
      }
    }
    return null
  }

  fun formatLocalClock(iso: String?): String {
    val millis = parseUtcMillis(iso) ?: return "--:--:--"
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
  }

  fun formatShortDateTime(iso: String?): String {
    val millis = parseUtcMillis(iso) ?: return iso.orEmpty()
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
  }

  fun formatChartTimestamp(iso: String?): String {
    val millis = parseUtcMillis(iso) ?: return ""
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
  }

  fun formatRelativeAgo(iso: String?, nowMillis: Long = System.currentTimeMillis()): String {
    val millis = parseUtcMillis(iso) ?: return "unknown"
    val diffSec = ((nowMillis - millis) / 1000L).coerceAtLeast(0L)
    return when {
      diffSec < 60L -> "${diffSec}s ago"
      diffSec < 3600L -> "${diffSec / 60L}m ago"
      diffSec < 86400L -> {
        val h = diffSec / 3600L
        val m = (diffSec % 3600L) / 60L
        if (m > 0) "${h}h ${m}m ago" else "${h}h ago"
      }
      else -> {
        val d = diffSec / 86400L
        val h = (diffSec % 86400L) / 3600L
        if (h > 0) "${d}d ${h}h ago" else "${d}d ago"
      }
    }
  }

  fun formatDurationSince(iso: String?, nowMillis: Long = System.currentTimeMillis()): String {
    val millis = parseUtcMillis(iso) ?: return ""
    val diffSec = abs(nowMillis - millis) / 1000L
    val days = diffSec / 86400L
    val hours = (diffSec % 86400L) / 3600L
    val mins = (diffSec % 3600L) / 60L
    return when {
      days > 0 -> "${days}d ${hours}h"
      hours > 0 -> "${hours}h ${mins}m"
      mins > 0 -> "${mins}m"
      else -> "${diffSec}s"
    }
  }

  fun formatDurationSeconds(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0s"
    val days = totalSeconds / 86400L
    val hours = (totalSeconds % 86400L) / 3600L
    val mins = (totalSeconds % 3600L) / 60L
    val secs = totalSeconds % 60L
    return buildList {
      if (days > 0) add("${days}d")
      if (hours > 0) add("${hours}h")
      if (mins > 0) add("${mins}m")
      if (secs > 0 && days == 0L) add("${secs}s")
    }.joinToString(" ")
  }

  fun formatUptimePct(value: Double): String = String.format(Locale.US, "%.3f%%", value)

  fun formatLatencyMs(ms: Double?): String {
    if (ms == null) return "—"
    return "${ms.toInt()} ms"
  }
}
