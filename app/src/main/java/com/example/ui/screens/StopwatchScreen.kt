package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LapItem
import com.example.model.TimeFormatter
import com.example.ui.components.StopwatchAnalogDigitalDial
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.viewmodel.ChronoViewModel

@Composable
fun StopwatchScreen(
  viewModel: ChronoViewModel,
  modifier: Modifier = Modifier
) {
  val state by viewModel.stopwatchState.collectAsState()
  val context = LocalContext.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(10.dp))

    // Minimalist Dial
    StopwatchAnalogDigitalDial(
      elapsedMillis = state.elapsedMillis,
      isRunning = state.isRunning,
      size = 250.dp
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Minimal Controls Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Reset Button
      FilledTonalButton(
        onClick = { viewModel.resetStopwatch() },
        enabled = state.elapsedMillis > 0L && !state.isRunning,
        shape = CircleShape,
        modifier = Modifier
          .size(58.dp)
          .testTag("stopwatch_reset_btn"),
        contentPadding = PaddingValues(0.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Reset Stopwatch",
          tint = if (state.elapsedMillis > 0L && !state.isRunning)
            MaterialTheme.colorScheme.onSurface
          else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(18.dp))

      // Start / Pause Primary Button
      Button(
        onClick = {
          if (state.isRunning) viewModel.pauseStopwatch() else viewModel.startStopwatch()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (state.isRunning) NeonCoral else NeonCyan,
          contentColor = if (state.isRunning) Color.White else Color(0xFF00363D)
        ),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
          .height(60.dp)
          .width(140.dp)
          .testTag("stopwatch_start_pause_btn")
      ) {
        Icon(
          imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = if (state.isRunning) "Pause" else "Start",
          modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (state.isRunning) "PAUSE" else "START",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      }

      Spacer(modifier = Modifier.width(18.dp))

      // Lap / Split Button
      FilledTonalButton(
        onClick = { viewModel.recordLap() },
        enabled = state.isRunning,
        shape = CircleShape,
        modifier = Modifier
          .size(58.dp)
          .testTag("stopwatch_lap_btn"),
        contentPadding = PaddingValues(0.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Flag,
          contentDescription = "Record Lap",
          tint = if (state.isRunning)
            MaterialTheme.colorScheme.primary
          else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
          modifier = Modifier.size(22.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Minimal Laps Header
    if (state.laps.isNotEmpty()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "LAPS (${state.laps.size})",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(
          onClick = {
            val summary = StringBuilder("ChronoTimer Laps\n\n")
            summary.append("Total Time: ${TimeFormatter.formatStopwatch(state.elapsedMillis)}\n")
            state.laps.forEach { lap ->
              summary.append("Lap ${lap.lapNumber}: ${TimeFormatter.formatStopwatch(lap.lapTimeMillis)}\n")
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, summary.toString())
            }
            context.startActivity(Intent.createChooser(intent, "Share Lap Times"))
          },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share Lap Times",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    // Sleek minimal Laps list
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      itemsIndexed(state.laps) { index, lap ->
        val isFastest = state.stats.bestLapIndex == index
        val isSlowest = state.stats.worstLapIndex == index

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = String.format("Lap %02d", lap.lapNumber),
              fontFamily = RobotoMonoFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
              text = TimeFormatter.formatStopwatch(lap.lapTimeMillis),
              fontFamily = RobotoMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp,
              color = when {
                isFastest -> NeonGreen
                isSlowest -> NeonCoral
                else -> MaterialTheme.colorScheme.onBackground
              }
            )
          }
        }
      }
    }
  }
}
