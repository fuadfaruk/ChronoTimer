package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.util.NoiseProfile
import com.example.viewmodel.ChronoViewModel
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WhiteNoiseSheet(
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
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .fluidPulseHalo(color = MaterialTheme.colorScheme.primary, isActive = settings.isWhiteNoisePlaying)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Headphones,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
        Column {
          Text(
            text = "Study Focus Audio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Mask chatter & study uninterrupted",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Auto-play switch
        val switchCardBg by animateColorAsState(
          targetValue = if (settings.whiteNoiseEnabled)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
          else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          animationSpec = tween(280),
          label = "noise_switch_card_bg"
        )
        val switchCardBorder by animateColorAsState(
          targetValue = if (settings.whiteNoiseEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
          animationSpec = tween(280),
          label = "noise_switch_card_border"
        )

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = switchCardBg),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, switchCardBorder, RoundedCornerShape(14.dp))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "Play while studying",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Plays speech-masking noise during active timer sessions to drown out nearby distractions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = settings.whiteNoiseEnabled,
              onCheckedChange = { viewModel.toggleWhiteNoise() },
              modifier = Modifier.testTag("white_noise_enable_switch")
            )
          }
        }

        // Noise Profile Selection
        Text(
          text = "FOCUS SOUND PROFILE",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          NoiseProfile.values().forEach { profile ->
            val isSelected = settings.whiteNoiseProfile == profile
            val cardInteraction = remember(profile) { MutableInteractionSource() }
            val cardBg by animateColorAsState(
              targetValue = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
              else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              animationSpec = tween(260),
              label = "profile_bg_${profile.name}"
            )
            val cardBorder by animateColorAsState(
              targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
              animationSpec = tween(260),
              label = "profile_border_${profile.name}"
            )

            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = cardBg),
              modifier = Modifier
                .fillMaxWidth()
                .fluidPressScale(cardInteraction)
                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                .clickable(
                  interactionSource = cardInteraction,
                  indication = LocalIndication.current
                ) { viewModel.setWhiteNoiseProfile(profile) }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
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

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                  Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                  )
                }
              }
            }
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        // Volume Slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Volume",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
            }
            Text(
              text = "${(settings.whiteNoiseVolume * 100).toInt()}%",
              fontFamily = RobotoMonoFontFamily,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 13.sp
            )
          }

          Slider(
            value = settings.whiteNoiseVolume,
            onValueChange = { viewModel.setWhiteNoiseVolume(it) },
            valueRange = 0.05f..1.0f,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.testTag("white_noise_volume_slider")
          )
        }

        // Test Sound Preview Button
        val previewInteraction = remember { MutableInteractionSource() }
        OutlinedButton(
          onClick = { viewModel.toggleWhiteNoisePreview() },
          interactionSource = previewInteraction,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .fluidPressScale(previewInteraction)
            .testTag("white_noise_test_preview_btn")
        ) {
          Icon(
            imageVector = if (settings.isWhiteNoisePlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (settings.isWhiteNoisePlaying) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (settings.isWhiteNoisePlaying) "Stop Audio Preview" else "Test Focus Audio",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
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

@Composable
fun AnimatedAudioWaves(
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.secondary
) {
  val transition = rememberInfiniteTransition(label = "audio_waves")
  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1100, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wave_phase"
  )

  Canvas(
    modifier = modifier
      .width(22.dp)
      .height(16.dp)
  ) {
    val barCount = 5
    val spacing = size.width / barCount
    val centerY = size.height / 2f
    val maxHalfH = size.height * 0.44f
    val minHalfH = size.height * 0.14f

    for (i in 0 until barCount) {
      val x = spacing * i + spacing / 2f
      val wave = 0.5f + 0.5f * sin(phase + i * 1.15f)
      val halfH = minHalfH + (maxHalfH - minHalfH) * wave
      drawLine(
        color = color,
        start = Offset(x, centerY - halfH),
        end = Offset(x, centerY + halfH),
        strokeWidth = 2.4.dp.toPx(),
        cap = StrokeCap.Round
      )
    }
  }
}
