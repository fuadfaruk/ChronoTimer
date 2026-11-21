package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimeFormatter
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RobotoMonoFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StopwatchAnalogDigitalDial(
  elapsedMillis: Long,
  isRunning: Boolean,
  modifier: Modifier = Modifier,
  size: Dp = 250.dp
) {
  val (mainTime, decimal) = TimeFormatter.formatStopwatchParts(elapsedMillis)

  val infiniteTransition = rememberInfiniteTransition(label = "dial_glow")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.75f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  val secondsAngle = ((elapsedMillis % 60000) / 60000f) * 360f

  val primaryColor = MaterialTheme.colorScheme.primary
  val surfaceColor = MaterialTheme.colorScheme.surface
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
  val outlineColor = MaterialTheme.colorScheme.outline

  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(
        Brush.radialGradient(
          colors = listOf(
            surfaceVariant.copy(alpha = 0.85f),
            surfaceColor
          )
        )
      )
      .border(1.5.dp, outlineColor.copy(alpha = 0.5f), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(this.size.width / 2f, this.size.height / 2f)
      val radius = this.size.width / 2f - 14.dp.toPx()

      // Outer track circle
      drawCircle(
        color = outlineColor.copy(alpha = 0.3f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
      )

      // Draw dial ticks: 60 ticks (one per second)
      val tickRadiusOuter = radius - 2.dp.toPx()
      for (i in 0 until 60) {
        val angleRad = (i * 6f) * (PI / 180f) - (PI / 2f)
        val isMajor = i % 5 == 0
        val tickLength = if (isMajor) 8.dp.toPx() else 4.dp.toPx()
        val tickStroke = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
        val tickColor = if (isMajor) primaryColor else outlineColor.copy(alpha = 0.4f)

        val startX = center.x + ((tickRadiusOuter - tickLength) * cos(angleRad)).toFloat()
        val startY = center.y + ((tickRadiusOuter - tickLength) * sin(angleRad)).toFloat()
        val endX = center.x + (tickRadiusOuter * cos(angleRad)).toFloat()
        val endY = center.y + (tickRadiusOuter * sin(angleRad)).toFloat()

        drawLine(
          color = tickColor,
          start = Offset(startX, startY),
          end = Offset(endX, endY),
          strokeWidth = tickStroke,
          cap = StrokeCap.Round
        )
      }

      // Elapsed smooth arc
      val sweepArcAngle = (elapsedMillis % 60000) / 60000f * 360f
      drawArc(
        brush = Brush.sweepGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.2f),
            primaryColor
          )
        ),
        startAngle = -90f,
        sweepAngle = sweepArcAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
      )

      // Needle hand
      rotate(degrees = secondsAngle, pivot = center) {
        drawLine(
          color = if (isRunning) primaryColor else outlineColor,
          start = center,
          end = Offset(center.x, center.y - radius + 10.dp.toPx()),
          strokeWidth = 2.5.dp.toPx(),
          cap = StrokeCap.Round
        )

        drawCircle(
          color = NeonAmber,
          radius = 3.5.dp.toPx(),
          center = Offset(center.x, center.y - radius + 10.dp.toPx())
        )
      }

      drawCircle(
        color = surfaceColor,
        radius = 7.dp.toPx(),
        center = center
      )
      drawCircle(
        color = if (isRunning) primaryColor.copy(alpha = pulseAlpha) else outlineColor,
        radius = 4.dp.toPx(),
        center = center
      )
    }

    // Centered Digital Readout
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(top = 30.dp)
    ) {
      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = mainTime,
          fontFamily = RobotoMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 32.sp,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = decimal,
          fontFamily = RobotoMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 20.sp,
          color = primaryColor,
          modifier = Modifier.padding(bottom = 2.dp)
        )
      }
    }
  }
}

@Composable
fun CircularProgressTimerArc(
  progress: Float,
  remainingMillis: Long,
  isRunning: Boolean,
  timerColor: Color = NeonCyan,
  modifier: Modifier = Modifier,
  size: Dp = 250.dp
) {
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
  val outlineColor = MaterialTheme.colorScheme.outline
  val displayTime = TimeFormatter.formatTimer(remainingMillis)

  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(
        Brush.radialGradient(
          colors = listOf(
            surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surface
          )
        )
      )
      .border(1.dp, outlineColor.copy(alpha = 0.35f), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
      val center = Offset(this.size.width / 2f, this.size.height / 2f)
      val radius = (this.size.width / 2f) - 6.dp.toPx()

      // Background track
      drawCircle(
        color = outlineColor.copy(alpha = 0.2f),
        radius = radius,
        center = center,
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
      )

      // Active progress arc
      val sweepAngle = progress * 360f
      drawArc(
        brush = Brush.sweepGradient(
          colors = listOf(
            timerColor.copy(alpha = 0.6f),
            timerColor
          )
        ),
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
      )
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = displayTime,
        fontFamily = RobotoMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = if (displayTime.length > 5) 34.sp else 44.sp,
        color = MaterialTheme.colorScheme.onBackground
      )
      if (isRunning) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(NeonGreen)
          )
          Text(
            text = "FOCUSING",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            letterSpacing = 1.sp
          )
        }
      }
    }
  }
}

@Composable
fun NumberStepper(
  value: Int,
  label: String,
  onValueChange: (Int) -> Unit,
  min: Int = 0,
  max: Int = 59,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
      .padding(horizontal = 8.dp, vertical = 8.dp)
  ) {
    Text(
      text = label.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.Bold,
      fontSize = 11.sp
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Plus button
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface)
        .clickable {
          if (value < max) onValueChange(value + 1) else onValueChange(min)
        },
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "Increment $label",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp)
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = String.format("%02d", value),
      fontFamily = RobotoMonoFontFamily,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Minus button
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface)
        .clickable {
          if (value > min) onValueChange(value - 1) else onValueChange(max)
        },
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Remove,
        contentDescription = "Decrement $label",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
