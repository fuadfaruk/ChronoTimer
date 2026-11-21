package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.IntervalScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.StopwatchScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.viewmodel.AppTab
import com.example.viewmodel.ChronoViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: ChronoViewModel by viewModels()

  override fun onStop() {
    super.onStop()
    viewModel.stopWhiteNoisePreview()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val settings by viewModel.settings.collectAsState()
      val stopwatchState by viewModel.stopwatchState.collectAsState()
      val timerState by viewModel.timerState.collectAsState()
      val intervalState by viewModel.intervalState.collectAsState()

      // Dynamically keep screen awake if setting is enabled and any timer/stopwatch is active
      val isAnyActive = stopwatchState.isRunning ||
        timerState.timers.any { it.isRunning } ||
        intervalState.isRunning

      DisposableEffect(settings.keepScreenOn, isAnyActive) {
        if (settings.keepScreenOn || isAnyActive) {
          window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
      }

      MyApplicationTheme(darkTheme = settings.isDarkTheme) {
        val currentTab by viewModel.currentTab.collectAsState()
        var showSettingsDialog by remember { mutableStateOf(false) }

        // BackHandler: navigate back to Timer if on another sub-tab
        BackHandler(enabled = currentTab != AppTab.TIMER) {
          viewModel.setTab(AppTab.TIMER)
        }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
            TopAppBar(
              title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "ChronoTimer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                  )
                }
              },
              actions = {
                IconButton(
                  onClick = { showSettingsDialog = true },
                  modifier = Modifier.testTag("action_settings_btn")
                ) {
                  Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              },
              colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
              ),
              modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
          },
          bottomBar = {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 8.dp,
              modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
              // 1. Timer Tab (First Tab)
              val anyTimerRunning = timerState.timers.any { it.isRunning }
              NavigationBarItem(
                selected = currentTab == AppTab.TIMER,
                onClick = { viewModel.setTab(AppTab.TIMER) },
                icon = {
                  Icon(
                    imageVector = if (anyTimerRunning || currentTab == AppTab.TIMER) Icons.Filled.HourglassBottom else Icons.Outlined.HourglassEmpty,
                    contentDescription = "Timer"
                  )
                },
                label = { Text("Timer") },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  selectedTextColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("nav_tab_timer")
              )

              // 2. Stopwatch Tab
              NavigationBarItem(
                selected = currentTab == AppTab.STOPWATCH,
                onClick = { viewModel.setTab(AppTab.STOPWATCH) },
                icon = {
                  Icon(
                    imageVector = if (currentTab == AppTab.STOPWATCH) Icons.Filled.Timer else Icons.Outlined.Timer,
                    contentDescription = "Stopwatch"
                  )
                },
                label = { Text("Stopwatch") },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  selectedTextColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("nav_tab_stopwatch")
              )

              // 3. Interval Tab
              NavigationBarItem(
                selected = currentTab == AppTab.INTERVAL,
                onClick = { viewModel.setTab(AppTab.INTERVAL) },
                icon = {
                  Icon(
                    imageVector = if (currentTab == AppTab.INTERVAL) Icons.Filled.DirectionsRun else Icons.Outlined.DirectionsRun,
                    contentDescription = "Interval"
                  )
                },
                label = { Text("Interval") },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  selectedTextColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("nav_tab_interval")
              )

              // 4. Presets Tab
              NavigationBarItem(
                selected = currentTab == AppTab.PRESETS,
                onClick = { viewModel.setTab(AppTab.PRESETS) },
                icon = {
                  Icon(
                    imageVector = if (currentTab == AppTab.PRESETS) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks,
                    contentDescription = "Presets"
                  )
                },
                label = { Text("Presets") },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  selectedTextColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("nav_tab_presets")
              )
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(innerPadding)
          ) {
            Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
              when (tab) {
                AppTab.STOPWATCH -> StopwatchScreen(viewModel = viewModel)
                AppTab.TIMER -> TimerScreen(viewModel = viewModel)
                AppTab.INTERVAL -> IntervalScreen(viewModel = viewModel)
                AppTab.PRESETS -> PresetsScreen(viewModel = viewModel)
              }
            }
          }

          if (showSettingsDialog) {
            SettingsDialog(
              viewModel = viewModel,
              onDismiss = { showSettingsDialog = false }
            )
          }
        }
      }
    }
  }
}
