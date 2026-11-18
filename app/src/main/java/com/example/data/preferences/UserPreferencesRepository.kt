package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gjnwm_status_prefs")

enum class ThemePreference(val label: String) {
  DARK("Dark NOC"),
  LIGHT("Light Crisp"),
  SYSTEM("System"),
}

data class UserPreferences(
  val themePreference: ThemePreference = ThemePreference.DARK,
  val refreshIntervalSeconds: Int = 60,
  val alertSoundEnabled: Boolean = false,
  val pushNotificationsEnabled: Boolean = true,
  val notifyOnRecovery: Boolean = true,
  val backgroundIntervalMinutes: Long = 15L,
  val notificationBannerDismissed: Boolean = false,
  val lastKnownMonitorStatuses: Map<Int, String> = emptyMap(),
)

class UserPreferencesRepository(private val context: Context) {

  private object Keys {
    val THEME = stringPreferencesKey("theme_preference")
    val REFRESH_SECONDS = intPreferencesKey("refresh_interval_seconds")
    val ALERT_SOUND = booleanPreferencesKey("alert_sound_enabled")
    val PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications_enabled")
    val NOTIFY_RECOVERY = booleanPreferencesKey("notify_on_recovery")
    val BACKGROUND_INTERVAL_MIN = longPreferencesKey("background_interval_minutes")
    val BANNER_DISMISSED = booleanPreferencesKey("notification_banner_dismissed")
    val LAST_STATUSES = stringPreferencesKey("last_known_monitor_statuses")
  }

  val preferences: Flow<UserPreferences> =
    context.dataStore.data.map { prefs ->
      val theme =
        prefs[Keys.THEME]?.let { raw ->
          ThemePreference.entries.firstOrNull { it.name == raw }
        } ?: ThemePreference.DARK
      val refresh = prefs[Keys.REFRESH_SECONDS] ?: 60
      val alertSound = prefs[Keys.ALERT_SOUND] ?: false
      val pushEnabled = prefs[Keys.PUSH_NOTIFICATIONS] ?: true
      val notifyRecovery = prefs[Keys.NOTIFY_RECOVERY] ?: true
      val bgMinutes = prefs[Keys.BACKGROUND_INTERVAL_MIN] ?: 15L
      val bannerDismissed = prefs[Keys.BANNER_DISMISSED] ?: false
      val lastStatuses = decodeStatuses(prefs[Keys.LAST_STATUSES].orEmpty())

      UserPreferences(
        themePreference = theme,
        refreshIntervalSeconds = refresh,
        alertSoundEnabled = alertSound,
        pushNotificationsEnabled = pushEnabled,
        notifyOnRecovery = notifyRecovery,
        backgroundIntervalMinutes = bgMinutes,
        notificationBannerDismissed = bannerDismissed,
        lastKnownMonitorStatuses = lastStatuses,
      )
    }

  suspend fun setThemePreference(theme: ThemePreference) {
    context.dataStore.edit { it[Keys.THEME] = theme.name }
  }

  suspend fun setRefreshIntervalSeconds(seconds: Int) {
    context.dataStore.edit { it[Keys.REFRESH_SECONDS] = seconds }
  }

  suspend fun setAlertSoundEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.ALERT_SOUND] = enabled }
  }

  suspend fun setPushNotificationsEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.PUSH_NOTIFICATIONS] = enabled }
  }

  suspend fun setNotifyOnRecovery(enabled: Boolean) {
    context.dataStore.edit { it[Keys.NOTIFY_RECOVERY] = enabled }
  }

  suspend fun setBackgroundIntervalMinutes(minutes: Long) {
    context.dataStore.edit { it[Keys.BACKGROUND_INTERVAL_MIN] = minutes }
  }

  suspend fun setNotificationBannerDismissed(dismissed: Boolean) {
    context.dataStore.edit { it[Keys.BANNER_DISMISSED] = dismissed }
  }

  suspend fun saveLastKnownMonitorStatuses(statuses: Map<Int, String>) {
    context.dataStore.edit { it[Keys.LAST_STATUSES] = encodeStatuses(statuses) }
  }

  companion object {
    fun encodeStatuses(statuses: Map<Int, String>): String =
      statuses.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }

    fun decodeStatuses(raw: String): Map<Int, String> {
      if (raw.isBlank()) return emptyMap()
      return raw.split(",").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) {
          val id = parts[0].toIntOrNull()
          val status = parts[1].trim()
          if (id != null && status.isNotEmpty()) id to status else null
        } else null
      }.toMap()
    }
  }
}
