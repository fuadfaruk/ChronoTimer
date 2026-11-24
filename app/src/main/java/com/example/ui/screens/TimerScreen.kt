package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TimeFormatter
import com.example.ui.components.AnimatedAudioWaves
import com.example.ui.components.CircularProgressTimerArc
import com.example.ui.components.FluidAnimatedValue
import com.example.ui.components.NumberStepper
import com.example.ui.components.WhiteNoiseSheet
import com.example.ui.components.fluidPressScale
import com.example.ui.components.fluidPulseHalo
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.resolveThemeAccentColor
import com.example.viewmodel.ChronoViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Immutable
private data class TimerScreenMeta(
  val timerId: String,
  val title: String,
  val initialDurationMillis: Long,
  val isRunning: Boolean,
  val hasStarted: Boolean,
  val colorHex: Long,
  val hasMultipleTimers: Boolean,
  val ringingTimerId: String?
)

@Immutable
private data class TimerSettingsMeta(
  val whiteNoiseEnabled: Boolean,
  val isWhiteNoisePlaying: Boolean,
  val quickAddSeconds: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(
  viewModel: ChronoViewModel,
  modifier: Modifier = Modifier
) {
  val metaFlow = remember(viewModel) {
    viewModel.timerState
      .map { state ->
        val active = state.timers.find { it.id == state.selectedTimerId }
          ?: state.timers.firstOrNull()
        active?.let {
          TimerScreenMeta(
            timerId = it.id,
            title = it.title,
            initialDurationMillis = it.initialDurationMillis,
            isRunning = it.isRunning,
            hasStarted = it.hasStarted,
            colorHex = it.colorHex,
            hasMultipleTimers = state.timers.size > 1,
            ringingTimerId = state.ringingTimerId
          )
        }
      }
      .distinctUntilChanged()
  }
  val settingsMetaFlow = remember(viewModel) {
    viewModel.settings
      .map { s ->
        TimerSettingsMeta(
          whiteNoiseEnabled = s.whiteNoiseEnabled,
          isWhiteNoisePlaying = s.isWhiteNoisePlaying,
          quickAddSeconds = s.quickAddSeconds
        )
      }
      .distinctUntilChanged()
  }

  val initialActive = remember {
    val state = viewModel.timerState.value
    val active = state.timers.find { it.id == state.selectedTimerId } ?: state.timers.firstOrNull()
    active?.let {
      TimerScreenMeta(
        timerId = it.id,
        title = it.title,
        initialDurationMillis = it.initialDurationMillis,
        isRunning = it.isRunning,
        hasStarted = it.hasStarted,
        colorHex = it.colorHex,
        hasMultipleTimers = state.timers.size > 1,
        ringingTimerId = state.ringingTimerId
      )
    }
  }
  val initialSettingsMeta = remember {
    val s = viewModel.settings.value
    TimerSettingsMeta(s.whiteNoiseEnabled, s.isWhiteNoisePlaying, s.quickAddSeconds)
  }

  val meta by metaFlow.collectAsStateWithLifecycle(initialValue = initialActive)
  val settingsMeta by settingsMetaFlow.collectAsStateWithLifecycle(initialValue = initialSettingsMeta)

  var showEditDurationDialog by remember { mutableStateOf(false) }
  var showEditQuickAddDialog by remember { mutableStateOf(false) }
  var showWhiteNoiseSheet by remember { mutableStateOf(false) }

  val activeMeta = meta ?: return

  val colorScheme = MaterialTheme.colorScheme
  val timerColor = resolveThemeAccentColor(activeMeta.colorHex, colorScheme)
  val isRunning = activeMeta.isRunning
  val isFocusMode = activeMeta.hasStarted
  val currentTotalSec = (activeMeta.initialDurationMillis / 1000L).toInt()
  val quickAddSec = settingsMeta.quickAddSeconds
  val quickAddLabel = remember(quickAddSec) { formatQuickAddButtonLabel(quickAddSec) }

  AnimatedContent(
    targetState = isFocusMode,
    transitionSpec = {
      ContentTransform(
        targetContentEnter = fadeIn(tween(300, easing = FastOutSlowInEasing)) +
          scaleIn(
            initialScale = 0.94f,
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioMediumBouncy,
              stiffness = Spring.StiffnessMediumLow
            )
          ),
        initialContentExit = fadeOut(tween(220)) +
          scaleOut(targetScale = 0.95f, animationSpec = tween(220))
      )
    },
    label = "timer_focus_mode_transition"
  ) { focusActive ->
    if (focusActive) {
      val focusButtonColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "focus_btn_color"
      )
      val focusContentColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "focus_btn_content_color"
      )
      val pauseInteraction = remember { MutableInteractionSource() }
      val resetInteraction = remember { MutableInteractionSource() }
      val addInteraction = remember { MutableInteractionSource() }

      Column(
        modifier = modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        TimerDialContainer(
          viewModel = viewModel,
          timerId = activeMeta.timerId,
          isRunning = isRunning,
          timerColor = if (isRunning) timerColor else MaterialTheme.colorScheme.primary,
          size = 280.dp,
          showStatusLabel = false
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
              )
            ),
          horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically
        ) {
          AnimatedVisibility(
            visible = !isRunning,
            enter = fadeIn(tween(260)) +
              expandHorizontally(
                animationSpec = spring(
                  dampingRatio = Spring.DampingRatioMediumBouncy,
                  stiffness = Spring.StiffnessMediumLow
                )
              ) +
              scaleIn(initialScale = 0.85f),
            exit = fadeOut(tween(180)) +
              shrinkHorizontally(animationSpec = tween(200)) +
              scaleOut(targetScale = 0.85f)
          ) {
            FilledTonalButton(
              onClick = { viewModel.resetTimer(activeMeta.timerId) },
              interactionSource = resetInteraction,
              shape = RoundedCornerShape(32.dp),
              contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
              modifier = Modifier
                .height(60.dp)
                .fluidPressScale(resetInteraction)
                .testTag("timer_reset_btn")
            ) {
              Text(
                text = "RESET",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
            }
          }

          Button(
            onClick = {
              if (isRunning) viewModel.pauseTimer(activeMeta.timerId)
              else viewModel.startTimer(activeMeta.timerId)
            },
            interactionSource = pauseInteraction,
            colors = ButtonDefaults.buttonColors(
              containerColor = focusButtonColor,
              contentColor = focusContentColor
            ),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
              .height(60.dp)
              .widthIn(min = 136.dp)
              .fluidPulseHalo(color = focusButtonColor, isActive = isRunning)
              .fluidPressScale(pauseInteraction)
              .testTag("timer_start_pause_btn")
          ) {
            FluidAnimatedValue(targetState = isRunning) { running ->
              Text(
                text = if (running) "PAUSE" else "RESUME",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
          }

          FilledTonalButton(
            onClick = { viewModel.addTimeToTimer(activeMeta.timerId, quickAddSec.toLong()) },
            interactionSource = addInteraction,
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
            modifier = Modifier
              .height(60.dp)
              .widthIn(min = 76.dp)
              .fluidPressScale(addInteraction)
              .testTag("timer_add_new_btn")
          ) {
            Text(
              text = quickAddLabel.replace("\n", " "),
              fontFamily = RobotoMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    } else {
      val noisePillInteraction = remember { MutableInteractionSource() }
      val dialTapInteraction = remember { MutableInteractionSource() }
      val startBtnInteraction = remember { MutableInteractionSource() }
      val quickAddBtnInteraction = remember { MutableInteractionSource() }

      val noiseContainerColor by animateColorAsState(
        targetValue = if (settingsMeta.isWhiteNoisePlaying) {
          MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        } else if (settingsMeta.whiteNoiseEnabled) {
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(350),
        label = "noise_pill_container"
      )
      val noiseBorderColor by animateColorAsState(
        targetValue = if (settingsMeta.isWhiteNoisePlaying) {
          MaterialTheme.colorScheme.secondary
        } else {
          MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        },
        animationSpec = tween(350),
        label = "noise_pill_border"
      )
      val animatedBadgeColor by animateColorAsState(
        targetValue = timerColor,
        animationSpec = tween(400),
        label = "badge_timer_color"
      )

      Column(
        modifier = modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = noiseContainerColor,
          border = BorderStroke(1.dp, noiseBorderColor),
          modifier = Modifier
            .fluidPressScale(noisePillInteraction)
            .animateContentSize(
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
              )
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(
              interactionSource = noisePillInteraction,
              indication = LocalIndication.current
            ) { showWhiteNoiseSheet = true }
            .testTag("timer_white_noise_card")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (settingsMeta.isWhiteNoisePlaying) {
              AnimatedAudioWaves(color = MaterialTheme.colorScheme.secondary)
            } else {
              Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = if (settingsMeta.whiteNoiseEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }

            Text(
              text = if (settingsMeta.isWhiteNoisePlaying)
                "Study Noise: Active"
              else if (settingsMeta.whiteNoiseEnabled)
                "Study Noise: On"
              else
                "Study Noise: Off",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = if (settingsMeta.isWhiteNoisePlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
            )

            IconButton(
              onClick = { viewModel.toggleWhiteNoise() },
              modifier = Modifier.size(24.dp).testTag("timer_white_noise_quick_switch")
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Configure Focus Noise",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }

        AnimatedVisibility(
          visible = activeMeta.hasMultipleTimers,
          enter = fadeIn(tween(240)) + scaleIn(initialScale = 0.94f),
          exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.94f)
        ) {
          ActiveTimersSwitcherRow(
            viewModel = viewModel,
            selectedTimerId = activeMeta.timerId
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = animatedBadgeColor.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, animatedBadgeColor.copy(alpha = 0.45f))
        ) {
          Row(
            modifier = Modifier
              .padding(horizontal = 12.dp, vertical = 4.dp)
              .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(animatedBadgeColor)
            )
            FluidAnimatedValue(targetState = activeMeta.title.uppercase()) { titleText ->
              Text(
                text = titleText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = animatedBadgeColor,
                letterSpacing = 0.6.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fluidPressScale(dialTapInteraction, pressedScale = 0.96f)
            .clip(CircleShape)
            .clickable(
              interactionSource = dialTapInteraction,
              indication = LocalIndication.current
            ) { showEditDurationDialog = true }
        ) {
          TimerDialContainer(
            viewModel = viewModel,
            timerId = activeMeta.timerId,
            isRunning = false,
            timerColor = MaterialTheme.colorScheme.primary,
            size = 240.dp,
            showStatusLabel = true
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.Center
        ) {
          val presets = remember {
            listOf(
              15 * 60 to "15m",
              25 * 60 to "25m",
              45 * 60 to "45m",
              50 * 60 to "50m",
              60 * 60 to "60m"
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { (sec, label) ->
              val isSelected = currentTotalSec == sec
              val chipInteraction = remember(sec) { MutableInteractionSource() }
              FilterChip(
                selected = isSelected,
                onClick = {
                  viewModel.updateCurrentTimerDuration(activeMeta.timerId, sec.toLong())
                },
                interactionSource = chipInteraction,
                label = {
                  Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  )
                },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isSelected,
                  borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                  selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fluidPressScale(chipInteraction)
              )
            }

            val customChipInteraction = remember { MutableInteractionSource() }
            FilterChip(
              selected = false,
              onClick = { showEditDurationDialog = true },
              interactionSource = customChipInteraction,
              label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Custom", fontSize = 13.sp)
                }
              },
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fluidPressScale(customChipInteraction)
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = { viewModel.startTimer(activeMeta.timerId) },
            interactionSource = startBtnInteraction,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
              .height(60.dp)
              .width(148.dp)
              .fluidPulseHalo(color = MaterialTheme.colorScheme.primary, isActive = true)
              .fluidPressScale(startBtnInteraction)
              .testTag("timer_start_pause_btn")
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Start",
              modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "START",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          }

          Spacer(modifier = Modifier.width(18.dp))

          Box(contentAlignment = Alignment.Center) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
              modifier = Modifier
                .size(58.dp)
                .fluidPressScale(quickAddBtnInteraction, pressedScale = 0.90f)
                .clip(CircleShape)
                .combinedClickable(
                  interactionSource = quickAddBtnInteraction,
                  indication = LocalIndication.current,
                  onClick = { showEditQuickAddDialog = true },
                  onLongClick = { showEditQuickAddDialog = true }
                )
                .testTag("timer_add_new_btn")
            ) {
              Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Text(
                  text = quickAddLabel,
                  fontFamily = RobotoMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = if (quickAddLabel.contains("\n")) 11.sp else 14.sp,
                  lineHeight = 13.sp,
                  textAlign = TextAlign.Center,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(1.dp))
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Customize quick-add time",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(11.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  if (showWhiteNoiseSheet) {
    WhiteNoiseSheet(
      viewModel = viewModel,
      onDismiss = { showWhiteNoiseSheet = false }
    )
  }

  if (showEditDurationDialog) {
    EditDurationDialog(
      initialSeconds = currentTotalSec,
      onDismiss = { showEditDurationDialog = false },
      onConfirm = { newSeconds ->
        viewModel.updateCurrentTimerDuration(activeMeta.timerId, newSeconds.toLong())
        showEditDurationDialog = false
      }
    )
  }

  if (showEditQuickAddDialog) {
    EditQuickAddDialog(
      initialTotalSeconds = settingsMeta.quickAddSeconds,
      onDismiss = { showEditQuickAddDialog = false },
      onConfirm = { mins, secs ->
        viewModel.setQuickAddIncrement(mins, secs)
        showEditQuickAddDialog = false
      }
    )
  }

  if (activeMeta.ringingTimerId != null) {
    val ringingId = activeMeta.ringingTimerId
    val alarmTransition = rememberInfiniteTransition(label = "alarm_ring_pulse")
    val alarmScaleState = alarmTransition.animateFloat(
      initialValue = 0.92f,
      targetValue = 1.14f,
      animationSpec = infiniteRepeatable(
        animation = tween(550, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "alarm_scale"
    )
    val alarmTiltState = alarmTransition.animateFloat(
      initialValue = -10f,
      targetValue = 10f,
      animationSpec = infiniteRepeatable(
        animation = tween(220, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "alarm_tilt"
    )

    AlertDialog(
      onDismissRequest = { viewModel.dismissAlarm() },
      icon = {
        Box(
          modifier = Modifier
            .size(56.dp)
            .fluidPulseHalo(color = MaterialTheme.colorScheme.tertiary, isActive = true),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = "Study Session Complete",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
              .size(40.dp)
              .graphicsLayer {
                val s = alarmScaleState.value
                scaleX = s
                scaleY = s
                rotationZ = alarmTiltState.value
              }
          )
        }
      },
      title = {
        Text(
          text = "Study Session Complete!",
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp
        )
      },
      text = {
        Text(
          text = "Great focus! Take a well-deserved break or start your next study block.",
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        Button(
          onClick = { viewModel.dismissAlarm() },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          )
        ) {
          Text("Done", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = {
            if (ringingId != null) {
              viewModel.addTimeToTimer(ringingId, quickAddSec.toLong())
              viewModel.startTimer(ringingId)
            }
            viewModel.dismissAlarm()
          }
        ) {
          Text("+${TimeFormatter.formatDurationSummary(quickAddSec.toLong())}")
        }
      }
    )
  }
}

@Composable
private fun TimerDialContainer(
  viewModel: ChronoViewModel,
  timerId: String,
  isRunning: Boolean,
  timerColor: Color,
  size: Dp,
  showStatusLabel: Boolean
) {
  val dialFlow = remember(viewModel, timerId) {
    viewModel.timerState
      .map { state ->
        val t = state.timers.find { it.id == timerId } ?: state.timers.firstOrNull()
        (t?.progress ?: 0f) to (t?.remainingMillis ?: 0L)
      }
      .distinctUntilChanged()
  }
  val initialPair = remember(timerId) {
    val t = viewModel.timerState.value.timers.find { it.id == timerId }
      ?: viewModel.timerState.value.timers.firstOrNull()
    (t?.progress ?: 0f) to (t?.remainingMillis ?: 0L)
  }
  val (progress, remainingMillis) = dialFlow.collectAsStateWithLifecycle(initialValue = initialPair).value

  CircularProgressTimerArc(
    progress = progress,
    remainingMillis = remainingMillis,
    isRunning = isRunning,
    timerColor = timerColor,
    size = size,
    showStatusLabel = showStatusLabel
  )
}

@Composable
private fun ActiveTimersSwitcherRow(
  viewModel: ChronoViewModel,
  selectedTimerId: String
) {
  val timerState by viewModel.timerState.collectAsStateWithLifecycle()
  val colorScheme = MaterialTheme.colorScheme

  Column {
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .animateContentSize(),
      horizontalArrangement = Arrangement.Center
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        timerState.timers.forEach { timer ->
          val isSelected = timer.id == selectedTimerId
          val chipColor = resolveThemeAccentColor(timer.colorHex, colorScheme)
          val chipInteraction = remember(timer.id) { MutableInteractionSource() }
          val chipBg by animateColorAsState(
            targetValue = if (isSelected) chipColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            animationSpec = tween(250),
            label = "chip_bg_${timer.id}"
          )
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = chipBg,
            border = BorderStroke(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) chipColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            modifier = Modifier
              .fluidPressScale(chipInteraction)
              .clip(RoundedCornerShape(16.dp))
              .clickable(
                interactionSource = chipInteraction,
                indication = LocalIndication.current
              ) { viewModel.selectTimer(timer.id) }
              .testTag("active_timer_chip_${timer.id}")
          ) {
            Row(
              modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(if (timer.isRunning) MaterialTheme.colorScheme.secondary else chipColor)
              )
              Text(
                text = "${timer.title} • ${TimeFormatter.formatTimer(timer.remainingMillis)}",
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
              )
              Box(
                modifier = Modifier
                  .size(20.dp)
                  .clip(CircleShape)
                  .clickable { viewModel.deleteTimer(timer.id) },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Remove ${timer.title}",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(12.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun formatQuickAddButtonLabel(totalSeconds: Int): String {
  val mins = totalSeconds / 60
  val secs = totalSeconds % 60
  return when {
    secs == 0 -> "+${mins}m"
    mins == 0 -> "+${secs}s"
    else -> "+${mins}m\n${secs}s"
  }
}

@Composable
fun EditQuickAddDialog(
  initialTotalSeconds: Int,
  onDismiss: () -> Unit,
  onConfirm: (minutes: Int, seconds: Int) -> Unit
) {
  var minutes by remember { mutableIntStateOf((initialTotalSeconds / 60).coerceIn(0, 120)) }
  var seconds by remember { mutableIntStateOf((initialTotalSeconds % 60).coerceIn(0, 59)) }
  val total = minutes * 60 + seconds

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Customize Quick Add", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Choose how many minutes and seconds are added each time you tap the quick-add button.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val quickOptions = listOf(
            15 to "+15s",
            30 to "+30s",
            60 to "+1m",
            120 to "+2m",
            300 to "+5m",
            600 to "+10m"
          )
          quickOptions.forEach { (secOpt, label) ->
            val isSelected = total == secOpt
            FilterChip(
              selected = isSelected,
              onClick = {
                minutes = secOpt / 60
                seconds = secOpt % 60
              },
              label = { Text(label, fontSize = 12.sp, fontFamily = RobotoMonoFontFamily) },
              shape = RoundedCornerShape(12.dp)
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          NumberStepper(
            value = minutes,
            label = "Mins",
            onValueChange = { minutes = it },
            min = 0,
            max = 120,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
          )
          NumberStepper(
            value = seconds,
            label = "Secs",
            onValueChange = { seconds = it },
            min = 0,
            max = 59,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
          )
        }

        Text(
          text = if (total > 0) {
            "Adds +${TimeFormatter.formatDurationSummary(total.toLong())} per tap"
          } else {
            "Select at least 1 second"
          },
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = if (total > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(minutes, seconds) },
        enabled = total > 0,
        modifier = Modifier.testTag("quick_add_save_btn")
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun EditDurationDialog(
  initialSeconds: Int,
  onDismiss: () -> Unit,
  onConfirm: (Int) -> Unit
) {
  var hours by remember { mutableIntStateOf(initialSeconds / 3600) }
  var minutes by remember { mutableIntStateOf((initialSeconds % 3600) / 60) }
  var seconds by remember { mutableIntStateOf(initialSeconds % 60) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Set Study Duration", fontWeight = FontWeight.Bold) },
    text = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        NumberStepper(
          value = hours,
          label = "Hours",
          onValueChange = { hours = it },
          min = 0,
          max = 12,
          modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
        )
        NumberStepper(
          value = minutes,
          label = "Mins",
          onValueChange = { minutes = it },
          min = 0,
          max = 59,
          modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
        )
        NumberStepper(
          value = seconds,
          label = "Secs",
          onValueChange = { seconds = it },
          min = 0,
          max = 59,
          modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val total = hours * 3600 + minutes * 60 + seconds
          onConfirm(if (total > 0) total else 60)
        }
      ) {
        Text("Set")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
