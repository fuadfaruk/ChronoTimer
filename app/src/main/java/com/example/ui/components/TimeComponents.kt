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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
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

private class StopwatchDialCache {
  val outerBlobPath = Path()
  val innerBlobPath = Path()
  val xBuffer = FloatArray(12)
  val yBuffer = FloatArray(12)

  // Precomputed 60 dial tick coordinates
  val tickStartX = FloatArray(60)
  val tickStartY = FloatArray(60)
  val tickEndX = FloatArray(60)
  val tickEndY = FloatArray(60)
  var lastTickSize = -1f
  var lastOuterRadius = -1f

  var lastPrimary = Color.Unspecified
  var lastSecondary = Color.Unspecified
  var outerHaloBrush: Brush? = null
  var innerBlobBrush: Brush? = null
  var sweepArcBrush: Brush? = null
  var bgRadialBrush: Brush? = null
  var lastBgSize = -1f
  var lastSurfaceVariant = Color.Unspecified
  var lastSurface = Color.Unspecified
}

@Composable
fun StopwatchAnalogDigitalDial(
  elapsedMillis: Long,
  isRunning: Boolean,
  modifier: Modifier = Modifier,
  size: Dp = 250.dp
) {
  val (mainTime, decimal) = TimeFormatter.formatStopwatchParts(elapsedMillis)

  val infiniteTransition = rememberInfiniteTransition(label = "dial_glow")
  val pulseAlphaState = infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )
  val blobPhaseState = infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(6500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "stopwatch_blob_phase"
  )

  val runningWeightState = animateFloatAsState(
    targetValue = if (isRunning) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "stopwatch_running_weight"
  )

  val dialScaleState = animateFloatAsState(
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

  val cache = remember { StopwatchDialCache() }

  Box(
    modifier = modifier
      .size(size)
      .graphicsLayer {
        val s = dialScaleState.value
        scaleX = s
        scaleY = s
      },
    contentAlignment = Alignment.Center
  ) {
    // Organic Morphing Outer Blob Halo (cached radial shader, zero per-frame allocations)
    Canvas(modifier = Modifier.fillMaxSize()) {
      val centerX = this.size.width / 2f
      val centerY = this.size.height / 2f
      val baseRadius = this.size.minDimension / 2f
      val runningWeight = runningWeightState.value
      val blobPhase = blobPhaseState.value
      val auraBoost = 0.88f + 0.14f * runningWeight

      if (cache.outerHaloBrush == null ||
        cache.lastOuterRadius != baseRadius ||
        cache.lastPrimary != primaryColor ||
        cache.lastSecondary != secondaryColor
      ) {
        cache.lastOuterRadius = baseRadius
        cache.lastPrimary = primaryColor
        cache.lastSecondary = secondaryColor
        cache.outerHaloBrush = Brush.radialGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.30f),
            secondaryColor.copy(alpha = 0.10f),
            Color.Transparent
          ),
          center = Offset.Zero,
          radius = (baseRadius * 1.15f).coerceAtLeast(1f)
        )
        cache.innerBlobBrush = Brush.radialGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.14f),
            Color.Transparent
          ),
          center = Offset.Zero,
          radius = (baseRadius * 0.9f).coerceAtLeast(1f)
        )
        cache.sweepArcBrush = Brush.sweepGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.15f),
            primaryColor
          )
        )
      }

      createOrganicBlobPath(
        center = Offset.Zero,
        baseRadius = baseRadius * 0.96f * auraBoost,
        phase = blobPhase,
        pointCount = 12,
        wobbleAmplitude = 0.055f + 0.03f * runningWeight,
        harmonicA = 3,
        harmonicB = 4,
        path = cache.outerBlobPath,
        xBuffer = cache.xBuffer,
        yBuffer = cache.yBuffer
      )
      translate(left = centerX, top = centerY) {
        drawPath(
          path = cache.outerBlobPath,
          brush = cache.outerHaloBrush!!,
          alpha = (0.55f + 0.45f * runningWeight).coerceIn(0f, 1f)
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)
        .clip(CircleShape)
        .border(1.5.dp, outlineColor.copy(alpha = 0.45f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = minOf(w, h) / 2f - 14.dp.toPx()
        val runningWeight = runningWeightState.value
        val blobPhase = blobPhaseState.value
        val pulseAlpha = pulseAlphaState.value

        if (cache.bgRadialBrush == null ||
          cache.lastBgSize != w ||
          cache.lastSurfaceVariant != surfaceVariant ||
          cache.lastSurface != surfaceColor
        ) {
          cache.lastBgSize = w
          cache.lastSurfaceVariant = surfaceVariant
          cache.lastSurface = surfaceColor
          cache.bgRadialBrush = Brush.radialGradient(
            colors = listOf(
              surfaceVariant.copy(alpha = 0.85f),
              surfaceColor.copy(alpha = 0.96f)
            ),
            center = center,
            radius = (w * 0.5f).coerceAtLeast(1f)
          )
        }

        // Dial background fill
        drawCircle(brush = cache.bgRadialBrush!!)

        // Subtle inner fluid morphing blob when running
        if (runningWeight > 0.02f && cache.innerBlobBrush != null) {
          createOrganicBlobPath(
            center = Offset.Zero,
            baseRadius = radius * 0.72f,
            phase = -blobPhase * 1.3f,
            pointCount = 10,
            wobbleAmplitude = 0.09f,
            harmonicA = 2,
            harmonicB = 3,
            path = cache.innerBlobPath,
            xBuffer = cache.xBuffer,
            yBuffer = cache.yBuffer
          )
          translate(left = center.x, top = center.y) {
            drawPath(
              path = cache.innerBlobPath,
              brush = cache.innerBlobBrush!!,
              alpha = runningWeight.coerceIn(0f, 1f)
            )
          }
        }

        // Outer track circle
        drawCircle(
          color = outlineColor.copy(alpha = 0.28f),
          radius = radius,
          center = center,
          style = Stroke(width = 1.dp.toPx())
        )

        // Precompute 60 tick coordinates only when canvas size changes
        if (cache.lastTickSize != w) {
          cache.lastTickSize = w
          val tickRadiusOuter = radius - 2.dp.toPx()
          val majorLen = 8.dp.toPx()
          val minorLen = 4.dp.toPx()
          for (i in 0 until 60) {
            val angleRad = (i * 6f) * (PI / 180f) - (PI / 2f)
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()
            val tickLength = if (i % 5 == 0) majorLen else minorLen
            cache.tickStartX[i] = center.x + (tickRadiusOuter - tickLength) * cosA
            cache.tickStartY[i] = center.y + (tickRadiusOuter - tickLength) * sinA
            cache.tickEndX[i] = center.x + tickRadiusOuter * cosA
            cache.tickEndY[i] = center.y + tickRadiusOuter * sinA
          }
        }

        val majorStroke = 2.dp.toPx()
        val minorStroke = 1.dp.toPx()
        val minorTickColor = outlineColor.copy(alpha = 0.4f)
        for (i in 0 until 60) {
          val isMajor = i % 5 == 0
          drawLine(
            color = if (isMajor) primaryColor else minorTickColor,
            start = Offset(cache.tickStartX[i], cache.tickStartY[i]),
            end = Offset(cache.tickEndX[i], cache.tickEndY[i]),
            strokeWidth = if (isMajor) majorStroke else minorStroke,
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
        cache.sweepArcBrush?.let { arcBrush ->
          drawArc(
            brush = arcBrush,
            startAngle = -90f,
            sweepAngle = sweepArcAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
          )
        }

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

          if (runningWeight > 0.01f) {
            drawCircle(
              color = tertiaryColor.copy(alpha = 0.3f * runningWeight),
              radius = 7.dp.toPx(),
              center = Offset(center.x, tipY)
            )
          }
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

private class TimerDialCache {
  val outerBlobPath = Path()
  val secondaryBlobPath = Path()
  val liquidCircleClip = Path()
  val wavePath1 = Path()
  val wavePath2 = Path()
  val xBuffer = FloatArray(12)
  val yBuffer = FloatArray(12)

  var lastOuterSize = -1f
  var lastInnerSize = -1f
  var lastColor = Color.Unspecified

  var outerBlobBrush: Brush? = null
  var waveBrush: Brush? = null
  var arcSweepBrush: Brush? = null
  var bgRadialBrush: Brush? = null
  var lastBgSize = -1f
  var lastSurfaceVariant = Color.Unspecified
  var lastSurface = Color.Unspecified
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

  // Keep State references and read .value only inside Canvas/graphicsLayer
  val animatedProgressState = animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 320, easing = LinearEasing),
    label = "smooth_timer_progress"
  )

  val animatedColorState = animateColorAsState(
    targetValue = timerColor,
    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
    label = "smooth_timer_color"
  )

  val runningWeightState = animateFloatAsState(
    targetValue = if (isRunning) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "timer_running_weight"
  )

  val dialScaleState = animateFloatAsState(
    targetValue = if (isRunning) 1.025f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "timer_dial_scale"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "timer_fluid_anim")
  val blobPhaseState = infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 7000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "timer_blob_phase"
  )
  val wavePhaseState = infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "timer_wave_phase"
  )
  val pulseScaleState = infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "timer_pulse_scale"
  )

  val cache = remember { TimerDialCache() }

  Box(
    modifier = modifier
      .size(size)
      .graphicsLayer {
        val s = dialScaleState.value
        scaleX = s
        scaleY = s
      },
    contentAlignment = Alignment.Center
  ) {
    // 1. Organic Morphing Outer Blob Aura (cached radial shader, zero per-frame allocations)
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height
      val centerX = w / 2f
      val centerY = h / 2f
      val baseRadius = this.size.minDimension / 2f

      val runningWeight = runningWeightState.value
      val animatedColor = animatedColorState.value
      val blobPhase = blobPhaseState.value
      val breath = if (isRunning) pulseScaleState.value else 1f

      if (cache.outerBlobBrush == null || cache.lastOuterSize != baseRadius || cache.lastColor != animatedColor) {
        cache.lastOuterSize = baseRadius
        cache.lastColor = animatedColor
        cache.outerBlobBrush = Brush.radialGradient(
          colors = listOf(
            animatedColor.copy(alpha = 0.36f),
            animatedColor.copy(alpha = 0.12f),
            Color.Transparent
          ),
          center = Offset.Zero,
          radius = (baseRadius * 1.18f).coerceAtLeast(1f)
        )
        cache.waveBrush = Brush.verticalGradient(
          colors = listOf(
            animatedColor.copy(alpha = 0.24f),
            animatedColor.copy(alpha = 0.05f)
          )
        )
        cache.arcSweepBrush = Brush.sweepGradient(
          colors = listOf(
            animatedColor.copy(alpha = 0.55f),
            animatedColor
          )
        )
      }

      createOrganicBlobPath(
        center = Offset.Zero,
        baseRadius = baseRadius * 0.95f * breath,
        phase = blobPhase,
        pointCount = 10,
        wobbleAmplitude = 0.055f + 0.035f * runningWeight,
        harmonicA = 2,
        harmonicB = 3,
        path = cache.outerBlobPath,
        xBuffer = cache.xBuffer,
        yBuffer = cache.yBuffer
      )
      translate(left = centerX, top = centerY) {
        drawPath(
          path = cache.outerBlobPath,
          brush = cache.outerBlobBrush!!,
          alpha = (0.55f + 0.45f * runningWeight).coerceIn(0f, 1f)
        )
      }

      createOrganicBlobPath(
        center = Offset(centerX, centerY),
        baseRadius = baseRadius * 0.91f,
        phase = -blobPhase * 1.25f + 1.1f,
        pointCount = 12,
        wobbleAmplitude = 0.045f + 0.03f * runningWeight,
        harmonicA = 3,
        harmonicB = 4,
        path = cache.secondaryBlobPath,
        xBuffer = cache.xBuffer,
        yBuffer = cache.yBuffer
      )
      drawPath(
        path = cache.secondaryBlobPath,
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
        .border(1.dp, outlineColor.copy(alpha = 0.32f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val fullW = this.size.width
        val fullH = this.size.height
        val fullCenter = Offset(fullW / 2f, fullH / 2f)

        if (cache.bgRadialBrush == null ||
          cache.lastBgSize != fullW ||
          cache.lastSurfaceVariant != surfaceVariant ||
          cache.lastSurface != surfaceColor
        ) {
          cache.lastBgSize = fullW
          cache.lastSurfaceVariant = surfaceVariant
          cache.lastSurface = surfaceColor
          cache.bgRadialBrush = Brush.radialGradient(
            colors = listOf(
              surfaceVariant.copy(alpha = 0.55f),
              surfaceColor.copy(alpha = 0.96f)
            ),
            center = fullCenter,
            radius = (fullW * 0.5f).coerceAtLeast(1f)
          )
        }
        drawCircle(brush = cache.bgRadialBrush!!)

        val innerPadding = 10.dp.toPx()
        val w = (fullW - innerPadding * 2f).coerceAtLeast(1f)
        val h = (fullH - innerPadding * 2f).coerceAtLeast(1f)
        val center = Offset(w / 2f, h / 2f)
        val radius = (minOf(w, h) / 2f) - 6.dp.toPx()

        val animatedProgress = animatedProgressState.value
        val animatedColor = animatedColorState.value
        val runningWeight = runningWeightState.value
        val wavePhase = wavePhaseState.value
        val pulseScale = pulseScaleState.value

        // Rebuild inner clip path only when size changes
        if (cache.lastInnerSize != w) {
          cache.lastInnerSize = w
          val innerRadius = (radius - 7.dp.toPx()).coerceAtLeast(1f)
          cache.liquidCircleClip.reset()
          cache.liquidCircleClip.addOval(
            Rect(
              left = center.x - innerRadius,
              top = center.y - innerRadius,
              right = center.x + innerRadius,
              bottom = center.y + innerRadius
            )
          )
        }

        translate(left = innerPadding, top = innerPadding) {
          // Subtle animated liquid level inside the dial
          clipPath(cache.liquidCircleClip) {
            val waveAmp = 4.dp.toPx() + 3.dp.toPx() * runningWeight

            createLiquidWavePath(
              width = w,
              height = h,
              fillFraction = animatedProgress,
              wavePhase = wavePhase,
              waveAmplitude = waveAmp,
              path = cache.wavePath1
            )
            cache.waveBrush?.let { wb ->
              drawPath(
                path = cache.wavePath1,
                brush = wb,
                alpha = (0.68f + 0.32f * runningWeight).coerceIn(0f, 1f)
              )
            }

            createLiquidWavePath(
              width = w,
              height = h,
              fillFraction = (animatedProgress - 0.02f).coerceAtLeast(0f),
              wavePhase = -wavePhase * 1.3f + 1.5f,
              waveAmplitude = waveAmp * 0.8f,
              path = cache.wavePath2
            )
            drawPath(
              path = cache.wavePath2,
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
          cache.arcSweepBrush?.let { arcBrush ->
            drawArc(
              brush = arcBrush,
              startAngle = -90f,
              sweepAngle = sweepAngle,
              useCenter = false,
              topLeft = Offset(center.x - radius, center.y - radius),
              size = Size(radius * 2f, radius * 2f),
              style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
          }

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
                  val p = pulseScaleState.value
                  scaleX = p
                  scaleY = p
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
