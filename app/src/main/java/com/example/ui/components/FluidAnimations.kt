package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Creates a smooth C1-continuous closed cubic Bezier organic blob path around [center].
 */
fun createOrganicBlobPath(
  center: Offset,
  baseRadius: Float,
  phase: Float,
  pointCount: Int = 10,
  wobbleAmplitude: Float = 0.12f,
  harmonicA: Int = 2,
  harmonicB: Int = 3,
  path: Path = Path()
): Path {
  path.reset()
  if (baseRadius <= 0f || pointCount < 4) return path

  val twoPi = (2.0 * PI).toFloat()
  val pts = Array(pointCount) { i ->
    val angle = (i.toFloat() / pointCount.toFloat()) * twoPi
    val wave1 = sin(angle * harmonicA + phase)
    val wave2 = cos(angle * harmonicB - phase * 1.35f) * 0.6f
    val r = baseRadius * (1f + wobbleAmplitude * (wave1 + wave2) * 0.65f)
    Offset(
      x = center.x + r * cos(angle),
      y = center.y + r * sin(angle)
    )
  }

  // Catmull-Rom spline converted to cubic Bezier segments
  val tension = 1f / 6f
  path.moveTo(pts[0].x, pts[0].y)
  for (i in 0 until pointCount) {
    val p0 = pts[(i - 1 + pointCount) % pointCount]
    val p1 = pts[i]
    val p2 = pts[(i + 1) % pointCount]
    val p3 = pts[(i + 2) % pointCount]

    val cp1 = Offset(
      p1.x + (p2.x - p0.x) * tension,
      p1.y + (p2.y - p0.y) * tension
    )
    val cp2 = Offset(
      p2.x - (p3.x - p1.x) * tension,
      p2.y - (p3.y - p1.y) * tension
    )
    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
  }
  path.close()
  return path
}

/**
 * Creates a horizontal liquid wave path clipped inside a container of [width] x [height].
 * [fillFraction] = 0f (empty) .. 1f (full).
 */
fun createLiquidWavePath(
  width: Float,
  height: Float,
  fillFraction: Float,
  wavePhase: Float,
  waveAmplitude: Float,
  path: Path = Path()
): Path {
  path.reset()
  val clamped = fillFraction.coerceIn(0f, 1f)
  if (clamped <= 0.001f || width <= 0f || height <= 0f) return path

  val baseY = height * (1f - clamped)
  val steps = 36
  val dx = width / steps

  path.moveTo(0f, height)
  path.lineTo(0f, baseY)

  for (i in 0..steps) {
    val x = i * dx
    val normX = x / width
    val primaryWave = sin(normX * (2.0 * PI * 1.4).toFloat() + wavePhase)
    val secondaryWave = cos(normX * (2.0 * PI * 2.3).toFloat() - wavePhase * 1.4f) * 0.45f
    val y = (baseY + (primaryWave + secondaryWave) * waveAmplitude).coerceIn(0f, height)
    path.lineTo(x, y)
  }

  path.lineTo(width, height)
  path.close()
  return path
}

/**
 * Ambient organic morphing blob backdrop that smoothly responds to tab/phase colors and running state.
 */
@Composable
fun FluidBlobBackground(
  primaryAccent: Color,
  secondaryAccent: Color,
  isActive: Boolean,
  isDarkTheme: Boolean,
  modifier: Modifier = Modifier
) {
  val animatedPrimary by animateColorAsState(
    targetValue = primaryAccent,
    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
    label = "bg_primary_accent"
  )
  val animatedSecondary by animateColorAsState(
    targetValue = secondaryAccent,
    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
    label = "bg_secondary_accent"
  )

  val activeIntensity by animateFloatAsState(
    targetValue = if (isActive) 1f else 0.65f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "bg_active_intensity"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "fluid_bg_blobs")
  val driftPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 16000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "drift_phase"
  )
  val morphPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 11000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "morph_phase"
  )

  val blob1Path = remember { Path() }
  val blob2Path = remember { Path() }
  val blob3Path = remember { Path() }

  val baseAlpha = if (isDarkTheme) 0.16f else 0.12f

  Box(modifier = modifier) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w <= 0f || h <= 0f) return@Canvas

      // Top-left floating primary blob
      val c1 = Offset(
        x = w * (0.22f + 0.10f * cos(driftPhase)),
        y = h * (0.20f + 0.08f * sin(driftPhase * 1.2f))
      )
      val r1 = (w.coerceAtMost(h) * 0.44f) * (0.94f + 0.08f * sin(morphPhase)) * (0.9f + 0.12f * activeIntensity)
      createOrganicBlobPath(
        center = c1,
        baseRadius = r1,
        phase = morphPhase,
        pointCount = 10,
        wobbleAmplitude = 0.16f,
        harmonicA = 2,
        harmonicB = 3,
        path = blob1Path
      )
      drawPath(
        path = blob1Path,
        brush = Brush.radialGradient(
          colors = listOf(
            animatedPrimary.copy(alpha = baseAlpha * activeIntensity),
            animatedPrimary.copy(alpha = baseAlpha * 0.35f * activeIntensity),
            Color.Transparent
          ),
          center = c1,
          radius = r1 * 1.35f
        )
      )

      // Bottom-right secondary blob
      val c2 = Offset(
        x = w * (0.78f + 0.10f * sin(driftPhase + 1.8f)),
        y = h * (0.76f + 0.09f * cos(driftPhase * 0.9f + 0.7f))
      )
      val r2 = (w.coerceAtMost(h) * 0.48f) * (0.94f + 0.07f * cos(morphPhase + 1.2f)) * (0.9f + 0.12f * activeIntensity)
      createOrganicBlobPath(
        center = c2,
        baseRadius = r2,
        phase = -morphPhase * 0.9f + 1.4f,
        pointCount = 10,
        wobbleAmplitude = 0.15f,
        harmonicA = 3,
        harmonicB = 2,
        path = blob2Path
      )
      drawPath(
        path = blob2Path,
        brush = Brush.radialGradient(
          colors = listOf(
            animatedSecondary.copy(alpha = baseAlpha * 0.9f * activeIntensity),
            animatedSecondary.copy(alpha = baseAlpha * 0.30f * activeIntensity),
            Color.Transparent
          ),
          center = c2,
          radius = r2 * 1.35f
        )
      )

      // Center-orbiting ambient core glow blob (stronger when active)
      val c3 = Offset(
        x = w * (0.50f + 0.12f * sin(driftPhase * 1.5f + 2.5f)),
        y = h * (0.46f + 0.08f * cos(driftPhase * 1.1f + 3.1f))
      )
      val r3 = (w.coerceAtMost(h) * 0.36f) * (0.92f + 0.08f * sin(morphPhase * 1.4f)) * activeIntensity
      createOrganicBlobPath(
        center = c3,
        baseRadius = r3,
        phase = morphPhase * 1.25f + 2.2f,
        pointCount = 12,
        wobbleAmplitude = 0.18f,
        harmonicA = 2,
        harmonicB = 4,
        path = blob3Path
      )
      drawPath(
        path = blob3Path,
        brush = Brush.radialGradient(
          colors = listOf(
            animatedPrimary.copy(alpha = baseAlpha * 0.55f * activeIntensity),
            animatedSecondary.copy(alpha = baseAlpha * 0.25f * activeIntensity),
            Color.Transparent
          ),
          center = c3,
          radius = r3 * 1.3f
        )
      )
    }
  }
}

/**
 * Adds elastic spring-physics compression on press using [interactionSource].
 */
fun Modifier.fluidPressScale(
  interactionSource: InteractionSource,
  pressedScale: Float = 0.94f
): Modifier = composed {
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) pressedScale else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "fluid_press_scale"
  )
  this.graphicsLayer {
    scaleX = scale
    scaleY = scale
  }
}

/**
 * Draws a soft breathing organic aura behind a primary control when [isActive] is true.
 */
fun Modifier.fluidPulseHalo(
  color: Color,
  isActive: Boolean
): Modifier = composed {
  val haloAlpha by animateFloatAsState(
    targetValue = if (isActive) 1f else 0f,
    animationSpec = tween(450, easing = FastOutSlowInEasing),
    label = "halo_visibility"
  )
  val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
  val pulse by infiniteTransition.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.14f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "halo_scale"
  )

  this.drawBehind {
    if (haloAlpha > 0.01f) {
      val radius = (size.maxDimension * 0.68f) * pulse
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            color.copy(alpha = 0.34f * haloAlpha),
            color.copy(alpha = 0.12f * haloAlpha),
            Color.Transparent
          ),
          center = center,
          radius = radius
        ),
        radius = radius,
        center = center
      )
    }
  }
}

/**
 * Smooth rolling/morphing content transition for numbers & short status text.
 */
@Composable
fun <T> FluidAnimatedValue(
  targetState: T,
  modifier: Modifier = Modifier,
  content: @Composable (T) -> Unit
) {
  AnimatedContent(
    targetState = targetState,
    modifier = modifier,
    transitionSpec = {
      ContentTransform(
        targetContentEnter = (slideInVertically(
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
          )
        ) { height -> height / 3 } + fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220))),
        initialContentExit = (slideOutVertically(
          animationSpec = tween(180)
        ) { height -> -height / 3 } + fadeOut(tween(180)) + scaleOut(targetScale = 0.92f, animationSpec = tween(180)))
      )
    },
    label = "fluid_animated_value"
  ) { state ->
    content(state)
  }
}
