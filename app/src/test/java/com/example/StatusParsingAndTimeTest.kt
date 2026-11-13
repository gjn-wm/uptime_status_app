package com.example

import com.example.data.model.MonitorDetailDto
import com.example.data.model.StatusOverviewDto
import com.example.util.TimeFormatters
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusParsingAndTimeTest {

  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  @Test
  fun `parses overview json payload`() {
    val json =
      """
      {
        "status": "up",
        "headline": "All systems operational",
        "updated_at": "2026-11-13T11:33:42.062670Z",
        "counts": { "up": 6, "down": 0, "paused": 0, "maintenance": 0, "pending": 0 },
        "monitors": [
          {
            "id": 5,
            "name": "HRMS MCP",
            "group": "",
            "type": "http",
            "status": "up",
            "status_label": "Operational",
            "uptime": 100.0,
            "uptime_30d": 100.0,
            "interval_seconds": 60,
            "last_check_at": "2026-11-13T11:33:38.435533Z",
            "last_response_ms": 90.103,
            "status_since": "2026-11-02T07:18:39.527851Z",
            "daily": [
              { "date": "2026-11-13", "uptime": 100.0, "level": "up" }
            ]
          }
        ],
        "announcements": [],
        "maintenance": []
      }
      """.trimIndent()

    val adapter = moshi.adapter(StatusOverviewDto::class.java)
    val parsed = adapter.fromJson(json)
    assertNotNull(parsed)
    assertEquals("up", parsed?.status)
    assertEquals(6, parsed?.counts?.up)
    assertEquals(1, parsed?.monitors?.size)
    assertEquals("HRMS MCP", parsed?.monitors?.first()?.name)
  }

  @Test
  fun `parses monitor detail json payload`() {
    val json =
      """
      {
        "id": 2,
        "name": "ITMSU",
        "group": "",
        "type": "http",
        "status": "up",
        "status_label": "Operational",
        "uptime": 87.513,
        "uptime_24h": 98.8,
        "uptime_7d": 91.2,
        "uptime_30d": 87.513,
        "interval_seconds": 60,
        "response_times": [
          { "t": "2026-11-12T11:36:30.167438Z", "ms": 261.2, "failed": 0 }
        ],
        "downtime": [
          { "start": "2026-11-13T06:01:49.982336Z", "end": "2026-11-13T06:18:07.136729Z", "reason": "HTTP 530 <none>", "ongoing": false }
        ],
        "response_summary": { "avg": 330.1, "min": 212.9, "max": 710.5 },
        "incidents": [
          { "id": 65, "monitor_id": 2, "monitor": "ITMSU", "started_at": "2026-11-13T06:01:49.982336Z", "ended_at": "2026-11-13T06:18:07.136729Z", "ongoing": false, "duration_seconds": 977, "duration": "16m 17s", "reason": "HTTP 530 <none>" }
        ]
      }
      """.trimIndent()

    val adapter = moshi.adapter(MonitorDetailDto::class.java)
    val parsed = adapter.fromJson(json)
    assertNotNull(parsed)
    assertEquals(2, parsed?.id)
    assertEquals(1, parsed?.incidents?.size)
    assertEquals("16m 17s", parsed?.incidents?.first()?.duration)
  }

  @Test
  fun `formats microsecond UTC timestamps and durations`() {
    val millis = TimeFormatters.parseUtcMillis("2026-11-13T11:33:42.062670Z")
    assertNotNull(millis)
    assertTrue((millis ?: 0L) > 0L)
    assertEquals("16m 17s", TimeFormatters.formatDurationSeconds(977L))
    assertEquals("99.880%", TimeFormatters.formatUptimePct(99.88))
  }
}
