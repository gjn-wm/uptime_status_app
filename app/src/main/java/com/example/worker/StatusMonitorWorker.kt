package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.StatusRepository
import com.example.notifications.OutageNotificationManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class StatusMonitorWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val prefsRepo = UserPreferencesRepository(applicationContext)
    val prefs = prefsRepo.preferences.first()
    if (!prefs.pushNotificationsEnabled || !OutageNotificationManager.hasNotificationPermission(applicationContext)) {
      return Result.success()
    }

    val repository = StatusRepository()
    val overviewResult = repository.fetchOverview()
    val overview = overviewResult.getOrElse {
      return if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    val previous = prefs.lastKnownMonitorStatuses
    val updated = overview.monitors.associate { it.id to it.status.lowercase() }

    for (monitor in overview.monitors) {
      val curr = monitor.status.lowercase()
      val prev = previous[monitor.id]
      if (curr == "down" && prev != "down") {
        val reason =
          runCatching {
            repository.fetchMonitorDetail(monitor.id, "24h").getOrNull()?.incidents?.firstOrNull()?.reason
          }.getOrNull()
        OutageNotificationManager.notifyServerDown(applicationContext, monitor, reason)
      } else if (curr == "up" && prev == "down" && prefs.notifyOnRecovery) {
        OutageNotificationManager.notifyServerRecovered(applicationContext, monitor)
      }
    }

    prefsRepo.saveLastKnownMonitorStatuses(updated)
    return Result.success()
  }
}

object StatusMonitorScheduler {
  private const val UNIQUE_WORK_NAME = "gjnwm_background_status_monitor"

  fun syncSchedule(
    context: Context,
    enabled: Boolean,
    intervalMinutes: Long,
  ) {
    val workManager = WorkManager.getInstance(context)
    if (!enabled || intervalMinutes <= 0L) {
      workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
      return
    }

    val safeMinutes = intervalMinutes.coerceAtLeast(15L)
    val constraints =
      Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val request =
      PeriodicWorkRequestBuilder<StatusMonitorWorker>(safeMinutes, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .build()

    workManager.enqueueUniquePeriodicWork(
      UNIQUE_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      request,
    )
  }
}
