package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.IntervalConfig
import com.example.model.IntervalStep
import com.example.model.TimeFormatter
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.resolveThemeAccentColor
import com.example.util.NoiseProfile
import com.example.viewmodel.ChronoViewModel
import kotlinx.coroutines.launch
import java.util.UUID

private val stepAccentPalette = listOf(
  0xFF00E676L, // Secondary M3 slot
  0xFF00E5FFL, // Primary M3 slot
  0xFFFF9100L, // Tertiary M3 slot
  0xFFB388FFL, // Primary-Tertiary blend slot
  0xFFFF5252L  // Error/Accent M3 slot
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomIntervalPresetEditorDialog(
  viewModel: ChronoViewModel,
  editingPreset: IntervalConfig? = null,
  onDismiss: () -> Unit,
  onSave: (IntervalConfig) -> Unit,
  onDelete: ((String) -> Unit)? = null
) {
  val isEditingExistingCustom = editingPreset != null && editingPreset.isCustom
  val isCustomizingStandard = editingPreset != null && !editingPreset.isCustom
  var presetName by remember(editingPreset) {
    mutableStateOf(
      when {
        isEditingExistingCustom -> editingPreset!!.name
        isCustomizingStandard -> "${editingPreset!!.name.substringBefore(" (")} Custom"
        else -> ""
      }
    )
  }
  var repeatCount by remember(editingPreset) {
    mutableIntStateOf(
      when {
        isEditingExistingCustom -> editingPreset!!.repeatCount.coerceAtLeast(1)
        isCustomizingStandard -> editingPreset!!.sets.coerceAtLeast(1)
        else -> 1
      }
    )
  }

  val steps = remember(editingPreset) {
    mutableStateListOf<IntervalStep>().apply {
      when {
        isEditingExistingCustom && editingPreset!!.steps.isNotEmpty() -> {
          addAll(editingPreset.steps)
        }
        isCustomizingStandard -> {
          addAll(editingPreset!!.toEditableSteps())
        }
        else -> {
          add(
            IntervalStep(
              name = "Deep Focus",
              durationSeconds = 25 * 60,
              backgroundSound = NoiseProfile.VOICE_MASKING,
              colorHex = 0xFF00E676L
            )
          )
          add(
            IntervalStep(
              name = "Short Break",
              durationSeconds = 5 * 60,
              backgroundSound = null,
              colorHex = 0xFF00E5FFL
            )
          )
        }
      }
    }
  }

  var pickingSoundForIndex by remember { mutableStateOf<Int?>(null) }
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  DisposableEffect(Unit) {
    onDispose {
      viewModel.stopWhiteNoisePreview()
    }
  }

  val totalDurationSeconds = steps.sumOf { it.durationSeconds.coerceAtLeast(5) } * repeatCount

  Dialog(
    onDismissRequest = {
      viewModel.stopWhiteNoisePreview()
      onDismiss()
    },
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing),
      color = MaterialTheme.colorScheme.background
    ) {
      Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
          TopAppBar(
            title = {
              Text(
                text = when {
                  isEditingExistingCustom -> "Edit Interval Preset"
                  isCustomizingStandard -> "Customize Interval Flow"
                  else -> "New Interval Preset"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
              )
            },
            navigationIcon = {
              IconButton(
                onClick = {
                  viewModel.stopWhiteNoisePreview()
                  onDismiss()
                },
                modifier = Modifier.testTag("interval_editor_close_btn")
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close Editor"
                )
              }
            },
            actions = {
              if (isEditingExistingCustom && onDelete != null) {
                IconButton(
                  onClick = {
                    viewModel.stopWhiteNoisePreview()
                    onDelete(editingPreset!!.id)
                  },
                  modifier = Modifier.testTag("interval_editor_delete_btn")
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Preset",
                    tint = MaterialTheme.colorScheme.error
                  )
                }
              }
              Button(
                onClick = {
                  viewModel.stopWhiteNoisePreview()
                  val sanitizedSteps = steps.mapIndexed { idx, step ->
                    step.copy(
                      name = step.name.trim().ifBlank { "Interval ${idx + 1}" },
                      durationSeconds = step.durationSeconds.coerceAtLeast(5)
                    )
                  }
                  val finalPreset = IntervalConfig(
                    id = if (isEditingExistingCustom) editingPreset!!.id else UUID.randomUUID().toString(),
                    name = presetName.trim().ifBlank { "Custom Flow" },
                    warmupSec = 0,
                    workSec = sanitizedSteps.first().durationSeconds,
                    restSec = 0,
                    sets = repeatCount,
                    steps = sanitizedSteps,
                    repeatCount = repeatCount,
                    isCustom = true
                  )
                  onSave(finalPreset)
                },
                enabled = steps.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier
                  .padding(end = 8.dp)
                  .testTag("interval_editor_save_btn")
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save", fontWeight = FontWeight.Bold)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface
            )
          )
        }
      ) { innerPadding ->
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
          contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // 1. Preset Name & Cycle Summary Card
          item {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                OutlinedTextField(
                  value = presetName,
                  onValueChange = { presetName = it },
                  label = { Text("Preset Name") },
                  placeholder = { Text("e.g. Morning Study Flow, HIIT Circuit") },
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("interval_preset_name_input")
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Repeat,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp)
                    )
                    Column {
                      Text(
                        text = "Repeat Sequence",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                      )
                      Text(
                        text = "${steps.size} intervals • Total ${TimeFormatter.formatDurationSummary(totalDurationSeconds.toLong())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = RobotoMonoFontFamily
                      )
                    }
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    SmallStepIconButton(
                      onClick = { if (repeatCount > 1) repeatCount-- },
                      icon = Icons.Default.Remove,
                      contentDescription = "Decrease Repeat Cycles"
                    )
                    Text(
                      text = "${repeatCount}x",
                      fontFamily = RobotoMonoFontFamily,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                      modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    SmallStepIconButton(
                      onClick = { if (repeatCount < 20) repeatCount++ },
                      icon = Icons.Default.Add,
                      contentDescription = "Increase Repeat Cycles"
                    )
                  }
                }
              }
            }
          }

          // Section Header
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "INTERVALS (${steps.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
              )
              Text(
                text = "Tap sound to customize per interval",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          // 2. Interval Step Cards
          itemsIndexed(steps, key = { _, step -> step.id }) { index, step ->
            IntervalStepEditorCard(
              index = index,
              totalSteps = steps.size,
              step = step,
              customAudioName = settings.customAudioName,
              modifier = Modifier.animateItem(
                fadeInSpec = tween(250),
                fadeOutSpec = tween(200),
                placementSpec = spring(
                  dampingRatio = Spring.DampingRatioMediumBouncy,
                  stiffness = Spring.StiffnessMediumLow
                )
              ),
              onStepChange = { updated -> steps[index] = updated },
              onMoveUp = if (index > 0) {
                {
                  val item = steps.removeAt(index)
                  steps.add(index - 1, item)
                }
              } else null,
              onMoveDown = if (index < steps.lastIndex) {
                {
                  val item = steps.removeAt(index)
                  steps.add(index + 1, item)
                }
              } else null,
              onDelete = if (steps.size > 1) {
                { steps.removeAt(index) }
              } else null,
              onPickSound = { pickingSoundForIndex = index }
            )
          }

          // 3. Add Interval Button
          item {
            OutlinedButton(
              onClick = {
                val nextIndex = steps.size + 1
                val nextColor = stepAccentPalette[steps.size % stepAccentPalette.size]
                val nextSound = if (steps.lastOrNull()?.backgroundSound == null) {
                  NoiseProfile.VOICE_MASKING
                } else {
                  NoiseProfile.PINK_NOISE
                }
                steps.add(
                  IntervalStep(
                    name = "Interval $nextIndex",
                    durationSeconds = 10 * 60,
                    backgroundSound = nextSound,
                    colorHex = nextColor
                  )
                )
                coroutineScope.launch {
                  listState.animateScrollToItem(steps.size + 2)
                }
              },
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("interval_add_step_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Add Interval",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }
  }

  // Per-Interval Sound Picker Dialog
  val activeIndex = pickingSoundForIndex
  if (activeIndex != null && activeIndex in steps.indices) {
    val activeStep = steps[activeIndex]
    IntervalSoundPickerDialog(
      viewModel = viewModel,
      intervalName = activeStep.name.ifBlank { "Interval ${activeIndex + 1}" },
      selectedSound = activeStep.backgroundSound,
      selectedCustomAudioUri = activeStep.customAudioUri,
      selectedCustomAudioName = activeStep.customAudioName,
      onSelect = { chosenSound, customUri, customName ->
        val idx = steps.indexOfFirst { it.id == activeStep.id }
        if (idx != -1) {
          steps[idx] = if (chosenSound == NoiseProfile.CUSTOM_AUDIO) {
            steps[idx].copy(
              backgroundSound = chosenSound,
              customAudioUri = customUri ?: steps[idx].customAudioUri,
              customAudioName = customName ?: steps[idx].customAudioName
            )
          } else {
            steps[idx].copy(backgroundSound = chosenSound)
          }
        }
        viewModel.stopWhiteNoisePreview()
        pickingSoundForIndex = null
      },
      onPickCustomAudioUri = { uri ->
        val stepId = activeStep.id
        viewModel.takeAudioPermissionAndResolveName(uri) { uriStr, displayName ->
          val idx = steps.indexOfFirst { it.id == stepId }
          if (idx != -1) {
            steps[idx] = steps[idx].copy(
              backgroundSound = NoiseProfile.CUSTOM_AUDIO,
              customAudioUri = uriStr,
              customAudioName = displayName
            )
          }
        }
        viewModel.stopWhiteNoisePreview()
        pickingSoundForIndex = null
      },
      onDismiss = {
        viewModel.stopWhiteNoisePreview()
        pickingSoundForIndex = null
      }
    )
  }
}

@Composable
private fun IntervalStepEditorCard(
  index: Int,
  totalSteps: Int,
  step: IntervalStep,
  customAudioName: String?,
  modifier: Modifier = Modifier,
  onStepChange: (IntervalStep) -> Unit,
  onMoveUp: (() -> Unit)?,
  onMoveDown: (() -> Unit)?,
  onDelete: (() -> Unit)?,
  onPickSound: () -> Unit
) {
  val colorScheme = MaterialTheme.colorScheme
  val accentColor by animateColorAsState(
    targetValue = resolveThemeAccentColor(step.colorHex, colorScheme),
    animationSpec = tween(280),
    label = "step_accent_${step.id}"
  )
  val mins = step.durationSeconds / 60
  val secs = step.durationSeconds % 60

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    tonalElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("interval_step_card_$index")
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header Row: Step # + Reorder + Delete (full 48dp touch targets)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "STEP ${index + 1}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (totalSteps > 1) {
            IconButton(
              onClick = { onMoveUp?.invoke() },
              enabled = onMoveUp != null,
              modifier = Modifier.minimumInteractiveComponentSize()
            ) {
              Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Move Interval Up",
                modifier = Modifier.size(18.dp),
                tint = if (onMoveUp != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
              )
            }
            IconButton(
              onClick = { onMoveDown?.invoke() },
              enabled = onMoveDown != null,
              modifier = Modifier.minimumInteractiveComponentSize()
            ) {
              Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "Move Interval Down",
                modifier = Modifier.size(18.dp),
                tint = if (onMoveDown != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
              )
            }
          }
          if (onDelete != null) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("interval_step_delete_$index")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove Interval",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
              )
            }
          }
        }
      }

      // Color selector row with accessible touch targets
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = "Color",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(end = 6.dp)
        )
        stepAccentPalette.forEach { colorLong ->
          val isColorSelected = step.colorHex == colorLong
          val dotColor = resolveThemeAccentColor(colorLong, colorScheme)
          val dotSize by animateDpAsState(
            targetValue = if (isColorSelected) 22.dp else 18.dp,
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioMediumBouncy,
              stiffness = Spring.StiffnessMedium
            ),
            label = "step_dot_size"
          )
          Box(
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(CircleShape)
              .clickable { onStepChange(step.copy(colorHex = colorLong)) },
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
                .border(
                  width = if (isColorSelected) 2.dp else 0.dp,
                  color = if (isColorSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                  shape = CircleShape
                )
            )
          }
        }
      }

      // Custom Interval Name Input
      OutlinedTextField(
        value = step.name,
        onValueChange = { onStepChange(step.copy(name = it)) },
        label = { Text("Interval Name") },
        placeholder = { Text("e.g. Warm-up, Deep Focus, Break") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("interval_step_name_$index")
      )

      // Duration Steppers (Minutes & Seconds)
      val maxStepSeconds = 180 * 60 + 59
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        CompactDurationField(
          label = "Minutes",
          value = mins,
          onDecrement = {
            val newMins = (mins - 1).coerceAtLeast(0)
            val total = (newMins * 60 + secs).coerceAtLeast(5)
            onStepChange(step.copy(durationSeconds = total))
          },
          onIncrement = {
            val newMins = (mins + 1).coerceAtMost(180)
            val total = (newMins * 60 + secs).coerceAtMost(maxStepSeconds)
            onStepChange(step.copy(durationSeconds = total))
          },
          modifier = Modifier.weight(1f)
        )

        CompactDurationField(
          label = "Seconds",
          value = secs,
          onDecrement = {
            val total = (step.durationSeconds - 5).coerceAtLeast(5)
            onStepChange(step.copy(durationSeconds = total))
          },
          onIncrement = {
            val total = (step.durationSeconds + 5).coerceAtMost(maxStepSeconds)
            onStepChange(step.copy(durationSeconds = total))
          },
          modifier = Modifier.weight(1f)
        )
      }

      // Quick duration pills for rapid entry
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val quickDurations = listOf(
          15 to "15s",
          30 to "30s",
          60 to "1m",
          5 * 60 to "5m",
          15 * 60 to "15m",
          25 * 60 to "25m",
          45 * 60 to "45m"
        )
        quickDurations.forEach { (secVal, label) ->
          val isMatch = step.durationSeconds == secVal
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isMatch) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
              1.dp,
              if (isMatch) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable { onStepChange(step.copy(durationSeconds = secVal)) }
          ) {
            Text(
              text = label,
              fontFamily = RobotoMonoFontFamily,
              fontSize = 11.sp,
              fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Medium,
              color = if (isMatch) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }
      }

      // Background Sound Selector Card
      val hasSound = step.backgroundSound != null
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (hasSound) {
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
          1.dp,
          if (hasSound) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable { onPickSound() }
          .testTag("interval_step_sound_picker_$index")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = if (hasSound) Icons.Default.Headphones else Icons.AutoMirrored.Filled.VolumeOff,
              contentDescription = null,
              tint = if (hasSound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
            Column {
              Text(
                text = "Background Sound",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              val effectiveCustomName = step.customAudioName ?: customAudioName
              Text(
                text = when {
                  step.backgroundSound == NoiseProfile.CUSTOM_AUDIO && !effectiveCustomName.isNullOrBlank() ->
                    "Custom: $effectiveCustomName"
                  step.backgroundSound != null -> step.backgroundSound.displayName
                  else -> "Silent (No Background Sound)"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (hasSound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Text(
            text = "Change",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}

@Composable
private fun CompactDurationField(
  label: String,
  value: Int,
  onDecrement: () -> Unit,
  onIncrement: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    modifier = modifier
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
      Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        SmallStepIconButton(
          onClick = onDecrement,
          icon = Icons.Default.Remove,
          contentDescription = "Decrease $label"
        )
        Text(
          text = String.format("%02d", value),
          fontFamily = RobotoMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = MaterialTheme.colorScheme.onBackground
        )
        SmallStepIconButton(
          onClick = onIncrement,
          icon = Icons.Default.Add,
          contentDescription = "Increase $label"
        )
      }
    }
  }
}

@Composable
private fun SmallStepIconButton(
  onClick: () -> Unit,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .fluidPressScale(interactionSource, pressedScale = 0.86f)
      .clip(CircleShape)
      .clickable(
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        onClick = onClick
      ),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}

@Composable
fun IntervalSoundPickerDialog(
  viewModel: ChronoViewModel,
  intervalName: String,
  selectedSound: NoiseProfile?,
  selectedCustomAudioUri: String? = null,
  selectedCustomAudioName: String? = null,
  onSelect: (sound: NoiseProfile?, customUri: String?, customName: String?) -> Unit,
  onPickCustomAudioUri: (android.net.Uri) -> Unit,
  onDismiss: () -> Unit
) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val effectiveCustomUri = selectedCustomAudioUri ?: settings.customAudioUri
  val effectiveCustomName = selectedCustomAudioName ?: settings.customAudioName
  val hasCustomAudio = !effectiveCustomUri.isNullOrBlank()

  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      onPickCustomAudioUri(uri)
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(
          text = "Background Sound",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "For \"$intervalName\"",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Silent option
        val isSilent = selectedSound == null
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isSilent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          border = BorderStroke(
            1.dp,
            if (isSilent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect(null, null, null) }
            .testTag("sound_option_silent")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.VolumeOff,
              contentDescription = null,
              tint = if (isSilent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Silent (No Background Sound)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSilent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSilent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = "Quiet interval with transition alerts only.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            if (isSilent) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        // Available synthesized ambient profiles & Custom Device Audio
        NoiseProfile.entries.forEach { profile ->
          val isSelected = selectedSound == profile
          val isCustomProfile = profile == NoiseProfile.CUSTOM_AUDIO
          val isPreviewing = settings.isWhiteNoisePlaying && settings.previewingProfile == profile

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(
              1.dp,
              if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                if (isCustomProfile && !hasCustomAudio) {
                  audioPickerLauncher.launch(arrayOf("audio/*"))
                } else {
                  onSelect(
                    profile,
                    if (isCustomProfile) effectiveCustomUri else null,
                    if (isCustomProfile) effectiveCustomName else null
                  )
                }
              }
              .testTag("sound_option_${profile.name}")
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .border(
                      2.dp,
                      if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                      CircleShape
                    )
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                )

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = if (isCustomProfile && hasCustomAudio) {
                      "Selected: ${effectiveCustomName ?: "Custom Audio"}"
                    } else {
                      profile.description
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = if (isCustomProfile && hasCustomAudio) {
                      MaterialTheme.colorScheme.onSurface
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                if (isCustomProfile) {
                  IconButton(
                    onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                  ) {
                    Icon(
                      imageVector = Icons.Default.AudioFile,
                      contentDescription = if (hasCustomAudio) "Change audio file" else "Browse audio file",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }

                if (!isCustomProfile || hasCustomAudio) {
                  IconButton(
                    onClick = {
                      viewModel.toggleNoiseProfilePreview(
                        profile = profile,
                        customUriOverride = if (isCustomProfile) effectiveCustomUri else null
                      )
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                  ) {
                    Icon(
                      imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                      contentDescription = if (isPreviewing) "Stop preview" else "Preview ${profile.shortLabel}",
                      tint = if (isPreviewing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
              }

              if (isCustomProfile && settings.customAudioError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = settings.customAudioError!!,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.error,
                  fontSize = 11.sp
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}
