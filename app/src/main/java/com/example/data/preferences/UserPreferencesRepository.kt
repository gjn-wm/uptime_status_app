package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
)

class UserPreferencesRepository(private val context: Context) {

  private object Keys {
    val THEME = stringPreferencesKey("theme_preference")
    val REFRESH_SECONDS = intPreferencesKey("refresh_interval_seconds")
    val ALERT_SOUND = booleanPreferencesKey("alert_sound_enabled")
  }

  val preferences: Flow<UserPreferences> =
    context.dataStore.data.map { prefs ->
      val theme =
        prefs[Keys.THEME]?.let { raw ->
          ThemePreference.entries.firstOrNull { it.name == raw }
        } ?: ThemePreference.DARK
      val refresh = prefs[Keys.REFRESH_SECONDS] ?: 60
      val alertSound = prefs[Keys.ALERT_SOUND] ?: false
      UserPreferences(
        themePreference = theme,
        refreshIntervalSeconds = refresh,
        alertSoundEnabled = alertSound,
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
}
