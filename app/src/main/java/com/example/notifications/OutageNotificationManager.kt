package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.MonitorSummaryDto

object OutageNotificationManager {

  const val CHANNEL_ID = "gjnwm_outage_alerts"
  const val EXTRA_MONITOR_ID = "extra_monitor_id"

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel =
          NotificationChannel(
            CHANNEL_ID,
            "Server Outage Alerts",
            NotificationManager.IMPORTANCE_HIGH,
          ).apply {
            description =
              "High-priority notifications when monitored servers on status.gjnwm.dpdns.org go down or recover."
            enableLights(true)
            lightColor = Color.parseColor("#DF484A")
            enableVibration(true)
          }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
  }

  @SuppressLint("MissingPermission")
  fun notifyServerDown(
    context: Context,
    monitor: MonitorSummaryDto,
    reason: String? = null,
  ) {
    if (!hasNotificationPermission(context)) return
    ensureChannel(context)

    val title = "Server Down: ${monitor.name}"
    val body =
      if (!reason.isNullOrBlank()) {
        "${monitor.name} is ${monitor.statusLabel.lowercase()} ($reason). Tap to inspect telemetry."
      } else {
        "${monitor.name} (${monitor.type.uppercase()}) is unreachable. Tap to inspect telemetry."
      }

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_pulse)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ERROR)
        .setColor(Color.parseColor("#DF484A"))
        .setAutoCancel(true)
        .setContentIntent(createMonitorPendingIntent(context, monitor.id))
        .build()

    NotificationManagerCompat.from(context).notify(10_000 + monitor.id, notification)
  }

  @SuppressLint("MissingPermission")
  fun notifyServerRecovered(
    context: Context,
    monitor: MonitorSummaryDto,
  ) {
    if (!hasNotificationPermission(context)) return
    ensureChannel(context)

    val title = "Server Recovered: ${monitor.name}"
    val latencyText = monitor.lastResponseMs?.let { " · ${it.toInt()} ms" } ?: ""
    val body = "${monitor.name} is back operational$latencyText."

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_pulse)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setColor(Color.parseColor("#3BD671"))
        .setAutoCancel(true)
        .setContentIntent(createMonitorPendingIntent(context, monitor.id))
        .build()

    NotificationManagerCompat.from(context).notify(10_000 + monitor.id, notification)
  }

  @SuppressLint("MissingPermission")
  fun sendTestOutageAlert(context: Context, sampleMonitorId: Int = 2, sampleName: String = "ITMSU") {
    if (!hasNotificationPermission(context)) return
    ensureChannel(context)

    val title = "Test Outage Alert: $sampleName"
    val body =
      "Push alerts are active. You will be notified immediately if any Gjn WM server goes down."

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_pulse)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setColor(Color.parseColor("#DF484A"))
        .setAutoCancel(true)
        .setContentIntent(createMonitorPendingIntent(context, sampleMonitorId))
        .build()

    NotificationManagerCompat.from(context).notify(99_001, notification)
  }

  private fun createMonitorPendingIntent(context: Context, monitorId: Int): PendingIntent {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_MONITOR_ID, monitorId)
      }
    return PendingIntent.getActivity(
      context,
      monitorId,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
