package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TimeFormatter
import com.example.ui.components.FluidAnimatedValue
import com.example.ui.components.StopwatchAnalogDigitalDial
import com.example.ui.components.fluidPressScale
import com.example.ui.components.fluidPulseHalo
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.viewmodel.ChronoViewModel

@Composable
fun StopwatchScreen(
  viewModel: ChronoViewModel,
  modifier: Modifier = Modifier
) {
  // Controls & laps only recompose on start/pause/reset/lap events—never on 16ms elapsed ticks
  val controlsState by viewModel.stopwatchControlsState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  val resetInteraction = remember { MutableInteractionSource() }
  val startPauseInteraction = remember { MutableInteractionSource() }
  val lapInteraction = remember { MutableInteractionSource() }

  val primaryBtnColor by animateColorAsState(
    targetValue = if (controlsState.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    animationSpec = tween(320, easing = FastOutSlowInEasing),
    label = "stopwatch_btn_color"
  )
  val primaryContentColor by animateColorAsState(
    targetValue = if (controlsState.isRunning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
    animationSpec = tween(320, easing = FastOutSlowInEasing),
    label = "stopwatch_btn_content"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(10.dp))

    // Isolated 60fps dial readout scope
    StopwatchDialSection(viewModel = viewModel)

    Spacer(modifier = Modifier.height(20.dp))

    // Minimal Controls Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      FilledTonalButton(
        onClick = { viewModel.resetStopwatch() },
        enabled = controlsState.hasElapsed && !controlsState.isRunning,
        interactionSource = resetInteraction,
        shape = CircleShape,
        modifier = Modifier
          .size(58.dp)
          .fluidPressScale(resetInteraction)
          .testTag("stopwatch_reset_btn"),
        contentPadding = PaddingValues(0.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Reset Stopwatch",
          tint = if (controlsState.hasElapsed && !controlsState.isRunning)
            MaterialTheme.colorScheme.onSurface
          else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(18.dp))

      Button(
        onClick = {
          if (controlsState.isRunning) viewModel.pauseStopwatch() else viewModel.startStopwatch()
        },
        interactionSource = startPauseInteraction,
        colors = ButtonDefaults.buttonColors(
          containerColor = primaryBtnColor,
          contentColor = primaryContentColor
        ),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
          .height(60.dp)
          .width(140.dp)
          .fluidPulseHalo(color = primaryBtnColor, isActive = controlsState.isRunning)
          .fluidPressScale(startPauseInteraction)
          .testTag("stopwatch_start_pause_btn")
      ) {
        FluidAnimatedValue(targetState = controlsState.isRunning) { running ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (running) "Pause" else "Start",
              modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (running) "PAUSE" else "START",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(18.dp))

      FilledTonalButton(
        onClick = { viewModel.recordLap() },
        enabled = controlsState.isRunning,
        interactionSource = lapInteraction,
        shape = CircleShape,
        modifier = Modifier
          .size(58.dp)
          .fluidPressScale(lapInteraction)
          .testTag("stopwatch_lap_btn"),
        contentPadding = PaddingValues(0.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Flag,
          contentDescription = "Record Lap",
          tint = if (controlsState.isRunning)
            MaterialTheme.colorScheme.primary
          else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
          modifier = Modifier.size(22.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    AnimatedVisibility(
      visible = controlsState.laps.isNotEmpty(),
      enter = fadeIn(tween(240)) + expandVertically(),
      exit = fadeOut(tween(180)) + shrinkVertically()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "LAPS (${controlsState.laps.size})",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(
          onClick = {
            val current = viewModel.stopwatchState.value
            val summary = StringBuilder("ChronoTimer Laps\n\n")
            summary.append("Total Time: ${TimeFormatter.formatStopwatch(current.elapsedMillis)}\n")
            current.laps.forEach { lap ->
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

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      itemsIndexed(controlsState.laps, key = { _, lap -> lap.lapNumber }) { index, lap ->
        val isFastest = controlsState.stats.bestLapIndex == index
        val isSlowest = controlsState.stats.worstLapIndex == index
        val cardColor by animateColorAsState(
          targetValue = when {
            isFastest -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            isSlowest -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
          },
          animationSpec = tween(300),
          label = "lap_card_color"
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = cardColor,
          modifier = Modifier
            .fillMaxWidth()
            .animateItem(
              fadeInSpec = tween(250),
              fadeOutSpec = tween(200),
              placementSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
              )
            )
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
                isFastest -> MaterialTheme.colorScheme.secondary
                isSlowest -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onBackground
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StopwatchDialSection(viewModel: ChronoViewModel) {
  val state by viewModel.stopwatchState.collectAsStateWithLifecycle()
  StopwatchAnalogDigitalDial(
    elapsedMillis = state.elapsedMillis,
    isRunning = state.isRunning,
    size = 250.dp
  )
}
