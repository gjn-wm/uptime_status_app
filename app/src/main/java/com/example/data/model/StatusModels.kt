package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatusOverviewDto(
  val status: String = "up",
  val headline: String = "All systems operational",
  @Json(name = "updated_at") val updatedAt: String = "",
  val counts: StatusCountsDto = StatusCountsDto(),
  val monitors: List<MonitorSummaryDto> = emptyList(),
  val announcements: List<AnnouncementDto> = emptyList(),
  val maintenance: List<MaintenanceDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class StatusCountsDto(
  val up: Int = 0,
  val down: Int = 0,
  val paused: Int = 0,
  val maintenance: Int = 0,
  val pending: Int = 0,
)

@JsonClass(generateAdapter = true)
data class MonitorSummaryDto(
  val id: Int,
  val name: String,
  val group: String = "",
  val type: String = "http",
  val status: String = "up",
  @Json(name = "status_label") val statusLabel: String = "Operational",
  val uptime: Double = 100.0,
  @Json(name = "uptime_30d") val uptime30d: Double = 100.0,
  @Json(name = "interval_seconds") val intervalSeconds: Int = 60,
  @Json(name = "last_check_at") val lastCheckAt: String? = null,
  @Json(name = "last_response_ms") val lastResponseMs: Double? = null,
  @Json(name = "status_since") val statusSince: String? = null,
  val daily: List<DailyUptimeDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DailyUptimeDto(
  val date: String,
  val uptime: Double? = null,
  val level: String = "nodata",
)

@JsonClass(generateAdapter = true)
data class AnnouncementDto(
  val id: Int? = null,
  val title: String? = null,
  val message: String? = null,
  val body: String? = null,
  val type: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class MaintenanceDto(
  val id: Int? = null,
  val title: String? = null,
  val message: String? = null,
  val description: String? = null,
  @Json(name = "starts_at") val startsAt: String? = null,
  @Json(name = "ends_at") val endsAt: String? = null,
  val status: String? = null,
)

@JsonClass(generateAdapter = true)
data class MonitorDetailDto(
  val id: Int,
  val name: String,
  val group: String = "",
  val type: String = "http",
  val status: String = "up",
  @Json(name = "status_label") val statusLabel: String = "Operational",
  val uptime: Double = 100.0,
  @Json(name = "uptime_24h") val uptime24h: Double = 100.0,
  @Json(name = "uptime_7d") val uptime7d: Double = 100.0,
  @Json(name = "uptime_30d") val uptime30d: Double = 100.0,
  @Json(name = "interval_seconds") val intervalSeconds: Int = 60,
  @Json(name = "last_check_at") val lastCheckAt: String? = null,
  @Json(name = "last_response_ms") val lastResponseMs: Double? = null,
  @Json(name = "status_since") val statusSince: String? = null,
  val daily: List<DailyUptimeDto> = emptyList(),
  @Json(name = "response_times") val responseTimes: List<ResponsePointDto> = emptyList(),
  val downtime: List<DowntimeWindowDto> = emptyList(),
  @Json(name = "response_summary") val responseSummary: ResponseSummaryDto = ResponseSummaryDto(),
  val incidents: List<IncidentDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ResponsePointDto(
  val t: String,
  val ms: Double? = null,
  val failed: Int = 0,
)

@JsonClass(generateAdapter = true)
data class DowntimeWindowDto(
  val start: String,
  val end: String? = null,
  val reason: String? = null,
  val ongoing: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ResponseSummaryDto(
  val avg: Double? = null,
  val min: Double? = null,
  val max: Double? = null,
)

@JsonClass(generateAdapter = true)
data class IncidentDto(
  val id: Int,
  @Json(name = "monitor_id") val monitorId: Int,
  val monitor: String,
  @Json(name = "started_at") val startedAt: String,
  @Json(name = "ended_at") val endedAt: String? = null,
  val ongoing: Boolean = false,
  @Json(name = "duration_seconds") val durationSeconds: Long = 0L,
  val duration: String = "",
  val reason: String = "",
)
