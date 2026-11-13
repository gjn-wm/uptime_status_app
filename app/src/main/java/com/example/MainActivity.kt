package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.preferences.ThemePreference
import com.example.ui.components.StatusPulseDot
import com.example.ui.screens.IncidentsScreen
import com.example.ui.screens.MonitorDetailScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.GjnWmStatusTheme
import com.example.ui.theme.LocalStatusColors
import com.example.ui.viewmodel.StatusViewModel

private data class TopLevelTab(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

private val topLevelTabs =
  listOf(
    TopLevelTab(
      route = "overview",
      labelRes = R.string.nav_services,
      selectedIcon = Icons.Rounded.Dns,
      unselectedIcon = Icons.Outlined.Dns,
    ),
    TopLevelTab(
      route = "incidents",
      labelRes = R.string.nav_incidents,
      selectedIcon = Icons.Rounded.History,
      unselectedIcon = Icons.Outlined.History,
    ),
    TopLevelTab(
      route = "settings",
      labelRes = R.string.nav_settings,
      selectedIcon = Icons.Rounded.Tune,
      unselectedIcon = Icons.Outlined.Tune,
    ),
  )

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: StatusViewModel = viewModel()
      val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
      val systemDark = isSystemInDarkTheme()
      val isDark =
        when (preferences.themePreference) {
          ThemePreference.DARK -> true
          ThemePreference.LIGHT -> false
          ThemePreference.SYSTEM -> systemDark
        }

      LaunchedEffect(isDark) {
        enableEdgeToEdge(
          statusBarStyle =
            if (isDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
          navigationBarStyle =
            if (isDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
      }

      GjnWmStatusTheme(themePreference = preferences.themePreference) {
        GjnWmStatusApp(viewModel = viewModel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GjnWmStatusApp(viewModel: StatusViewModel) {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route

  val overviewState by viewModel.overviewState.collectAsStateWithLifecycle()
  val incidentsState by viewModel.incidentsState.collectAsStateWithLifecycle()
  val detailState by viewModel.detailState.collectAsStateWithLifecycle()
  val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()

  val statusColors = LocalStatusColors.current
  val isTopLevel = currentRoute in topLevelTabs.map { it.route }
  val ongoingCount = incidentsState.incidents.count { it.ongoing }

  BoxWithConstraints(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    val useRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useRail && isTopLevel) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxHeight(),
        ) {
          topLevelTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationRailItem(
              selected = selected,
              onClick = {
                navController.navigate(tab.route) {
                  popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                TabIconWithBadge(
                  tab = tab,
                  selected = selected,
                  ongoingCount = if (tab.route == "incidents") ongoingCount else 0,
                )
              },
              label = { Text(stringResource(tab.labelRes)) },
              modifier = Modifier.testTag("nav_rail_${tab.route}"),
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
          if (isTopLevel) {
            TopAppBar(
              title = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(34.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      StatusPulseDot(
                        status = overviewState.overview?.status ?: "up",
                        size = 9.dp,
                      )
                    }
                  }
                  Column {
                    Text(
                      text = stringResource(R.string.brand_title),
                      style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                      text = stringResource(R.string.brand_subtitle),
                      style = MaterialTheme.typography.labelSmall,
                      color = statusColors.muted,
                    )
                  }
                }
              },
              actions = {
                IconButton(
                  onClick = {
                    viewModel.refreshOverview(isManual = true)
                    if (currentRoute == "incidents") {
                      viewModel.loadIncidents()
                    }
                  },
                  modifier = Modifier.testTag("topbar_refresh_button"),
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
                  actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
          }
        },
        bottomBar = {
          if (!useRail && isTopLevel) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 0.dp,
            ) {
              topLevelTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                  selected = selected,
                  onClick = {
                    navController.navigate(tab.route) {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  icon = {
                    TabIconWithBadge(
                      tab = tab,
                      selected = selected,
                      ongoingCount = if (tab.route == "incidents") ongoingCount else 0,
                    )
                  },
                  label = { Text(stringResource(tab.labelRes)) },
                  modifier = Modifier.testTag("nav_bar_${tab.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        NavHost(
          navController = navController,
          startDestination = "overview",
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
          composable("overview") {
            OverviewScreen(
              uiState = overviewState,
              detailState = detailState,
              latestIncident = incidentsState.incidents.firstOrNull(),
              onRefresh = { viewModel.refreshOverview(isManual = true) },
              onSearchChange = viewModel::updateSearchQuery,
              onFilterChange = viewModel::updateOverviewFilter,
              onMonitorClick = { monitorId ->
                viewModel.loadMonitorDetail(monitorId)
                navController.navigate("monitor/$monitorId")
              },
              onSelectSplitMonitor = viewModel::selectMonitorForSplit,
              onDetailRangeSelected = { id, range -> viewModel.loadMonitorDetail(id, range) },
            )
          }

          composable("incidents") {
            IncidentsScreen(
              uiState = incidentsState,
              monitors = overviewState.overview?.monitors.orEmpty(),
              onRefresh = { viewModel.loadIncidents() },
              onSelectMonitorFilter = viewModel::setIncidentMonitorFilter,
              onSelectStatusFilter = viewModel::setIncidentStatusFilter,
              onIncidentClick = { monitorId ->
                viewModel.loadMonitorDetail(monitorId)
                navController.navigate("monitor/$monitorId")
              },
            )
          }

          composable("settings") {
            SettingsScreen(
              preferences = preferences,
              lastUpdatedUtc = overviewState.overview?.updatedAt,
              monitorCount = overviewState.overview?.monitors?.size ?: 0,
              onThemeChange = viewModel::setThemePreference,
              onRefreshIntervalChange = viewModel::setRefreshIntervalSeconds,
              onAlertSoundToggle = viewModel::setAlertSoundEnabled,
              onTestAlertChime = viewModel::testAlertChime,
            )
          }

          composable(
            route = "monitor/{monitorId}",
            arguments = listOf(navArgument("monitorId") { type = NavType.IntType }),
          ) { entry ->
            val monitorId = entry.arguments?.getInt("monitorId") ?: return@composable
            MonitorDetailScreen(
              monitorId = monitorId,
              uiState = detailState,
              onLoadDetail = { id, range -> viewModel.loadMonitorDetail(id, range) },
              onBack = { navController.popBackStack() },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TabIconWithBadge(
  tab: TopLevelTab,
  selected: Boolean,
  ongoingCount: Int,
) {
  val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
  if (ongoingCount > 0) {
    BadgedBox(
      badge = {
        Badge(containerColor = LocalStatusColors.current.down, contentColor = Color.White) {
          Text(text = ongoingCount.toString())
        }
      }
    ) {
      Icon(imageVector = icon, contentDescription = stringResource(tab.labelRes))
    }
  } else {
    Icon(imageVector = icon, contentDescription = stringResource(tab.labelRes))
  }
}
