package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.ThemePreference
import com.example.data.preferences.UserPreferences
import com.example.data.remote.StatusApiService
import com.example.ui.theme.LocalStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  preferences: UserPreferences,
  lastUpdatedUtc: String?,
  monitorCount: Int,
  onThemeChange: (ThemePreference) -> Unit,
  onRefreshIntervalChange: (Int) -> Unit,
  onAlertSoundToggle: (Boolean) -> Unit,
  onTestAlertChime: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val statusColors = LocalStatusColors.current

  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("settings_list"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "source_card") {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColors.border),
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Image(
            painter = painterResource(id = R.drawable.img_noc_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (statusColors.isDark) 0.4f else 0.12f,
            modifier = Modifier.matchParentSize(),
          )
          Box(
            modifier =
              Modifier.matchParentSize()
                .background(
                  Brush.verticalGradient(
                    colors =
                      listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                      )
                  )
                )
          )
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = stringResource(R.string.settings_source_title),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = stringResource(R.string.brand_title),
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${StatusApiService.BASE_URL} · $monitorCount active monitors",
              style = MaterialTheme.typography.bodySmall,
              color = statusColors.muted,
            )
            if (!lastUpdatedUtc.isNullOrBlank()) {
              Text(
                text = "API snapshot: $lastUpdatedUtc",
                style = MaterialTheme.typography.labelSmall,
                color = statusColors.muted,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(StatusApiService.BASE_URL))
                  context.startActivity(intent)
                },
                modifier = Modifier.weight(1f).testTag("open_web_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Web Page", style = MaterialTheme.typography.labelLarge)
              }
              OutlinedButton(
                onClick = {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(StatusApiService.RSS_URL))
                  context.startActivity(intent)
                },
                modifier = Modifier.weight(1f).testTag("open_rss_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.RssFeed,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("RSS Feed", style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      }
    }

    item(key = "theme_card") {
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Palette,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = stringResource(R.string.settings_appearance_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          val themes = ThemePreference.entries
          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            themes.forEachIndexed { idx, theme ->
              SegmentedButton(
                selected = preferences.themePreference == theme,
                onClick = { onThemeChange(theme) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = themes.size),
                modifier = Modifier.testTag("theme_option_${theme.name.lowercase()}"),
              ) {
                Text(text = theme.label, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }
    }

    item(key = "refresh_card") {
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Timer,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = stringResource(R.string.settings_refresh_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          val intervals = listOf(30 to "30s", 60 to "60s", 120 to "2m", 0 to "Manual")
          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            intervals.forEachIndexed { idx, (seconds, label) ->
              SegmentedButton(
                selected = preferences.refreshIntervalSeconds == seconds,
                onClick = { onRefreshIntervalChange(seconds) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = intervals.size),
                modifier = Modifier.testTag("refresh_option_$seconds"),
              ) {
                Text(text = label, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }
    }

    item(key = "alert_card") {
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
              Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = stringResource(R.string.settings_alerts_title),
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = stringResource(R.string.settings_alerts_desc),
                  style = MaterialTheme.typography.bodySmall,
                  color = statusColors.muted,
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
              checked = preferences.alertSoundEnabled,
              onCheckedChange = onAlertSoundToggle,
              modifier = Modifier.testTag("alert_sound_switch"),
            )
          }

          OutlinedButton(
            onClick = onTestAlertChime,
            modifier = Modifier.fillMaxWidth().testTag("test_chime_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.VolumeUp,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.action_test_sound))
          }
        }
      }
    }

    item(key = "bottom_space") { Spacer(modifier = Modifier.height(12.dp)) }
  }
}
