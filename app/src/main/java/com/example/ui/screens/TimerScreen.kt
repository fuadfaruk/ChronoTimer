package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimeFormatter
import com.example.ui.components.AnimatedAudioWaves
import com.example.ui.components.CircularProgressTimerArc
import com.example.ui.components.NumberStepper
import com.example.ui.components.WhiteNoiseSheet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.viewmodel.ChronoViewModel

@Composable
fun TimerScreen(
  viewModel: ChronoViewModel,
  modifier: Modifier = Modifier
) {
  val timerState by viewModel.timerState.collectAsState()
  val settings by viewModel.settings.collectAsState()
  val activeTimer = timerState.timers.find { it.id == timerState.selectedTimerId }
    ?: timerState.timers.firstOrNull()

  var showEditDurationDialog by remember { mutableStateOf(false) }
  var showWhiteNoiseSheet by remember { mutableStateOf(false) }

  if (activeTimer == null) return

  val timerColor = Color(activeTimer.colorHex)
  val isRunning = activeTimer.isRunning
  val currentTotalSec = (activeTimer.initialDurationMillis / 1000L).toInt()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // 1. Sleek Focus Noise Pill (Distraction-masking audio)
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = if (settings.isWhiteNoisePlaying)
        NeonGreen.copy(alpha = 0.15f)
      else if (settings.whiteNoiseEnabled)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
      else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (settings.isWhiteNoisePlaying) NeonGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
      ),
      modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .clickable { showWhiteNoiseSheet = true }
        .testTag("timer_white_noise_card")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (settings.isWhiteNoisePlaying) {
          AnimatedAudioWaves(color = NeonGreen)
        } else {
          Icon(
            imageVector = Icons.Default.Headphones,
            contentDescription = null,
            tint = if (settings.whiteNoiseEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }

        Text(
          text = if (settings.isWhiteNoisePlaying)
            "Study Noise: Active"
          else if (settings.whiteNoiseEnabled)
            "Study Noise: On"
          else
            "Study Noise: Off",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = if (settings.isWhiteNoisePlaying) NeonGreen else MaterialTheme.colorScheme.onSurface
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

    // Active Timers Switcher (when multiple timers exist, e.g. started from Presets)
    if (timerState.timers.size > 1) {
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          timerState.timers.forEach { timer ->
            val isSelected = timer.id == activeTimer.id
            val chipColor = Color(timer.colorHex)
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (isSelected) chipColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              border = BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) chipColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
              ),
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { viewModel.selectTimer(timer.id) }
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
                    .background(if (timer.isRunning) NeonGreen else chipColor)
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

    Spacer(modifier = Modifier.height(12.dp))

    // Active Timer Title Badge
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = timerColor.copy(alpha = 0.12f),
      border = BorderStroke(1.dp, timerColor.copy(alpha = 0.45f))
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(timerColor)
        )
        Text(
          text = activeTimer.title.uppercase(),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = timerColor,
          letterSpacing = 0.6.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Minimalist Dynamic Countdown Arc (Tap to set duration directly)
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .clip(CircleShape)
        .clickable {
          if (!isRunning) showEditDurationDialog = true
        }
    ) {
      CircularProgressTimerArc(
        progress = activeTimer.progress,
        remainingMillis = activeTimer.remainingMillis,
        isRunning = isRunning,
        timerColor = if (isRunning) timerColor else MaterialTheme.colorScheme.primary,
        size = 240.dp
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 3. Dynamic Quick Study Preset Pills
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.Center
    ) {
      val presets = listOf(
        15 * 60 to "15m",
        25 * 60 to "25m",
        45 * 60 to "45m",
        50 * 60 to "50m",
        60 * 60 to "60m"
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { (sec, label) ->
          val isSelected = currentTotalSec == sec
          FilterChip(
            selected = isSelected,
            onClick = {
              viewModel.updateCurrentTimerDuration(activeTimer.id, sec.toLong())
            },
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
            )
          )
        }

        // Custom time button
        FilterChip(
          selected = false,
          onClick = { showEditDurationDialog = true },
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
          shape = RoundedCornerShape(16.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 4. Primary Minimalist Action Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Reset button
      FilledTonalButton(
        onClick = { viewModel.resetTimer(activeTimer.id) },
        shape = CircleShape,
        modifier = Modifier
          .size(58.dp)
          .testTag("timer_reset_btn"),
        contentPadding = PaddingValues(0.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Reset Timer",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(18.dp))

      // Start / Pause Primary Button
      Button(
        onClick = {
          if (isRunning) viewModel.pauseTimer(activeTimer.id)
          else viewModel.startTimer(activeTimer.id)
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isRunning) NeonCoral else MaterialTheme.colorScheme.primary,
          contentColor = if (isRunning) Color.White else Color(0xFF00363D)
        ),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
          .height(60.dp)
          .width(140.dp)
          .testTag("timer_start_pause_btn")
      ) {
        Icon(
          imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = if (isRunning) "Pause" else "Start",
          modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isRunning) "PAUSE" else "START",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      }

      Spacer(modifier = Modifier.width(18.dp))

      // Quick +5 min extension button
      FilledTonalButton(
        onClick = { viewModel.addTimeToTimer(activeTimer.id, 300L) },
        shape = CircleShape,
        modifier = Modifier
          .size(58.dp)
          .testTag("timer_add_new_btn"),
        contentPadding = PaddingValues(0.dp)
      ) {
        Text(
          text = "+5m",
          fontFamily = RobotoMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }

  // Study Focus Noise Dialog Sheet
  if (showWhiteNoiseSheet) {
    WhiteNoiseSheet(
      viewModel = viewModel,
      onDismiss = { showWhiteNoiseSheet = false }
    )
  }

  // Edit Duration Dialog
  if (showEditDurationDialog) {
    EditDurationDialog(
      initialSeconds = currentTotalSec,
      onDismiss = { showEditDurationDialog = false },
      onConfirm = { newSeconds ->
        viewModel.updateCurrentTimerDuration(activeTimer.id, newSeconds.toLong())
        showEditDurationDialog = false
      }
    )
  }

  // Ringing Alarm Dialog Overlay
  if (timerState.ringingTimerId != null) {
    val ringingTimer = timerState.timers.find { it.id == timerState.ringingTimerId }
    AlertDialog(
      onDismissRequest = { viewModel.dismissAlarm() },
      icon = {
        Icon(
          imageVector = Icons.Default.NotificationsActive,
          contentDescription = "Study Session Complete",
          tint = NeonAmber,
          modifier = Modifier.size(40.dp)
        )
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
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
          Text("Done", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = {
            if (ringingTimer != null) {
              viewModel.addTimeToTimer(ringingTimer.id, 300L)
              viewModel.startTimer(ringingTimer.id)
            }
            viewModel.dismissAlarm()
          }
        ) {
          Text("+5 Min")
        }
      }
    )
  }
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
