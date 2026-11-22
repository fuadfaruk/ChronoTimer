package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimeFormatter
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
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )
  val blobPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(6500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "stopwatch_blob_phase"
  )

  val runningWeight by animateFloatAsState(
    targetValue = if (isRunning) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "stopwatch_running_weight"
  )

  val dialScale by animateFloatAsState(
    targetValue = if (isRunning) 1.02f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "stopwatch_dial_scale"
  )

  val secondsAngle = ((elapsedMillis % 60000) / 60000f) * 360f

  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val tertiaryColor = MaterialTheme.colorScheme.tertiary
  val surfaceColor = MaterialTheme.colorScheme.surface
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
  val outlineColor = MaterialTheme.colorScheme.outline

  val outerBlobPath = remember { Path() }
  val innerBlobPath = remember { Path() }

  Box(
    modifier = modifier
      .size(size)
      .graphicsLayer {
        scaleX = dialScale
        scaleY = dialScale
      },
    contentAlignment = Alignment.Center
  ) {
    // Organic Morphing Outer Blob Halo
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(this.size.width / 2f, this.size.height / 2f)
      val baseRadius = this.size.minDimension / 2f
      val auraBoost = 0.88f + 0.14f * runningWeight

      createOrganicBlobPath(
        center = center,
        baseRadius = baseRadius * 0.96f * auraBoost,
        phase = blobPhase,
        pointCount = 12,
        wobbleAmplitude = 0.055f + 0.03f * runningWeight,
        harmonicA = 3,
        harmonicB = 4,
        path = outerBlobPath
      )
      drawPath(
        path = outerBlobPath,
        brush = Brush.radialGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.16f + 0.14f * runningWeight),
            secondaryColor.copy(alpha = 0.08f * runningWeight),
            Color.Transparent
          ),
          center = center,
          radius = baseRadius * 1.15f
        )
      )
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(
              surfaceVariant.copy(alpha = 0.85f),
              surfaceColor.copy(alpha = 0.96f)
            )
          )
        )
        .border(
          width = 1.5.dp,
          brush = Brush.sweepGradient(
            colors = listOf(
              outlineColor.copy(alpha = 0.45f),
              primaryColor.copy(alpha = 0.35f + 0.35f * runningWeight),
              outlineColor.copy(alpha = 0.45f)
            )
          ),
          shape = CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.width / 2f - 14.dp.toPx()

        // Subtle inner fluid morphing blob when running
        if (runningWeight > 0.02f) {
          createOrganicBlobPath(
            center = center,
            baseRadius = radius * 0.72f,
            phase = -blobPhase * 1.3f,
            pointCount = 10,
            wobbleAmplitude = 0.09f,
            harmonicA = 2,
            harmonicB = 3,
            path = innerBlobPath
          )
          drawPath(
            path = innerBlobPath,
            brush = Brush.radialGradient(
              colors = listOf(
                primaryColor.copy(alpha = 0.14f * runningWeight),
                Color.Transparent
              ),
              center = center,
              radius = radius * 0.9f
            )
          )
        }

        // Outer track circle
        drawCircle(
          color = outlineColor.copy(alpha = 0.28f),
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

        // Elapsed smooth arc glow + primary arc
        val sweepArcAngle = (elapsedMillis % 60000) / 60000f * 360f
        if (sweepArcAngle > 0.5f) {
          drawArc(
            color = primaryColor.copy(alpha = 0.22f),
            startAngle = -90f,
            sweepAngle = sweepArcAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
          )
        }
        drawArc(
          brush = Brush.sweepGradient(
            colors = listOf(
              primaryColor.copy(alpha = 0.15f),
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

        // Needle hand with luminous droplet tip
        rotate(degrees = secondsAngle, pivot = center) {
          val tipY = center.y - radius + 10.dp.toPx()
          drawLine(
            color = if (isRunning) primaryColor else outlineColor,
            start = center,
            end = Offset(center.x, tipY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
          )

          drawCircle(
            color = tertiaryColor.copy(alpha = 0.3f * runningWeight),
            radius = 7.dp.toPx(),
            center = Offset(center.x, tipY)
          )
          drawCircle(
            color = tertiaryColor,
            radius = 3.5.dp.toPx(),
            center = Offset(center.x, tipY)
          )
        }

        drawCircle(
          color = surfaceColor,
          radius = 7.dp.toPx(),
          center = center
        )
        drawCircle(
          color = if (isRunning) primaryColor.copy(alpha = pulseAlpha) else outlineColor,
          radius = 4.5.dp.toPx(),
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
            fontSize = 31.sp,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = decimal,
            fontFamily = RobotoMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 19.sp,
            color = primaryColor,
            modifier = Modifier.padding(bottom = 2.dp)
          )
        }
      }
    }
  }
}

@Composable
fun CircularProgressTimerArc(
  progress: Float,
  remainingMillis: Long,
  isRunning: Boolean,
  timerColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
  size: Dp = 250.dp,
  showStatusLabel: Boolean = true
) {
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
  val surfaceColor = MaterialTheme.colorScheme.surface
  val outlineColor = MaterialTheme.colorScheme.outline
  val activeLabelColor = MaterialTheme.colorScheme.secondary
  val tipHighlightColor = MaterialTheme.colorScheme.onPrimary
  val displayTime = TimeFormatter.formatTimer(remainingMillis)

  // Smoothly interpolate progress for liquid arc motion
  val animatedProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 320, easing = LinearEasing),
    label = "smooth_timer_progress"
  )

  val animatedColor by animateColorAsState(
    targetValue = timerColor,
    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
    label = "smooth_timer_color"
  )

  val runningWeight by animateFloatAsState(
    targetValue = if (isRunning) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "timer_running_weight"
  )

  val dialScale by animateFloatAsState(
    targetValue = if (isRunning) 1.025f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "timer_dial_scale"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "timer_fluid_anim")
  val blobPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 7000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "timer_blob_phase"
  )
  val wavePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "timer_wave_phase"
  )
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "timer_pulse_scale"
  )

  val outerBlobPath = remember { Path() }
  val secondaryBlobPath = remember { Path() }
  val liquidCircleClip = remember { Path() }
  val wavePath1 = remember { Path() }
  val wavePath2 = remember { Path() }

  Box(
    modifier = modifier
      .size(size)
      .graphicsLayer {
        scaleX = dialScale
        scaleY = dialScale
      },
    contentAlignment = Alignment.Center
  ) {
    // 1. Organic Morphing Outer Blob Aura
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(this.size.width / 2f, this.size.height / 2f)
      val baseRadius = this.size.minDimension / 2f
      val breath = if (isRunning) pulseScale else 1f

      createOrganicBlobPath(
        center = center,
        baseRadius = baseRadius * 0.95f * breath,
        phase = blobPhase,
        pointCount = 10,
        wobbleAmplitude = 0.055f + 0.035f * runningWeight,
        harmonicA = 2,
        harmonicB = 3,
        path = outerBlobPath
      )
      drawPath(
        path = outerBlobPath,
        brush = Brush.radialGradient(
          colors = listOf(
            animatedColor.copy(alpha = 0.20f + 0.16f * runningWeight),
            animatedColor.copy(alpha = 0.06f + 0.06f * runningWeight),
            Color.Transparent
          ),
          center = center,
          radius = baseRadius * 1.18f
        )
      )

      createOrganicBlobPath(
        center = center,
        baseRadius = baseRadius * 0.91f,
        phase = -blobPhase * 1.25f + 1.1f,
        pointCount = 12,
        wobbleAmplitude = 0.045f + 0.03f * runningWeight,
        harmonicA = 3,
        harmonicB = 4,
        path = secondaryBlobPath
      )
      drawPath(
        path = secondaryBlobPath,
        color = animatedColor.copy(alpha = 0.10f + 0.12f * runningWeight),
        style = Stroke(width = 1.5.dp.toPx())
      )
    }

    // 2. Main Circular Dial Body with Liquid Wave & Fluid Progress Arc
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(
              surfaceVariant.copy(alpha = 0.55f),
              surfaceColor.copy(alpha = 0.96f)
            )
          )
        )
        .border(1.dp, outlineColor.copy(alpha = 0.32f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = (this.size.width / 2f) - 6.dp.toPx()

        // Inner circular clip for liquid fill
        val innerRadius = (radius - 7.dp.toPx()).coerceAtLeast(1f)
        liquidCircleClip.reset()
        liquidCircleClip.addOval(
          androidx.compose.ui.geometry.Rect(
            left = center.x - innerRadius,
            top = center.y - innerRadius,
            right = center.x + innerRadius,
            bottom = center.y + innerRadius
          )
        )

        // Subtle animated liquid level inside the dial
        clipPath(liquidCircleClip) {
          val w = this.size.width
          val h = this.size.height
          val waveAmp = (4.dp.toPx() + 3.dp.toPx() * runningWeight)

          createLiquidWavePath(
            width = w,
            height = h,
            fillFraction = animatedProgress,
            wavePhase = wavePhase,
            waveAmplitude = waveAmp,
            path = wavePath1
          )
          drawPath(
            path = wavePath1,
            brush = Brush.verticalGradient(
              colors = listOf(
                animatedColor.copy(alpha = 0.16f + 0.08f * runningWeight),
                animatedColor.copy(alpha = 0.04f)
              )
            )
          )

          createLiquidWavePath(
            width = w,
            height = h,
            fillFraction = (animatedProgress - 0.02f).coerceAtLeast(0f),
            wavePhase = -wavePhase * 1.3f + 1.5f,
            waveAmplitude = waveAmp * 0.8f,
            path = wavePath2
          )
          drawPath(
            path = wavePath2,
            color = animatedColor.copy(alpha = 0.09f + 0.05f * runningWeight)
          )
        }

        // Background track
        drawCircle(
          color = outlineColor.copy(alpha = 0.2f),
          radius = radius,
          center = center,
          style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Outer soft glow arc
        val sweepAngle = animatedProgress * 360f
        if (sweepAngle > 0.5f) {
          drawArc(
            color = animatedColor.copy(alpha = 0.22f + 0.12f * runningWeight),
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
          )
        }

        // Active progress arc
        drawArc(
          brush = Brush.sweepGradient(
            colors = listOf(
              animatedColor.copy(alpha = 0.55f),
              animatedColor
            )
          ),
          startAngle = -90f,
          sweepAngle = sweepAngle,
          useCenter = false,
          topLeft = Offset(center.x - radius, center.y - radius),
          size = Size(radius * 2f, radius * 2f),
          style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Luminous fluid droplet at tip of progress arc
        if (sweepAngle > 1f) {
          val tipAngleRad = ((-90f + sweepAngle) * PI / 180.0)
          val tipX = center.x + (radius * cos(tipAngleRad)).toFloat()
          val tipY = center.y + (radius * sin(tipAngleRad)).toFloat()
          val tipCenter = Offset(tipX, tipY)

          drawCircle(
            color = animatedColor.copy(alpha = 0.35f),
            radius = 9.dp.toPx() * (if (isRunning) pulseScale else 1f),
            center = tipCenter
          )
          drawCircle(
            color = tipHighlightColor.copy(alpha = 0.9f),
            radius = 3.5.dp.toPx(),
            center = tipCenter
          )
        }
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = displayTime,
          fontFamily = RobotoMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = if (displayTime.length > 5) 33.sp else 42.sp,
          color = MaterialTheme.colorScheme.onBackground
        )
        if (isRunning && showStatusLabel) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .graphicsLayer {
                  scaleX = pulseScale
                  scaleY = pulseScale
                }
                .clip(CircleShape)
                .background(activeLabelColor)
            )
            Text(
              text = "FOCUSING",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = activeLabelColor,
              letterSpacing = 1.sp
            )
          }
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
  val plusInteraction = remember { MutableInteractionSource() }
  val minusInteraction = remember { MutableInteractionSource() }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
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

    // Plus button with bouncy spring press
    Box(
      modifier = Modifier
        .size(34.dp)
        .fluidPressScale(plusInteraction, pressedScale = 0.86f)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface)
        .clickable(
          interactionSource = plusInteraction,
          indication = LocalIndication.current
        ) {
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

    FluidAnimatedValue(targetState = value) { animatedVal ->
      Text(
        text = String.format("%02d", animatedVal),
        fontFamily = RobotoMonoFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Minus button with bouncy spring press
    Box(
      modifier = Modifier
        .size(34.dp)
        .fluidPressScale(minusInteraction, pressedScale = 0.86f)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface)
        .clickable(
          interactionSource = minusInteraction,
          indication = LocalIndication.current
        ) {
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

