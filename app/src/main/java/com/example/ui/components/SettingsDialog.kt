package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.util.NoiseProfile
import com.example.viewmodel.ChronoViewModel

@Composable
fun SettingsDialog(
  viewModel: ChronoViewModel,
  onDismiss: () -> Unit
) {
  val settings by viewModel.settings.collectAsState()

  AlertDialog(
    onDismissRequest = {
      viewModel.stopWhiteNoisePreview()
      onDismiss()
    },
    title = {
      Text(
        text = "Settings",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Study Focus Noise Section
        Text(
          text = "STUDY FOCUS AUDIO",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (settings.whiteNoiseEnabled)
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              1.dp,
              if (settings.whiteNoiseEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
              RoundedCornerShape(14.dp)
            )
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Headphones,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
                Column {
                  Text(
                    text = "Play while studying",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
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

            if (settings.whiteNoiseEnabled) {
              Spacer(modifier = Modifier.height(10.dp))

              // Profile selection chips
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NoiseProfile.values().forEach { profile ->
                  val isSelected = settings.whiteNoiseProfile == profile
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .clickable { viewModel.setWhiteNoiseProfile(profile) }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Box(
                        modifier = Modifier
                          .size(12.dp)
                          .clip(CircleShape)
                          .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                          .border(
                            1.5.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape
                          )
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = profile.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Volume slider
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Volume",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "${(settings.whiteNoiseVolume * 100).toInt()}%",
                  fontFamily = RobotoMonoFontFamily,
                  fontSize = 12.sp,
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
                )
              )

              // Test Preview button
              OutlinedButton(
                onClick = { viewModel.toggleWhiteNoisePreview() },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp)
              ) {
                Icon(
                  imageVector = if (settings.isWhiteNoisePlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                  contentDescription = null,
                  tint = if (settings.isWhiteNoisePlaying) NeonAmber else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (settings.isWhiteNoisePlaying) "Stop Test Audio" else "Test Focus Audio",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        Text(
          text = "GENERAL",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        // Sound toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VolumeUp,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Alert Tones & Sound",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
          }
          Switch(
            checked = settings.soundEnabled,
            onCheckedChange = { viewModel.toggleSound() }
          )
        }

        // Vibration toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Vibration,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Haptic Vibration",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
          }
          Switch(
            checked = settings.vibrationEnabled,
            onCheckedChange = { viewModel.toggleVibration() }
          )
        }

        // Keep Screen On
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.LockClock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Keep Screen Awake",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
          }
          Switch(
            checked = settings.keepScreenOn,
            onCheckedChange = { viewModel.toggleKeepScreenOn() }
          )
        }

        // Theme Mode Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Brightness4,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Dark Mode",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
          }
          Switch(
            checked = settings.isDarkTheme,
            onCheckedChange = { viewModel.toggleTheme() }
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          viewModel.stopWhiteNoisePreview()
          onDismiss()
        }
      ) {
        Text("Done")
      }
    }
  )
}
