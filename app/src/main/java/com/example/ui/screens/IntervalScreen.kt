package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PresetDefaults
import com.example.model.IntervalConfig
import com.example.model.IntervalPhase
import com.example.model.TimeFormatter
import com.example.ui.components.AnimatedAudioWaves
import com.example.ui.components.CircularProgressTimerArc
import com.example.ui.components.CustomIntervalPresetEditorDialog
import com.example.ui.components.FluidAnimatedValue
import com.example.ui.components.IntervalSoundPickerDialog
import com.example.ui.components.WhiteNoiseSheet
import com.example.ui.components.fluidPressScale
import com.example.ui.components.fluidPulseHalo
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.contrastingContentColor
import com.example.ui.theme.resolveThemeAccentColor
import com.example.util.NoiseProfile
import com.example.viewmodel.ChronoViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Immutable
private data class IntervalScreenMeta(
  val config: IntervalConfig,
  val phase: IntervalPhase,
  val currentSet: Int,
  val currentStepIndex: Int,
  val isRunning: Boolean,
  val isCompletionRinging: Boolean
)

@Immutable
private data class IntervalAudioMeta(
  val whiteNoiseEnabled: Boolean,
  val isWhiteNoisePlaying: Boolean,
  val whiteNoiseProfile: NoiseProfile,
  val customAudioUri: String?,
  val customAudioName: String?
)

@Composable
fun IntervalScreen(
  viewModel: ChronoViewModel,
  modifier: Modifier = Modifier
) {
  val metaFlow = remember(viewModel) {
    viewModel.intervalState
      .map { s ->
        IntervalScreenMeta(
          config = s.config,
          phase = s.phase,
          currentSet = s.currentSet,
          currentStepIndex = s.currentStepIndex,
          isRunning = s.isRunning,
          isCompletionRinging = s.isCompletionRinging
        )
      }
      .distinctUntilChanged()
  }
  val audioMetaFlow = remember(viewModel) {
    viewModel.settings
      .map { s ->
        IntervalAudioMeta(
          whiteNoiseEnabled = s.whiteNoiseEnabled,
          isWhiteNoisePlaying = s.isWhiteNoisePlaying,
          whiteNoiseProfile = s.whiteNoiseProfile,
          customAudioUri = s.customAudioUri,
          customAudioName = s.customAudioName
        )
      }
      .distinctUntilChanged()
  }

  val initialMeta = remember(viewModel) {
    val s = viewModel.intervalState.value
    IntervalScreenMeta(
      config = s.config,
      phase = s.phase,
      currentSet = s.currentSet,
      currentStepIndex = s.currentStepIndex,
      isRunning = s.isRunning,
      isCompletionRinging = s.isCompletionRinging
    )
  }
  val initialAudioMeta = remember(viewModel) {
    val s = viewModel.settings.value
    IntervalAudioMeta(
      whiteNoiseEnabled = s.whiteNoiseEnabled,
      isWhiteNoisePlaying = s.isWhiteNoisePlaying,
      whiteNoiseProfile = s.whiteNoiseProfile,
      customAudioUri = s.customAudioUri,
      customAudioName = s.customAudioName
    )
  }

  val meta by metaFlow.collectAsStateWithLifecycle(initialValue = initialMeta)
  val audioMeta by audioMetaFlow.collectAsStateWithLifecycle(initialValue = initialAudioMeta)
  val customIntervalPresets by viewModel.customIntervalPresets.collectAsStateWithLifecycle()

  var showPresetEditor by remember { mutableStateOf(false) }
  var editingPreset by remember { mutableStateOf<IntervalConfig?>(null) }
  var showWhiteNoiseSheet by remember { mutableStateOf(false) }
  var showStepSoundPicker by remember { mutableStateOf(false) }

  val config = meta.config
  val currentStep = config.steps.getOrNull(meta.currentStepIndex)

  val colorScheme = MaterialTheme.colorScheme
  val targetPhaseColor = when {
    meta.phase == IntervalPhase.FINISHED -> colorScheme.tertiary
    config.isMultiStep && currentStep != null -> resolveThemeAccentColor(currentStep.colorHex, colorScheme)
    meta.phase == IntervalPhase.READY -> colorScheme.primary
    meta.phase == IntervalPhase.PREPARE -> colorScheme.tertiary
    meta.phase == IntervalPhase.WORK -> colorScheme.secondary
    meta.phase == IntervalPhase.REST -> colorScheme.primary
    else -> colorScheme.primary
  }

  val phaseColor by animateColorAsState(
    targetValue = targetPhaseColor,
    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
    label = "interval_phase_color"
  )

  val phaseBadgeText = if (config.isMultiStep && currentStep != null) {
    when (meta.phase) {
      IntervalPhase.FINISHED -> "SESSION COMPLETE!"
      else -> {
        val stepPart = "STEP ${meta.currentStepIndex + 1}/${config.steps.size}"
        val cyclePart = if (config.repeatCount > 1) " • CYCLE ${meta.currentSet}/${config.repeatCount}" else ""
        "${currentStep.name.uppercase()} • $stepPart$cyclePart"
      }
    }
  } else {
    val phaseTitle = when (meta.phase) {
      IntervalPhase.READY -> "READY TO STUDY"
      IntervalPhase.PREPARE -> "GET READY"
      IntervalPhase.WORK -> "STUDY FOCUS"
      IntervalPhase.REST -> "BREAK TIME"
      IntervalPhase.FINISHED -> "SESSION COMPLETE!"
    }
    "$phaseTitle • SET ${meta.currentSet}/${config.sets}"
  }

  val dialStatusLabel = if (config.isMultiStep) {
    when (meta.phase) {
      IntervalPhase.FINISHED -> ""
      else -> "FOCUSING"
    }
  } else {
    when (meta.phase) {
      IntervalPhase.PREPARE -> "GET READY"
      IntervalPhase.WORK -> "FOCUSING"
      IntervalPhase.REST -> "BREAK TIME"
      else -> ""
    }
  }

  val resetInteraction = remember { MutableInteractionSource() }
  val startPauseInteraction = remember { MutableInteractionSource() }
  val secondaryActionInteraction = remember { MutableInteractionSource() }

  val targetStartBg = if (meta.isRunning) colorScheme.error else targetPhaseColor
  val startBtnBgColor by animateColorAsState(
    targetValue = targetStartBg,
    animationSpec = tween(320, easing = FastOutSlowInEasing),
    label = "interval_start_bg"
  )
  val startBtnContentColor by animateColorAsState(
    targetValue = contrastingContentColor(targetStartBg, colorScheme),
    animationSpec = tween(320, easing = FastOutSlowInEasing),
    label = "interval_start_content"
  )

  val timelineListState = rememberLazyListState()
  LaunchedEffect(meta.currentStepIndex, config.id) {
    if (config.isMultiStep && meta.currentStepIndex in config.steps.indices) {
      timelineListState.animateScrollToItem(meta.currentStepIndex)
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isCompactHeight = maxHeight < 580.dp
    val dialSize = when {
      isCompactHeight && config.isMultiStep -> 176.dp
      isCompactHeight -> 196.dp
      config.isMultiStep -> 218.dp
      else -> 244.dp
    }
    val vGapSmall = if (isCompactHeight) 8.dp else 12.dp
    val vGapDial = if (isCompactHeight) 10.dp else 14.dp
    val vGapControls = if (isCompactHeight) 12.dp else 18.dp

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = if (isCompactHeight) Arrangement.Top else Arrangement.Center
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          PresetDefaults.defaultIntervalConfigs.forEach { defaultConfig ->
            val isSelected = config.id == defaultConfig.id
            val chipInteraction = remember(defaultConfig.id) { MutableInteractionSource() }
            FilterChip(
              selected = isSelected,
              onClick = { viewModel.selectIntervalConfig(defaultConfig) },
              interactionSource = chipInteraction,
              label = {
                Text(
                  text = defaultConfig.name.substringBefore(" ("),
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              },
              shape = RoundedCornerShape(16.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              modifier = Modifier.fluidPressScale(chipInteraction)
            )
          }

          customIntervalPresets.forEach { preset ->
            val isSelected = config.id == preset.id
            val chipInteraction = remember(preset.id) { MutableInteractionSource() }
            FilterChip(
              selected = isSelected,
              onClick = { viewModel.selectIntervalConfig(preset) },
              interactionSource = chipInteraction,
              label = {
                Text(
                  text = preset.name,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              },
              shape = RoundedCornerShape(16.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              modifier = Modifier
                .fluidPressScale(chipInteraction)
                .testTag("interval_custom_preset_chip_${preset.id}")
            )
          }

          val addChipInteraction = remember { MutableInteractionSource() }
          FilterChip(
            selected = false,
            onClick = {
              editingPreset = null
              showPresetEditor = true
            },
            interactionSource = addChipInteraction,
            label = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Custom",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            },
            shape = RoundedCornerShape(16.dp),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = false,
              borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            ),
            modifier = Modifier
              .fluidPressScale(addChipInteraction)
              .testTag("interval_add_preset_chip")
          )
        }
      }

      AnimatedVisibility(
        visible = config.isCustom && config.isMultiStep,
        enter = fadeIn(tween(240)) + expandVertically(),
        exit = fadeOut(tween(180)) + shrinkVertically()
      ) {
        Column {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = config.name,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${config.steps.size} intervals • ${TimeFormatter.formatDurationSummary(config.totalDurationSeconds.toLong())}" +
                    if (config.repeatCount > 1) " • ${config.repeatCount}x cycles" else "",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontFamily = RobotoMonoFontFamily
                )
              }

              IconButton(
                onClick = {
                  editingPreset = config
                  showPresetEditor = true
                },
                modifier = Modifier.testTag("interval_edit_preset_btn")
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Edit Custom Preset",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(vGapSmall))

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = phaseColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.6f))
      ) {
        Row(
          modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .animateContentSize(
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
              )
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(phaseColor)
          )
          FluidAnimatedValue(targetState = phaseBadgeText) { text ->
            Text(
              text = text,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = phaseColor,
              letterSpacing = 0.5.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (config.isMultiStep && currentStep != null) {
        val stepSound = currentStep.backgroundSound
        val stepCustomUri = currentStep.customAudioUri ?: audioMeta.customAudioUri
        val stepCustomName = currentStep.customAudioName ?: audioMeta.customAudioName
        val isCustomAudioConfigured = stepSound != NoiseProfile.CUSTOM_AUDIO || !stepCustomUri.isNullOrBlank()
        val isPlayingStepSound = meta.isRunning && stepSound != null && isCustomAudioConfigured && audioMeta.isWhiteNoisePlaying

        val soundPillInteraction = remember { MutableInteractionSource() }
        val soundPillBg by animateColorAsState(
          targetValue = if (isPlayingStepSound) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
          } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          },
          animationSpec = tween(300),
          label = "interval_sound_bg"
        )
        val soundPillBorder by animateColorAsState(
          targetValue = if (isPlayingStepSound) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
          animationSpec = tween(300),
          label = "interval_sound_border"
        )

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = soundPillBg,
          border = BorderStroke(1.dp, soundPillBorder),
          modifier = Modifier
            .fluidPressScale(soundPillInteraction)
            .animateContentSize()
            .clip(RoundedCornerShape(18.dp))
            .clickable(
              interactionSource = soundPillInteraction,
              indication = LocalIndication.current
            ) { showStepSoundPicker = true }
            .testTag("interval_sound_status_pill")
        ) {
          Row(
            modifier = Modifier
              .widthIn(max = 268.dp)
              .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (isPlayingStepSound) {
              AnimatedAudioWaves(color = MaterialTheme.colorScheme.secondary)
            } else {
              Icon(
                imageVector = if (stepSound != null) Icons.Default.Headphones else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = null,
                tint = if (stepSound != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
              )
            }
            val stepSoundLabel = when {
              stepSound == NoiseProfile.CUSTOM_AUDIO && !stepCustomName.isNullOrBlank() -> stepCustomName
              stepSound == NoiseProfile.CUSTOM_AUDIO && !isCustomAudioConfigured -> "Pick Custom Audio"
              else -> stepSound?.shortLabel
            }
            Text(
              text = if (stepSoundLabel != null) {
                if (isPlayingStepSound) "Playing: $stepSoundLabel" else "Step Sound: $stepSoundLabel"
              } else {
                "Silent Step • Tap to Set"
              },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = if (isPlayingStepSound) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      } else {
        val isStandardNoisePlaying = meta.isRunning &&
          meta.phase == IntervalPhase.WORK &&
          audioMeta.whiteNoiseEnabled &&
          audioMeta.isWhiteNoisePlaying
        val soundPillInteraction = remember { MutableInteractionSource() }
        val soundPillBg by animateColorAsState(
          targetValue = when {
            isStandardNoisePlaying -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
            audioMeta.whiteNoiseEnabled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          },
          animationSpec = tween(300),
          label = "interval_std_sound_bg"
        )
        val soundPillBorder by animateColorAsState(
          targetValue = if (isStandardNoisePlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
          animationSpec = tween(300),
          label = "interval_std_sound_border"
        )

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = soundPillBg,
          border = BorderStroke(1.dp, soundPillBorder),
          modifier = Modifier
            .fluidPressScale(soundPillInteraction)
            .animateContentSize()
            .clip(RoundedCornerShape(18.dp))
            .clickable(
              interactionSource = soundPillInteraction,
              indication = LocalIndication.current
            ) { showWhiteNoiseSheet = true }
            .testTag("interval_sound_status_pill")
        ) {
          Row(
            modifier = Modifier
              .widthIn(max = 268.dp)
              .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (isStandardNoisePlaying) {
              AnimatedAudioWaves(color = MaterialTheme.colorScheme.secondary)
            } else {
              Icon(
                imageVector = if (audioMeta.whiteNoiseEnabled) Icons.Default.Headphones else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = null,
                tint = if (audioMeta.whiteNoiseEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
              )
            }

            val isCustomSelected = audioMeta.whiteNoiseProfile == NoiseProfile.CUSTOM_AUDIO &&
              !audioMeta.customAudioName.isNullOrBlank()
            val pillLabel = when {
              isStandardNoisePlaying && isCustomSelected -> "Playing: ${audioMeta.customAudioName}"
              isStandardNoisePlaying -> "Study Noise: Active"
              audioMeta.whiteNoiseEnabled && isCustomSelected -> "Audio: ${audioMeta.customAudioName}"
              audioMeta.whiteNoiseEnabled -> "Study Noise: On"
              else -> "Study Noise: Off"
            }

            Text(
              text = pillLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = if (isStandardNoisePlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )

            IconButton(
              onClick = { viewModel.toggleWhiteNoise() },
              modifier = Modifier
                .size(24.dp)
                .testTag("interval_white_noise_quick_switch")
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Toggle Focus Audio",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(vGapDial))

      IntervalDialContainer(
        viewModel = viewModel,
        isRunning = meta.isRunning,
        phaseColor = phaseColor,
        size = dialSize,
        statusLabel = dialStatusLabel
      )

      if (config.isMultiStep && config.steps.isNotEmpty()) {
        Spacer(modifier = Modifier.height(vGapDial))
        LazyRow(
          state = timelineListState,
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          itemsIndexed(config.steps, key = { idx, step -> "${step.id}_$idx" }) { idx, step ->
            val isActive = idx == meta.currentStepIndex && meta.phase != IntervalPhase.FINISHED
            val isCompleted = meta.phase == IntervalPhase.FINISHED ||
              (meta.phase != IntervalPhase.READY && idx < meta.currentStepIndex)
            val stepColor = resolveThemeAccentColor(step.colorHex, colorScheme)
            val stepInteraction = remember(step.id, idx) { MutableInteractionSource() }
            val stepScaleState = animateFloatAsState(
              targetValue = if (isActive) 1.03f else 1f,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
              ),
              label = "step_scale_$idx"
            )
            val stepBg by animateColorAsState(
              targetValue = if (isActive) stepColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              animationSpec = tween(280),
              label = "step_bg_$idx"
            )

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = stepBg,
              border = BorderStroke(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) stepColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
              ),
              modifier = Modifier
                .widthIn(min = 116.dp, max = 160.dp)
                .graphicsLayer {
                  val s = stepScaleState.value
                  scaleX = s
                  scaleY = s
                }
                .fluidPressScale(stepInteraction)
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                  interactionSource = stepInteraction,
                  indication = LocalIndication.current
                ) { viewModel.jumpToIntervalStep(idx) }
                .testTag("interval_timeline_step_$idx")
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                  if (isCompleted) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = "Completed",
                      tint = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.size(12.dp)
                    )
                  } else {
                    Box(
                      modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(stepColor)
                    )
                  }
                  Text(
                    text = step.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isActive) stepColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = TimeFormatter.formatDurationSummary(step.durationSeconds.toLong()),
                    fontFamily = RobotoMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = "•",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                  )
                  Icon(
                    imageVector = if (step.backgroundSound != null) Icons.Default.Headphones else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = null,
                    tint = if (step.backgroundSound != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                  )
                  val stepSoundBadge = if (step.backgroundSound == NoiseProfile.CUSTOM_AUDIO && !step.customAudioName.isNullOrBlank()) {
                    step.customAudioName
                  } else {
                    step.backgroundSound?.shortLabel ?: "Silent"
                  }
                  Text(
                    text = stepSoundBadge,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(vGapControls))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        FilledTonalButton(
          onClick = { viewModel.resetInterval() },
          interactionSource = resetInteraction,
          shape = CircleShape,
          modifier = Modifier
            .size(58.dp)
            .fluidPressScale(resetInteraction)
            .testTag("interval_reset_btn"),
          contentPadding = PaddingValues(0.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Reset Interval",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(18.dp))

        Button(
          onClick = {
            if (meta.isRunning) viewModel.pauseInterval() else viewModel.startInterval()
          },
          interactionSource = startPauseInteraction,
          colors = ButtonDefaults.buttonColors(
            containerColor = startBtnBgColor,
            contentColor = startBtnContentColor
          ),
          shape = RoundedCornerShape(32.dp),
          modifier = Modifier
            .height(60.dp)
            .width(140.dp)
            .fluidPulseHalo(color = startBtnBgColor, isActive = meta.isRunning)
            .fluidPressScale(startPauseInteraction)
            .testTag("interval_start_pause_btn")
        ) {
          val buttonLabel = when {
            meta.isRunning -> "PAUSE"
            meta.phase != IntervalPhase.READY && meta.phase != IntervalPhase.FINISHED -> "RESUME"
            else -> "START"
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (meta.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = buttonLabel,
              modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            FluidAnimatedValue(targetState = buttonLabel) { labelText ->
              Text(
                text = labelText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                softWrap = false,
                maxLines = 1
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(18.dp))

        val canSkip = (config.isMultiStep && (config.steps.size > 1 || config.repeatCount > 1)) ||
          (!config.isMultiStep && meta.phase != IntervalPhase.READY && meta.phase != IntervalPhase.FINISHED)
        if (canSkip) {
          FilledTonalButton(
            onClick = { viewModel.skipToNextIntervalStep() },
            interactionSource = secondaryActionInteraction,
            shape = CircleShape,
            modifier = Modifier
              .size(58.dp)
              .fluidPressScale(secondaryActionInteraction)
              .testTag("interval_skip_btn"),
            contentPadding = PaddingValues(0.dp)
          ) {
            Icon(
              imageVector = Icons.Default.SkipNext,
              contentDescription = "Next Interval",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(24.dp)
            )
          }
        } else {
          FilledTonalButton(
            onClick = {
              editingPreset = meta.config
              showPresetEditor = true
            },
            interactionSource = secondaryActionInteraction,
            shape = CircleShape,
            modifier = Modifier
              .size(58.dp)
              .fluidPressScale(secondaryActionInteraction)
              .testTag("interval_custom_btn"),
            contentPadding = PaddingValues(0.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Customize Interval Preset",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }
  }

  if (showPresetEditor) {
    CustomIntervalPresetEditorDialog(
      viewModel = viewModel,
      editingPreset = editingPreset,
      onDismiss = { showPresetEditor = false },
      onSave = { savedPreset ->
        viewModel.saveCustomIntervalPreset(savedPreset, selectAfterSave = true)
        showPresetEditor = false
      },
      onDelete = { presetId ->
        viewModel.deleteCustomIntervalPreset(presetId)
        showPresetEditor = false
      }
    )
  }

  if (showStepSoundPicker && config.isMultiStep && currentStep != null) {
    val stepIndex = meta.currentStepIndex
    IntervalSoundPickerDialog(
      viewModel = viewModel,
      intervalName = currentStep.name,
      selectedSound = currentStep.backgroundSound,
      selectedCustomAudioUri = currentStep.customAudioUri,
      selectedCustomAudioName = currentStep.customAudioName,
      onSelect = { chosenSound, customUri, customName ->
        viewModel.updateActiveIntervalStepSound(
          stepIndex = stepIndex,
          sound = chosenSound,
          customAudioUri = customUri,
          customAudioName = customName
        )
        viewModel.stopWhiteNoisePreview()
        showStepSoundPicker = false
      },
      onPickCustomAudioUri = { uri ->
        viewModel.takeAudioPermissionAndResolveName(uri) { uriStr, displayName ->
          viewModel.updateActiveIntervalStepSound(
            stepIndex = stepIndex,
            sound = NoiseProfile.CUSTOM_AUDIO,
            customAudioUri = uriStr,
            customAudioName = displayName
          )
        }
        viewModel.stopWhiteNoisePreview()
        showStepSoundPicker = false
      },
      onDismiss = {
        viewModel.stopWhiteNoisePreview()
        showStepSoundPicker = false
      }
    )
  }

  if (showWhiteNoiseSheet) {
    WhiteNoiseSheet(
      viewModel = viewModel,
      onDismiss = { showWhiteNoiseSheet = false }
    )
  }

  if (meta.isCompletionRinging) {
    val alarmTransition = rememberInfiniteTransition(label = "interval_alarm_ring_pulse")
    val alarmScaleState = alarmTransition.animateFloat(
      initialValue = 0.92f,
      targetValue = 1.14f,
      animationSpec = infiniteRepeatable(
        animation = tween(550, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "interval_alarm_scale"
    )
    val alarmTiltState = alarmTransition.animateFloat(
      initialValue = -10f,
      targetValue = 10f,
      animationSpec = infiniteRepeatable(
        animation = tween(220, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "interval_alarm_tilt"
    )

    AlertDialog(
      onDismissRequest = { viewModel.dismissIntervalAlarm() },
      icon = {
        Box(
          modifier = Modifier
            .size(56.dp)
            .fluidPulseHalo(color = MaterialTheme.colorScheme.tertiary, isActive = true),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = "Interval Session Complete",
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
          text = "Interval Session Complete!",
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp
        )
      },
      text = {
        Text(
          text = "Completed ${config.name} (${TimeFormatter.formatDurationSummary(config.totalDurationSeconds.toLong())}). Great focus!",
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        Button(
          onClick = { viewModel.dismissIntervalAlarm() },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier.testTag("interval_completion_done_btn")
        ) {
          Text("Done", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { viewModel.restartInterval() },
          modifier = Modifier.testTag("interval_completion_restart_btn")
        ) {
          Text("Restart")
        }
      }
    )
  }
}

@Composable
private fun IntervalDialContainer(
  viewModel: ChronoViewModel,
  isRunning: Boolean,
  phaseColor: Color,
  size: Dp,
  statusLabel: String
) {
  val dialFlow = remember(viewModel) {
    viewModel.intervalState
      .map { it.progressInPhase to it.phaseRemainingMillis }
      .distinctUntilChanged()
  }
  val initialPair = remember(viewModel) {
    val s = viewModel.intervalState.value
    s.progressInPhase to s.phaseRemainingMillis
  }
  val (progress, remainingMillis) = dialFlow.collectAsStateWithLifecycle(initialValue = initialPair).value

  CircularProgressTimerArc(
    progress = progress,
    remainingMillis = remainingMillis,
    isRunning = isRunning,
    timerColor = phaseColor,
    size = size,
    showStatusLabel = statusLabel.isNotBlank(),
    statusLabel = statusLabel
  )
}
