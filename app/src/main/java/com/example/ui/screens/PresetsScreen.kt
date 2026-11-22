package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.model.TimeFormatter
import com.example.model.TimerPreset
import com.example.ui.components.CustomIntervalPresetEditorDialog
import com.example.ui.components.NumberStepper
import com.example.ui.components.fluidPressScale
import com.example.ui.components.fluidPulseHalo
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.contrastingContentColor
import com.example.ui.theme.resolveThemeAccentColor
import com.example.viewmodel.AppTab
import com.example.viewmodel.ChronoViewModel

private sealed interface PresetListItem {
  val key: String

  data class Timer(val preset: TimerPreset, val isCustom: Boolean) : PresetListItem {
    override val key: String = "timer_${preset.id}"
  }

  data class Interval(val config: IntervalConfig, val isCustom: Boolean) : PresetListItem {
    override val key: String = "interval_${config.id}"
  }
}

private val presetColorPalette = listOf(
  0xFF00E5FFL,
  0xFF00E676L,
  0xFFFF9100L,
  0xFFB388FFL,
  0xFFFF5252L
)

@Composable
fun PresetsScreen(
  viewModel: ChronoViewModel,
  modifier: Modifier = Modifier
) {
  val customPresets by viewModel.customPresets.collectAsState()
  val customIntervalPresets by viewModel.customIntervalPresets.collectAsState()
  val timerState by viewModel.timerState.collectAsState()
  val intervalState by viewModel.intervalState.collectAsState()

  var selectedCategory by remember { mutableStateOf("All") }
  val categories = listOf("All", "Study", "Break", "Intervals", "Custom")

  val itemsToShow = remember(customPresets, customIntervalPresets, selectedCategory) {
    val timerItems = (customPresets + PresetDefaults.defaultTimerPresets).map { preset ->
      PresetListItem.Timer(
        preset = preset,
        isCustom = customPresets.any { it.id == preset.id }
      )
    }
    val intervalItems = (customIntervalPresets + PresetDefaults.defaultIntervalConfigs).map { config ->
      PresetListItem.Interval(
        config = config,
        isCustom = config.isCustom
      )
    }

    when (selectedCategory) {
      "All" -> timerItems.filter { it.isCustom } +
        intervalItems.filter { it.isCustom } +
        timerItems.filter { !it.isCustom } +
        intervalItems.filter { !it.isCustom }
      "Intervals" -> intervalItems
      "Custom" -> timerItems.filter { it.isCustom || it.preset.category.equals("Custom", ignoreCase = true) } +
        intervalItems.filter { it.isCustom }
      else -> timerItems.filter { it.preset.category.equals(selectedCategory, ignoreCase = true) }
    }
  }

  var showAddPresetDialog by remember { mutableStateOf(false) }
  var showIntervalEditor by remember { mutableStateOf(false) }
  var editingIntervalPreset by remember { mutableStateOf<IntervalConfig?>(null) }

  val colorScheme = MaterialTheme.colorScheme
  val runningTimer = timerState.timers.firstOrNull { it.isRunning }
  val fabInteraction = remember { MutableInteractionSource() }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddPresetDialog = true },
        interactionSource = fabInteraction,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = Modifier
          .fluidPulseHalo(color = MaterialTheme.colorScheme.primary, isActive = true)
          .fluidPressScale(fabInteraction, pressedScale = 0.90f)
          .testTag("add_custom_preset_fab")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Create Custom Preset"
        )
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
      // Active Running Timer Banner (if a timer is running, allow jumping back to it)
      AnimatedVisibility(
        visible = runningTimer != null,
        enter = fadeIn(tween(240)) + expandVertically(),
        exit = fadeOut(tween(180)) + shrinkVertically()
      ) {
        runningTimer?.let { running ->
          val bannerInteraction = remember(running.id) { MutableInteractionSource() }
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp)
              .fluidPressScale(bannerInteraction)
              .clip(RoundedCornerShape(14.dp))
              .clickable(
                interactionSource = bannerInteraction,
                indication = LocalIndication.current
              ) {
                viewModel.selectTimer(running.id)
                viewModel.setTab(AppTab.TIMER)
              }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.HourglassBottom,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Running: ${running.title}",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Text(
                text = TimeFormatter.formatTimer(running.remainingMillis),
                fontFamily = RobotoMonoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
            }
          }
        }
      }

      // Category Pills
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(categories) { category ->
          val isSelected = selectedCategory == category
          val chipInteraction = remember(category) { MutableInteractionSource() }
          FilterChip(
            selected = isSelected,
            onClick = { selectedCategory = category },
            interactionSource = chipInteraction,
            label = { Text(category, fontSize = 12.sp) },
            shape = RoundedCornerShape(16.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.fluidPressScale(chipInteraction)
          )
        }
      }

      // Presets List
      if (itemsToShow.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "No custom presets yet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Tap + to create a custom timer or multi-interval preset",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentPadding = PaddingValues(bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(itemsToShow, key = { it.key }) { item ->
            val itemModifier = Modifier.animateItem(
              fadeInSpec = tween(260),
              fadeOutSpec = tween(200),
              placementSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
              )
            )

            when (item) {
              is PresetListItem.Timer -> {
                val preset = item.preset
                val accentColor = resolveThemeAccentColor(preset.colorHex, colorScheme)
                val isTimerActive = timerState.timers.any {
                  it.isRunning && it.title == preset.title &&
                    it.initialDurationMillis == preset.durationSeconds * 1000L
                }
                val startInteraction = remember(item.key) { MutableInteractionSource() }
                val borderColor by animateColorAsState(
                  targetValue = if (isTimerActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                  animationSpec = tween(280),
                  label = "preset_border_${item.key}"
                )

                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                  border = BorderStroke(1.dp, borderColor),
                  modifier = itemModifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(width = 3.dp, height = 34.dp)
                          .clip(RoundedCornerShape(2.dp))
                          .background(accentColor)
                      )

                      Spacer(modifier = Modifier.width(12.dp))

                      Column {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                          Text(
                            text = preset.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          if (isTimerActive) {
                            Text(
                              text = "ACTIVE",
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Bold,
                              color = MaterialTheme.colorScheme.secondary
                            )
                          }
                        }
                        Text(
                          text = "${TimeFormatter.formatDurationSummary(preset.durationSeconds)} • ${preset.category}",
                          fontFamily = RobotoMonoFontFamily,
                          fontSize = 12.sp,
                          color = accentColor,
                          fontWeight = FontWeight.Bold
                        )
                      }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (item.isCustom) {
                        IconButton(
                          onClick = { viewModel.deleteCustomPreset(preset.id) },
                          modifier = Modifier.size(36.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Preset",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                          )
                        }
                      }

                      Button(
                        onClick = { viewModel.applyPresetToTimer(preset, startImmediately = true) },
                        interactionSource = startInteraction,
                        colors = ButtonDefaults.buttonColors(
                          containerColor = accentColor,
                          contentColor = contrastingContentColor(accentColor, colorScheme)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                          .height(34.dp)
                          .fluidPressScale(startInteraction, pressedScale = 0.91f)
                      ) {
                        Icon(
                          imageVector = Icons.Default.PlayArrow,
                          contentDescription = "Start",
                          modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = "START",
                          fontWeight = FontWeight.Bold,
                          fontSize = 11.sp
                        )
                      }
                    }
                  }
                }
              }

              is PresetListItem.Interval -> {
                val config = item.config
                val accentColor = if (config.isMultiStep && config.steps.isNotEmpty()) {
                  resolveThemeAccentColor(config.steps.first().colorHex, colorScheme)
                } else {
                  colorScheme.secondary
                }
                val isRunningThis = intervalState.isRunning && intervalState.config.id == config.id
                val soundCount = config.steps.count { it.backgroundSound != null }
                val subtitle = if (config.isMultiStep) {
                  val base = "${config.steps.size} intervals • ${TimeFormatter.formatDurationSummary(config.totalDurationSeconds.toLong())}"
                  if (soundCount > 0) "$base • ${soundCount} sound${if (soundCount > 1) "s" else ""}" else base
                } else {
                  "Interval • ${TimeFormatter.formatDurationSummary(config.totalDurationSeconds.toLong())}"
                }
                val startInteraction = remember(item.key) { MutableInteractionSource() }
                val borderColor by animateColorAsState(
                  targetValue = if (isRunningThis) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                  animationSpec = tween(280),
                  label = "interval_preset_border_${item.key}"
                )

                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                  border = BorderStroke(1.dp, borderColor),
                  modifier = itemModifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(width = 3.dp, height = 34.dp)
                          .clip(RoundedCornerShape(2.dp))
                          .background(accentColor)
                      )

                      Spacer(modifier = Modifier.width(12.dp))

                      Column {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                          )
                          Text(
                            text = config.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          if (soundCount > 0) {
                            Icon(
                              imageVector = Icons.Default.Headphones,
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.size(11.dp)
                            )
                          }
                          Text(
                            text = subtitle,
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 11.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                      }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (item.isCustom) {
                        IconButton(
                          onClick = {
                            editingIntervalPreset = config
                            showIntervalEditor = true
                          },
                          modifier = Modifier.size(34.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Interval Preset",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                          )
                        }
                        IconButton(
                          onClick = { viewModel.deleteCustomIntervalPreset(config.id) },
                          modifier = Modifier.size(34.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Interval Preset",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                          )
                        }
                      }

                      Button(
                        onClick = { viewModel.applyIntervalPreset(config, startImmediately = true) },
                        interactionSource = startInteraction,
                        colors = ButtonDefaults.buttonColors(
                          containerColor = accentColor,
                          contentColor = contrastingContentColor(accentColor, colorScheme)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                          .height(34.dp)
                          .fluidPressScale(startInteraction, pressedScale = 0.91f)
                      ) {
                        Icon(
                          imageVector = Icons.Default.PlayArrow,
                          contentDescription = "Start",
                          modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = "START",
                          fontWeight = FontWeight.Bold,
                          fontSize = 11.sp
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    if (showAddPresetDialog) {
      AddCustomPresetDialog(
        defaultCategory = if (selectedCategory in listOf("Study", "Break", "Custom")) selectedCategory else "Custom",
        onDismiss = { showAddPresetDialog = false },
        onSave = { title, durationSeconds, category, colorHex ->
          viewModel.addCustomPreset(title, durationSeconds, category, colorHex)
          showAddPresetDialog = false
        },
        onCreateIntervalPreset = {
          showAddPresetDialog = false
          editingIntervalPreset = null
          showIntervalEditor = true
        }
      )
    }

    if (showIntervalEditor) {
      CustomIntervalPresetEditorDialog(
        viewModel = viewModel,
        editingPreset = editingIntervalPreset,
        onDismiss = { showIntervalEditor = false },
        onSave = { savedPreset ->
          viewModel.saveCustomIntervalPreset(savedPreset)
          showIntervalEditor = false
        },
        onDelete = { presetId ->
          viewModel.deleteCustomIntervalPreset(presetId)
          showIntervalEditor = false
        }
      )
    }
  }
}

@Composable
fun AddCustomPresetDialog(
  defaultCategory: String = "Custom",
  onDismiss: () -> Unit,
  onSave: (title: String, durationSeconds: Long, category: String, colorHex: Long) -> Unit,
  onCreateIntervalPreset: (() -> Unit)? = null
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(defaultCategory) }
  var selectedColor by remember { mutableLongStateOf(presetColorPalette.first()) }
  var minutes by remember { mutableIntStateOf(25) }
  var hours by remember { mutableIntStateOf(0) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Study Preset", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title") },
          placeholder = { Text("e.g. Essay Writing, Math Problem Set") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Category Selector
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("Study", "Break", "Custom").forEach { cat ->
            val isSelected = category == cat
            FilterChip(
              selected = isSelected,
              onClick = { category = cat },
              label = { Text(cat, fontSize = 11.sp) },
              shape = RoundedCornerShape(12.dp)
            )
          }
        }

        // Color Picker Dots
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Color",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          val dialogColorScheme = MaterialTheme.colorScheme
          presetColorPalette.forEach { colorHex ->
            val isColorSelected = selectedColor == colorHex
            val swatchColor = resolveThemeAccentColor(colorHex, dialogColorScheme)
            val dotSize by animateDpAsState(
              targetValue = if (isColorSelected) 24.dp else 19.dp,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
              ),
              label = "preset_dot_size"
            )
            Box(
              modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(swatchColor)
                .border(
                  width = if (isColorSelected) 2.dp else 0.dp,
                  color = if (isColorSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                  shape = CircleShape
                )
                .clickable { selectedColor = colorHex }
            )
          }
        }

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
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
          )
          NumberStepper(
            value = minutes,
            label = "Mins",
            onValueChange = { minutes = it },
            min = 1,
            max = 59,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
          )
        }

        if (onCreateIntervalPreset != null) {
          OutlinedButton(
            onClick = onCreateIntervalPreset,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Default.DirectionsRun,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create Multi-Interval Preset with Sounds", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val totalSec = hours * 3600L + minutes * 60L
          onSave(
            title.ifBlank { "Study Session" },
            if (totalSec > 0L) totalSec else 1500L,
            category.ifBlank { "Custom" },
            selectedColor
          )
        }
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
