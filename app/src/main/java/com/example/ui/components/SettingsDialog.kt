package com.example.ui.components

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.TimeFormatter
import com.example.ui.screens.EditQuickAddDialog
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.util.NoiseProfile
import com.example.viewmodel.ChronoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
  viewModel: ChronoViewModel,
  onDismiss: () -> Unit
) {
  val settings by viewModel.settings.collectAsState()
  var showQuickAddDialog by remember { mutableStateOf(false) }
  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      viewModel.setCustomAudioUri(uri, selectAsDefaultProfile = true)
    }
  }

  val dismissDialog = {
    viewModel.stopWhiteNoisePreview()
    onDismiss()
  }

  Dialog(
    onDismissRequest = dismissDialog,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 24.dp),
      contentAlignment = Alignment.Center
    ) {
      val maxDialogHeight = (maxHeight * 0.9f).coerceAtLeast(360.dp)

      Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp)
          .heightIn(max = maxDialogHeight)
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Settings",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
              onClick = dismissDialog,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("settings_close_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

          // Scrollable Body
          Column(
            modifier = Modifier
              .weight(1f, fill = false)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // Study Focus Audio Section
            Text(
              text = "STUDY FOCUS AUDIO",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )

            val audioCardBg by animateColorAsState(
              targetValue = if (settings.whiteNoiseEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
              else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              animationSpec = tween(280),
              label = "settings_audio_bg"
            )
            val audioCardBorder by animateColorAsState(
              targetValue = if (settings.whiteNoiseEnabled)
                MaterialTheme.colorScheme.primary
              else
                MaterialTheme.colorScheme.outlineVariant,
              animationSpec = tween(280),
              label = "settings_audio_border"
            )

            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = audioCardBg),
              border = BorderStroke(1.dp, audioCardBorder),
              modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                  animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                  )
                )
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { viewModel.toggleWhiteNoise() }
                    .padding(vertical = 2.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    modifier = Modifier
                      .weight(1f)
                      .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Headphones,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "Play while studying",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        text = "Masks background speech & chatter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                  Switch(
                    checked = settings.whiteNoiseEnabled,
                    onCheckedChange = { viewModel.toggleWhiteNoise() },
                    modifier = Modifier.testTag("settings_white_noise_switch")
                  )
                }

                AnimatedVisibility(
                  visible = settings.whiteNoiseEnabled,
                  enter = fadeIn(tween(240)) + expandVertically(
                    animationSpec = spring(
                      dampingRatio = Spring.DampingRatioNoBouncy,
                      stiffness = Spring.StiffnessMediumLow
                    )
                  ),
                  exit = fadeOut(tween(180)) + shrinkVertically(animationSpec = tween(200))
                ) {
                  Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Profile selection rows
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      NoiseProfile.entries.forEach { profile ->
                        val isSelected = settings.whiteNoiseProfile == profile
                        val isCustomProfile = profile == NoiseProfile.CUSTOM_AUDIO
                        val hasCustomAudio = !settings.customAudioUri.isNullOrBlank()
                        val chipInteraction = remember(profile) { MutableInteractionSource() }
                        val chipBg by animateColorAsState(
                          targetValue = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                          else
                            MaterialTheme.colorScheme.surface,
                          animationSpec = tween(220),
                          label = "settings_profile_bg_${profile.name}"
                        )
                        Surface(
                          shape = RoundedCornerShape(10.dp),
                          color = chipBg,
                          border = BorderStroke(
                            1.dp,
                            if (isSelected)
                              MaterialTheme.colorScheme.primary
                            else
                              MaterialTheme.colorScheme.outlineVariant
                          ),
                          modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .fluidPressScale(chipInteraction)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                              interactionSource = chipInteraction,
                              indication = LocalIndication.current
                            ) {
                              if (isCustomProfile && !hasCustomAudio) {
                                audioPickerLauncher.launch(arrayOf("audio/*"))
                              } else {
                                viewModel.setWhiteNoiseProfile(profile)
                              }
                            }
                            .testTag("settings_profile_${profile.name}")
                        ) {
                          Column(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(horizontal = 12.dp, vertical = 10.dp)
                          ) {
                            Row(
                              modifier = Modifier.fillMaxWidth(),
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Box(
                                modifier = Modifier
                                  .size(14.dp)
                                  .clip(CircleShape)
                                  .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                  )
                                  .border(
                                    1.5.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                  )
                              )
                              Spacer(modifier = Modifier.width(10.dp))
                              Column(modifier = Modifier.weight(1f)) {
                                Text(
                                  text = profile.displayName,
                                  style = MaterialTheme.typography.bodyMedium,
                                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                  color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                  else
                                    MaterialTheme.colorScheme.onSurface,
                                  maxLines = 1,
                                  overflow = TextOverflow.Ellipsis
                                )
                                if (isCustomProfile) {
                                  Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                  ) {
                                    if (hasCustomAudio) {
                                      Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                      )
                                    }
                                    Text(
                                      text = if (hasCustomAudio) {
                                        settings.customAudioName ?: "Selected audio file"
                                      } else {
                                        "Tap to choose audio from device"
                                      },
                                      style = MaterialTheme.typography.labelSmall,
                                      color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                      else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                      maxLines = 1,
                                      overflow = TextOverflow.Ellipsis
                                    )
                                  }
                                }
                              }

                              if (isCustomProfile) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                  shape = RoundedCornerShape(8.dp),
                                  color = if (isSelected) {
                                    MaterialTheme.colorScheme.surface
                                  } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                  },
                                  modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { audioPickerLauncher.launch(arrayOf("audio/*")) }
                                    .testTag("settings_custom_audio_browse_btn")
                                ) {
                                  Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                  ) {
                                    Icon(
                                      imageVector = Icons.Default.AudioFile,
                                      contentDescription = null,
                                      tint = MaterialTheme.colorScheme.primary,
                                      modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                      text = if (hasCustomAudio) "Change" else "Browse",
                                      style = MaterialTheme.typography.labelSmall,
                                      fontWeight = FontWeight.Bold,
                                      color = MaterialTheme.colorScheme.primary
                                    )
                                  }
                                }
                                if (hasCustomAudio) {
                                  IconButton(
                                    onClick = { viewModel.clearCustomAudio() },
                                    modifier = Modifier
                                      .size(28.dp)
                                      .testTag("settings_custom_audio_clear_btn")
                                  ) {
                                    Icon(
                                      imageVector = Icons.Default.Close,
                                      contentDescription = "Remove custom audio",
                                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                      modifier = Modifier.size(15.dp)
                                    )
                                  }
                                }
                              }
                            }

                            if (isCustomProfile && settings.customAudioError != null) {
                              Spacer(modifier = Modifier.height(4.dp))
                              Text(
                                text = settings.customAudioError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                              )
                            }
                          }
                        }
                      }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Volume slider
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Volume",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        text = "${(settings.whiteNoiseVolume * 100).toInt()}%",
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                      )
                    }

                    Slider(
                      value = settings.whiteNoiseVolume,
                      onValueChange = { viewModel.setWhiteNoiseVolume(it) },
                      valueRange = 0.05f..1.0f,
                      colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                      ),
                      modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Test Preview button
                    val needsCustomFile = settings.whiteNoiseProfile == NoiseProfile.CUSTOM_AUDIO &&
                      settings.customAudioUri.isNullOrBlank()
                    val testInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                      onClick = {
                        if (needsCustomFile) {
                          audioPickerLauncher.launch(arrayOf("audio/*"))
                        } else {
                          viewModel.toggleWhiteNoisePreview()
                        }
                      },
                      interactionSource = testInteraction,
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .fluidPressScale(testInteraction)
                        .testTag("settings_test_audio_btn")
                    ) {
                      Icon(
                        imageVector = when {
                          needsCustomFile -> Icons.Default.AudioFile
                          settings.isWhiteNoisePlaying -> Icons.Default.Stop
                          else -> Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                        tint = if (settings.isWhiteNoisePlaying && !needsCustomFile) {
                          MaterialTheme.colorScheme.tertiary
                        } else {
                          MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(18.dp)
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = when {
                          needsCustomFile -> "Choose Audio File"
                          settings.isWhiteNoisePlaying -> "Stop Test Audio"
                          else -> "Test Focus Audio"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                  }
                }
              }
            }

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 2.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            Text(
              text = "GENERAL",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )

            SettingsToggleRow(
              icon = Icons.Default.VolumeUp,
              title = "Alert Tones & Sound",
              checked = settings.soundEnabled,
              onToggle = { viewModel.toggleSound() },
              switchTestTag = "settings_sound_switch"
            )

            SettingsToggleRow(
              icon = Icons.Default.Vibration,
              title = "Haptic Vibration",
              checked = settings.vibrationEnabled,
              onToggle = { viewModel.toggleVibration() },
              switchTestTag = "settings_vibration_switch"
            )

            SettingsToggleRow(
              icon = Icons.Default.LockClock,
              title = "Keep Screen Awake",
              checked = settings.keepScreenOn,
              onToggle = { viewModel.toggleKeepScreenOn() },
              switchTestTag = "settings_keep_awake_switch"
            )

            // Theme Mode Selector (System / Light / Dark)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Brightness4,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Follows your Android system theme by default",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
              ) {
                AppThemeMode.entries.forEachIndexed { index, mode ->
                  val isSelected = settings.themeMode == mode
                  SegmentedButton(
                    selected = isSelected,
                    onClick = { viewModel.setThemeMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                      index = index,
                      count = AppThemeMode.entries.size
                    ),
                    colors = SegmentedButtonDefaults.colors(
                      activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                      activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                      inactiveContainerColor = MaterialTheme.colorScheme.surface,
                      inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    icon = {},
                    modifier = Modifier
                      .heightIn(min = 44.dp)
                      .testTag("settings_theme_${mode.name.lowercase()}")
                  ) {
                    Text(
                      text = mode.label,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }

            // Material You Dynamic Color Toggle (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              SettingsToggleRow(
                icon = Icons.Default.Palette,
                title = "Material You Dynamic Color",
                subtitle = "Adapt colors to your device wallpaper",
                checked = settings.dynamicColorEnabled,
                onToggle = { viewModel.toggleDynamicColor() },
                switchTestTag = "settings_dynamic_color_switch"
              )
            }

            // Quick Add Increment
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showQuickAddDialog = true }
                .padding(horizontal = 4.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier
                  .weight(1f)
                  .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.MoreTime,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Timer Quick-Add Step",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Tap to customize minutes & seconds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Text(
                  text = "+${TimeFormatter.formatDurationSummary(settings.quickAddSeconds.toLong())}",
                  fontFamily = RobotoMonoFontFamily,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

          // Footer
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = dismissDialog,
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("settings_done_btn")
            ) {
              Text(
                text = "Done",
                style = MaterialTheme.typography.labelLarge
              )
            }
          }
        }
      }
    }
  }

  if (showQuickAddDialog) {
    EditQuickAddDialog(
      initialTotalSeconds = settings.quickAddSeconds,
      onDismiss = { showQuickAddDialog = false },
      onConfirm = { mins, secs ->
        viewModel.setQuickAddIncrement(mins, secs)
        showQuickAddDialog = false
      }
    )
  }
}

@Composable
private fun SettingsToggleRow(
  icon: ImageVector,
  title: String,
  checked: Boolean,
  onToggle: () -> Unit,
  switchTestTag: String,
  subtitle: String? = null
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable { onToggle() }
      .padding(horizontal = 4.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      modifier = Modifier
        .weight(1f)
        .padding(end = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp)
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = { onToggle() },
      modifier = Modifier.testTag(switchTestTag)
    )
  }
}
