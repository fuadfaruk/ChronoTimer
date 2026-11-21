package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PresetDefaults
import com.example.model.IntervalConfig
import com.example.model.IntervalPhase
import com.example.model.TimeFormatter
import com.example.ui.components.AnimatedAudioWaves
import com.example.ui.components.CircularProgressTimerArc
import com.example.ui.components.CustomIntervalPresetEditorDialog
import com.example.ui.components.NumberStepper
import com.example.ui.components.WhiteNoiseSheet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.viewmodel.ChronoViewModel

@Composable
fun IntervalScreen(
  viewModel: ChronoViewModel,
  modifier: Modifier = Modifier
) {
  val intervalState by viewModel.intervalState.collectAsState()
  val customIntervalPresets by viewModel.customIntervalPresets.collectAsState()
  val settings by viewModel.settings.collectAsState()

  var showQuickCustomDialog by remember { mutableStateOf(false) }
  var showPresetEditor by remember { mutableStateOf(false) }
  var editingPreset by remember { mutableStateOf<IntervalConfig?>(null) }
  var showWhiteNoiseSheet by remember { mutableStateOf(false) }

  val config = intervalState.config
  val currentStep = intervalState.currentStep

  val phaseColor = when {
    intervalState.phase == IntervalPhase.FINISHED -> Color(0xFFFFD700)
    config.isMultiStep && currentStep != null -> Color(currentStep.colorHex)
    intervalState.phase == IntervalPhase.READY -> MaterialTheme.colorScheme.primary
    intervalState.phase == IntervalPhase.PREPARE -> NeonAmber
    intervalState.phase == IntervalPhase.WORK -> NeonGreen
    intervalState.phase == IntervalPhase.REST -> NeonCyan
    else -> MaterialTheme.colorScheme.primary
  }

  val phaseBadgeText = if (config.isMultiStep && currentStep != null) {
    when (intervalState.phase) {
      IntervalPhase.FINISHED -> "SESSION COMPLETE!"
      else -> {
        val stepPart = "STEP ${intervalState.currentStepIndex + 1}/${config.steps.size}"
        val cyclePart = if (config.repeatCount > 1) " • CYCLE ${intervalState.currentSet}/${config.repeatCount}" else ""
        "${currentStep.name.uppercase()} • $stepPart$cyclePart"
      }
    }
  } else {
    val phaseTitle = when (intervalState.phase) {
      IntervalPhase.READY -> "READY TO STUDY"
      IntervalPhase.PREPARE -> "GET READY"
      IntervalPhase.WORK -> "STUDY FOCUS"
      IntervalPhase.REST -> "BREAK TIME"
      IntervalPhase.FINISHED -> "SESSION COMPLETE!"
    }
    "$phaseTitle • SET ${intervalState.currentSet}/${config.sets}"
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // 1. Interval Mode Selector Row (Built-in routines, Custom routines, + Custom)
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
        // Built-in Default Interval Modes
        PresetDefaults.defaultIntervalConfigs.forEach { defaultConfig ->
          val isSelected = config.id == defaultConfig.id
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.selectIntervalConfig(defaultConfig) },
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
            )
          )
        }

        // Saved Custom Interval Routines
        customIntervalPresets.forEach { preset ->
          val isSelected = config.id == preset.id
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.selectIntervalConfig(preset) },
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
            modifier = Modifier.testTag("interval_custom_preset_chip_${preset.id}")
          )
        }

        // + Custom Multi-Interval Builder
        FilterChip(
          selected = config.id == "custom",
          onClick = {
            editingPreset = null
            showPresetEditor = true
          },
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
          modifier = Modifier.testTag("interval_add_preset_chip")
        )
      }
    }

    // Selected Custom Preset Header (Summary + Edit Button)
    if (config.isCustom) {
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

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Phase & Step Badge
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = phaseColor.copy(alpha = 0.12f),
      border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.6f))
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(phaseColor)
        )
        Text(
          text = phaseBadgeText,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = phaseColor,
          letterSpacing = 0.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    // Active Interval Background Sound Pill (for custom multi-step presets)
    if (config.isMultiStep && currentStep != null) {
      Spacer(modifier = Modifier.height(8.dp))
      val stepSound = currentStep.backgroundSound
      val isPlayingStepSound = intervalState.isRunning && stepSound != null && settings.isWhiteNoisePlaying

      Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isPlayingStepSound) {
          NeonGreen.copy(alpha = 0.14f)
        } else {
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
          1.dp,
          if (isPlayingStepSound) NeonGreen.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier
          .clip(RoundedCornerShape(18.dp))
          .clickable { showWhiteNoiseSheet = true }
          .testTag("interval_sound_status_pill")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (isPlayingStepSound) {
            AnimatedAudioWaves(color = NeonGreen)
          } else {
            Icon(
              imageVector = if (stepSound != null) Icons.Default.Headphones else Icons.AutoMirrored.Filled.VolumeOff,
              contentDescription = null,
              tint = if (stepSound != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp)
            )
          }
          Text(
            text = if (stepSound != null) {
              if (isPlayingStepSound) "Playing: ${stepSound.shortLabel}" else "Sound: ${stepSound.shortLabel}"
            } else {
              "Silent Interval"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isPlayingStepSound) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Dynamic Progress Arc
    CircularProgressTimerArc(
      progress = intervalState.progressInPhase,
      remainingMillis = intervalState.phaseRemainingSeconds * 1000L,
      isRunning = intervalState.isRunning,
      timerColor = phaseColor,
      size = if (config.isMultiStep) 224.dp else 250.dp
    )

    // 4. Interval Sequence Timeline Strip (when multi-step preset is selected)
    if (config.isMultiStep && config.steps.isNotEmpty()) {
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        config.steps.forEachIndexed { idx, step ->
          val isActive = idx == intervalState.currentStepIndex && intervalState.phase != IntervalPhase.FINISHED
          val isCompleted = intervalState.phase == IntervalPhase.FINISHED || idx < intervalState.currentStepIndex
          val stepColor = Color(step.colorHex)

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isActive) stepColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(
              width = if (isActive) 1.5.dp else 1.dp,
              color = if (isActive) stepColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            modifier = Modifier
              .widthIn(min = 116.dp, max = 160.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable { viewModel.jumpToIntervalStep(idx) }
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
                    tint = NeonGreen,
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
                Text(
                  text = step.backgroundSound?.shortLabel ?: "Silent",
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

    Spacer(modifier = Modifier.height(18.dp))

    // 5. Minimal Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Reset button
      FilledTonalButton(
        onClick = { viewModel.resetInterval() },
        shape = CircleShape,
        modifier = Modifier
          .size(58.dp)
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

      // Start / Pause Primary Button
      Button(
        onClick = {
          if (intervalState.isRunning) viewModel.pauseInterval() else viewModel.startInterval()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (intervalState.isRunning) NeonCoral else phaseColor,
          contentColor = if (intervalState.isRunning) Color.White else Color(0xFF00363D)
        ),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
          .height(60.dp)
          .width(140.dp)
          .testTag("interval_start_pause_btn")
      ) {
        Icon(
          imageVector = if (intervalState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = if (intervalState.isRunning) "Pause" else "Start",
          modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (intervalState.isRunning) "PAUSE" else "START",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      }

      Spacer(modifier = Modifier.width(18.dp))

      if (config.isMultiStep && config.steps.size > 1) {
        // Skip to Next Interval Step
        FilledTonalButton(
          onClick = { viewModel.skipToNextIntervalStep() },
          shape = CircleShape,
          modifier = Modifier
            .size(58.dp)
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
        // Quick Settings/Configure
        FilledTonalButton(
          onClick = { showQuickCustomDialog = true },
          shape = CircleShape,
          modifier = Modifier
            .size(58.dp)
            .testTag("interval_custom_btn"),
          contentPadding = PaddingValues(0.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Interval Settings",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
          )
        }
      }
    }
  }

  // Quick 2-phase Interval Builder Dialog
  if (showQuickCustomDialog) {
    CustomIntervalDialog(
      current = intervalState.config,
      onDismiss = { showQuickCustomDialog = false },
      onSave = { warmup, work, rest, sets ->
        viewModel.updateCustomInterval(warmup, work, rest, sets)
        showQuickCustomDialog = false
      },
      onOpenMultiStepBuilder = {
        showQuickCustomDialog = false
        editingPreset = null
        showPresetEditor = true
      }
    )
  }

  // Full Custom Multi-Interval Preset Builder / Editor
  if (showPresetEditor) {
    CustomIntervalPresetEditorDialog(
      viewModel = viewModel,
      editingPreset = editingPreset,
      onDismiss = { showPresetEditor = false },
      onSave = { savedPreset ->
        viewModel.saveCustomIntervalPreset(savedPreset)
        showPresetEditor = false
      },
      onDelete = { presetId ->
        viewModel.deleteCustomIntervalPreset(presetId)
        showPresetEditor = false
      }
    )
  }

  // Audio volume / focus noise sheet
  if (showWhiteNoiseSheet) {
    WhiteNoiseSheet(
      viewModel = viewModel,
      onDismiss = { showWhiteNoiseSheet = false }
    )
  }
}

@Composable
fun CustomIntervalDialog(
  current: IntervalConfig,
  onDismiss: () -> Unit,
  onSave: (warmup: Int, work: Int, rest: Int, sets: Int) -> Unit,
  onOpenMultiStepBuilder: (() -> Unit)? = null
) {
  var workMinutes by remember { mutableIntStateOf((current.workSec / 60).coerceAtLeast(1)) }
  var restMinutes by remember { mutableIntStateOf((current.restSec / 60).coerceAtLeast(1)) }
  var sets by remember { mutableIntStateOf(current.sets.coerceAtLeast(1)) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Quick Study Intervals", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          NumberStepper(
            value = workMinutes,
            label = "Study (m)",
            onValueChange = { workMinutes = it },
            min = 1,
            max = 120,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
          )
          NumberStepper(
            value = restMinutes,
            label = "Break (m)",
            onValueChange = { restMinutes = it },
            min = 1,
            max = 30,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
          )
          NumberStepper(
            value = sets,
            label = "Cycles",
            onValueChange = { sets = it },
            min = 1,
            max = 20,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
          )
        }

        if (onOpenMultiStepBuilder != null) {
          OutlinedButton(
            onClick = onOpenMultiStepBuilder,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create Named Preset with Sounds", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { onSave(0, workMinutes * 60, restMinutes * 60, sets) }) {
        Text("Apply")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
