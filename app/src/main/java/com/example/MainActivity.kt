package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.model.IntervalPhase
import com.example.ui.components.FluidBlobBackground
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.IntervalScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.StopwatchScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.resolveThemeAccentColor
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

      val systemInDark = isSystemInDarkTheme()
      val useDarkTheme = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
      }

      MyApplicationTheme(
        darkTheme = useDarkTheme,
        dynamicColor = settings.dynamicColorEnabled
      ) {
        val currentTab by viewModel.currentTab.collectAsState()
        var showSettingsDialog by remember { mutableStateOf(false) }

        val activeTimer = timerState.timers.find { it.id == timerState.selectedTimerId }
          ?: timerState.timers.firstOrNull()
        val isTimerFullScreen = currentTab == AppTab.TIMER && activeTimer?.hasStarted == true

        LaunchedEffect(isTimerFullScreen) {
          if (isTimerFullScreen) showSettingsDialog = false
        }

        val view = LocalView.current
        DisposableEffect(isTimerFullScreen, useDarkTheme, view) {
          val controller = WindowCompat.getInsetsController(window, view)
          controller.isAppearanceLightStatusBars = !useDarkTheme
          controller.isAppearanceLightNavigationBars = !useDarkTheme
          controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
          if (isTimerFullScreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
          } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
          }
          onDispose {
            WindowCompat.getInsetsController(window, view)
              .show(WindowInsetsCompat.Type.systemBars())
          }
        }

        // BackHandler: navigate back to Timer if on another sub-tab
        BackHandler(enabled = currentTab != AppTab.TIMER) {
          viewModel.setTab(AppTab.TIMER)
        }

        // Determine ambient blob colors dynamically from active Material You / M3 ColorScheme & state
        val colorScheme = MaterialTheme.colorScheme
        val (ambientPrimary, ambientSecondary) = when (currentTab) {
          AppTab.TIMER -> {
            val timerAccent = activeTimer?.let { resolveThemeAccentColor(it.colorHex, colorScheme) }
              ?: colorScheme.primary
            timerAccent to colorScheme.tertiary
          }
          AppTab.STOPWATCH -> colorScheme.primary to colorScheme.secondary
          AppTab.INTERVAL -> {
            val phaseAccent = when {
              intervalState.phase == IntervalPhase.FINISHED -> colorScheme.tertiary
              intervalState.config.isMultiStep && intervalState.currentStep != null ->
                resolveThemeAccentColor(intervalState.currentStep!!.colorHex, colorScheme)
              intervalState.phase == IntervalPhase.PREPARE -> colorScheme.tertiary
              intervalState.phase == IntervalPhase.WORK -> colorScheme.secondary
              intervalState.phase == IntervalPhase.REST -> colorScheme.primary
              else -> colorScheme.primary
            }
            phaseAccent to colorScheme.secondary
          }
          AppTab.PRESETS -> colorScheme.tertiary to colorScheme.primary
        }

        val logoTransition = rememberInfiniteTransition(label = "logo_float")
        val logoBob by logoTransition.animateFloat(
          initialValue = -2.5f,
          targetValue = 2.5f,
          animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
          ),
          label = "logo_bob"
        )
        val logoTilt by logoTransition.animateFloat(
          initialValue = -6f,
          targetValue = 6f,
          animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
          ),
          label = "logo_tilt"
        )

        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
          // Living Fluid Blob Backdrop across the entire app
          FluidBlobBackground(
            primaryAccent = ambientPrimary,
            secondaryAccent = ambientSecondary,
            isActive = isAnyActive,
            isDarkTheme = useDarkTheme,
            modifier = Modifier.fillMaxSize()
          )

          Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = if (isTimerFullScreen) {
              WindowInsets(0, 0, 0, 0)
            } else {
              ScaffoldDefaults.contentWindowInsets
            },
            topBar = {
              AnimatedVisibility(
                visible = !isTimerFullScreen,
                enter = fadeIn(tween(260)) + expandVertically(),
                exit = fadeOut(tween(200)) + shrinkVertically()
              ) {
                TopAppBar(
                  title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                          .size(22.dp)
                          .graphicsLayer {
                            if (isAnyActive) {
                              translationY = logoBob
                              rotationZ = logoTilt
                            }
                          }
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
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
                  ),
                  modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                )
              }
            },
            bottomBar = {
              AnimatedVisibility(
                visible = !isTimerFullScreen,
                enter = fadeIn(tween(260)) + expandVertically(),
                exit = fadeOut(tween(200)) + shrinkVertically()
              ) {
                NavigationBar(
                  containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                  tonalElevation = 8.dp,
                  modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                  // 1. Timer Tab (First Tab)
                  val anyTimerRunning = timerState.timers.any { it.isRunning }
                  FluidNavBarItem(
                    selected = currentTab == AppTab.TIMER,
                    onClick = { viewModel.setTab(AppTab.TIMER) },
                    icon = {
                      Icon(
                        imageVector = if (anyTimerRunning || currentTab == AppTab.TIMER) Icons.Filled.HourglassBottom else Icons.Outlined.HourglassEmpty,
                        contentDescription = "Timer"
                      )
                    },
                    label = "Timer",
                    modifier = Modifier.testTag("nav_tab_timer")
                  )

                  // 2. Stopwatch Tab
                  FluidNavBarItem(
                    selected = currentTab == AppTab.STOPWATCH,
                    onClick = { viewModel.setTab(AppTab.STOPWATCH) },
                    icon = {
                      Icon(
                        imageVector = if (currentTab == AppTab.STOPWATCH) Icons.Filled.Timer else Icons.Outlined.Timer,
                        contentDescription = "Stopwatch"
                      )
                    },
                    label = "Stopwatch",
                    modifier = Modifier.testTag("nav_tab_stopwatch")
                  )

                  // 3. Interval Tab
                  FluidNavBarItem(
                    selected = currentTab == AppTab.INTERVAL,
                    onClick = { viewModel.setTab(AppTab.INTERVAL) },
                    icon = {
                      Icon(
                        imageVector = if (currentTab == AppTab.INTERVAL) Icons.Filled.DirectionsRun else Icons.Outlined.DirectionsRun,
                        contentDescription = "Interval"
                      )
                    },
                    label = "Interval",
                    modifier = Modifier.testTag("nav_tab_interval")
                  )

                  // 4. Presets Tab
                  FluidNavBarItem(
                    selected = currentTab == AppTab.PRESETS,
                    onClick = { viewModel.setTab(AppTab.PRESETS) },
                    icon = {
                      Icon(
                        imageVector = if (currentTab == AppTab.PRESETS) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks,
                        contentDescription = "Presets"
                      )
                    },
                    label = "Presets",
                    modifier = Modifier.testTag("nav_tab_presets")
                  )
                }
              }
            }
          ) { innerPadding ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ) {
              AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                  val order = listOf(AppTab.TIMER, AppTab.STOPWATCH, AppTab.INTERVAL, AppTab.PRESETS)
                  val fromIdx = order.indexOf(initialState)
                  val toIdx = order.indexOf(targetState)
                  val direction = if (toIdx >= fromIdx) 1 else -1

                  ContentTransform(
                    targetContentEnter = slideInHorizontally(
                      animationSpec = tween(340, easing = FastOutSlowInEasing)
                    ) { width -> direction * (width / 5) } +
                      fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                      scaleIn(
                        initialScale = 0.96f,
                        animationSpec = spring(
                          dampingRatio = Spring.DampingRatioNoBouncy,
                          stiffness = Spring.StiffnessMediumLow
                        )
                      ),
                    initialContentExit = slideOutHorizontally(
                      animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ) { width -> -direction * (width / 6) } +
                      fadeOut(tween(220)) +
                      scaleOut(
                        targetScale = 0.97f,
                        animationSpec = tween(220)
                      )
                  )
                },
                label = "tab_fluid_transition"
              ) { tab ->
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
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.FluidNavBarItem(
  selected: Boolean,
  onClick: () -> Unit,
  icon: @Composable () -> Unit,
  label: String,
  modifier: Modifier = Modifier
) {
  val iconScale by animateFloatAsState(
    targetValue = if (selected) 1.15f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "nav_icon_scale"
  )

  NavigationBarItem(
    selected = selected,
    onClick = onClick,
    icon = {
      Box(
        modifier = Modifier.graphicsLayer {
          scaleX = iconScale
          scaleY = iconScale
        }
      ) {
        icon()
      }
    },
    label = { Text(label) },
    colors = NavigationBarItemDefaults.colors(
      indicatorColor = MaterialTheme.colorScheme.primaryContainer,
      selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
      selectedTextColor = MaterialTheme.colorScheme.primary
    ),
    modifier = modifier
  )
}
