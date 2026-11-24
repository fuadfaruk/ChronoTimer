package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Creates a smooth C1-continuous closed cubic Bezier organic blob path around [center].
 * Accepts optional pre-allocated [xBuffer] and [yBuffer] arrays for zero-allocation per-frame drawing.
 */
fun createOrganicBlobPath(
  center: Offset,
  baseRadius: Float,
  phase: Float,
  pointCount: Int = 10,
  wobbleAmplitude: Float = 0.12f,
  harmonicA: Int = 2,
  harmonicB: Int = 3,
  path: Path = Path(),
  xBuffer: FloatArray? = null,
  yBuffer: FloatArray? = null
): Path {
  path.reset()
  if (baseRadius <= 0f || pointCount < 4) return path

  val xs = if (xBuffer != null && xBuffer.size >= pointCount) xBuffer else FloatArray(pointCount)
  val ys = if (yBuffer != null && yBuffer.size >= pointCount) yBuffer else FloatArray(pointCount)

  val twoPi = (2.0 * PI).toFloat()
  val invCount = 1f / pointCount.toFloat()
  val phaseB = phase * 1.35f
  val cx = center.x
  val cy = center.y

  for (i in 0 until pointCount) {
    val angle = (i * invCount) * twoPi
    val wave1 = sin(angle * harmonicA + phase)
    val wave2 = cos(angle * harmonicB - phaseB) * 0.6f
    val r = baseRadius * (1f + wobbleAmplitude * (wave1 + wave2) * 0.65f)
    xs[i] = cx + r * cos(angle)
    ys[i] = cy + r * sin(angle)
  }

  // Catmull-Rom spline converted to cubic Bezier segments
  val tension = 1f / 6f
  path.moveTo(xs[0], ys[0])
  for (i in 0 until pointCount) {
    val i0 = (i - 1 + pointCount) % pointCount
    val i1 = i
    val i2 = (i + 1) % pointCount
    val i3 = (i + 2) % pointCount

    val p0x = xs[i0]
    val p0y = ys[i0]
    val p1x = xs[i1]
    val p1y = ys[i1]
    val p2x = xs[i2]
    val p2y = ys[i2]
    val p3x = xs[i3]
    val p3y = ys[i3]

    path.cubicTo(
      p1x + (p2x - p0x) * tension,
      p1y + (p2y - p0y) * tension,
      p2x - (p3x - p1x) * tension,
      p2y - (p3y - p1y) * tension,
      p2x,
      p2y
    )
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

  val freq1 = (2.0 * PI * 1.4).toFloat()
  val freq2 = (2.0 * PI * 2.3).toFloat()
  val secPhase = wavePhase * 1.4f

  for (i in 0..steps) {
    val x = i * dx
    val normX = x / width
    val primaryWave = sin(normX * freq1 + wavePhase)
    val secondaryWave = cos(normX * freq2 - secPhase) * 0.45f
    val y = (baseY + (primaryWave + secondaryWave) * waveAmplitude).coerceIn(0f, height)
    path.lineTo(x, y)
  }

  path.lineTo(width, height)
  path.close()
  return path
}

private class FluidBlobCache {
  val blob1Path = Path()
  val blob2Path = Path()
  val blob3Path = Path()
  val xBuffer = FloatArray(12)
  val yBuffer = FloatArray(12)

  var lastPrimary: Color = Color.Unspecified
  var lastSecondary: Color = Color.Unspecified
  var lastRadius1: Float = -1f
  var lastRadius2: Float = -1f
  var lastRadius3: Float = -1f

  var brush1: Brush? = null
  var brush2: Brush? = null
  var brush3: Brush? = null
}

/**
 * Ambient organic morphing blob backdrop that smoothly responds to tab/phase colors and running state.
 * Animation state reads and shader caches are strictly isolated inside Canvas onDraw to avoid recomposition
 * and per-frame object/shader allocations.
 */
@Composable
fun FluidBlobBackground(
  primaryAccent: Color,
  secondaryAccent: Color,
  isActive: Boolean,
  isDarkTheme: Boolean,
  modifier: Modifier = Modifier
) {
  val animatedPrimaryState = animateColorAsState(
    targetValue = primaryAccent,
    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
    label = "bg_primary_accent"
  )
  val animatedSecondaryState = animateColorAsState(
    targetValue = secondaryAccent,
    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
    label = "bg_secondary_accent"
  )

  val activeIntensityState = animateFloatAsState(
    targetValue = if (isActive) 1f else 0.65f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "bg_active_intensity"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "fluid_bg_blobs")
  val driftPhaseState = infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 16000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "drift_phase"
  )
  val morphPhaseState = infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 11000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "morph_phase"
  )

  val cache = remember { FluidBlobCache() }
  val baseAlpha = if (isDarkTheme) 0.16f else 0.12f

  Box(modifier = modifier.graphicsLayer()) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w <= 0f || h <= 0f) return@Canvas

      val minDim = w.coerceAtMost(h)
      val driftPhase = driftPhaseState.value
      val morphPhase = morphPhaseState.value
      val activeIntensity = activeIntensityState.value
      val animatedPrimary = animatedPrimaryState.value
      val animatedSecondary = animatedSecondaryState.value

      val rBase1 = minDim * 0.44f
      val rBase2 = minDim * 0.48f
      val rBase3 = minDim * 0.36f

      if (cache.brush1 == null || cache.lastPrimary != animatedPrimary || cache.lastRadius1 != rBase1) {
        cache.lastPrimary = animatedPrimary
        cache.lastRadius1 = rBase1
        cache.brush1 = Brush.radialGradient(
          colors = listOf(
            animatedPrimary,
            animatedPrimary.copy(alpha = 0.35f),
            Color.Transparent
          ),
          center = Offset.Zero,
          radius = (rBase1 * 1.35f).coerceAtLeast(1f)
        )
        cache.brush3 = null
      }

      if (cache.brush2 == null || cache.lastSecondary != animatedSecondary || cache.lastRadius2 != rBase2) {
        cache.lastSecondary = animatedSecondary
        cache.lastRadius2 = rBase2
        cache.brush2 = Brush.radialGradient(
          colors = listOf(
            animatedSecondary,
            animatedSecondary.copy(alpha = 0.30f),
            Color.Transparent
          ),
          center = Offset.Zero,
          radius = (rBase2 * 1.35f).coerceAtLeast(1f)
        )
        cache.brush3 = null
      }

      if (cache.brush3 == null || cache.lastRadius3 != rBase3) {
        cache.lastRadius3 = rBase3
        cache.brush3 = Brush.radialGradient(
          colors = listOf(
            animatedPrimary.copy(alpha = 0.55f),
            animatedSecondary.copy(alpha = 0.25f),
            Color.Transparent
          ),
          center = Offset.Zero,
          radius = (rBase3 * 1.3f).coerceAtLeast(1f)
        )
      }

      val globalAlpha = (baseAlpha * activeIntensity).coerceIn(0f, 1f)

      // Top-left floating primary blob
      val c1x = w * (0.22f + 0.10f * cos(driftPhase))
      val c1y = h * (0.20f + 0.08f * sin(driftPhase * 1.2f))
      val s1 = (0.94f + 0.08f * sin(morphPhase)) * (0.9f + 0.12f * activeIntensity)
      createOrganicBlobPath(
        center = Offset.Zero,
        baseRadius = rBase1 * s1,
        phase = morphPhase,
        pointCount = 10,
        wobbleAmplitude = 0.16f,
        harmonicA = 2,
        harmonicB = 3,
        path = cache.blob1Path,
        xBuffer = cache.xBuffer,
        yBuffer = cache.yBuffer
      )
      translate(left = c1x, top = c1y) {
        drawPath(
          path = cache.blob1Path,
          brush = cache.brush1!!,
          alpha = globalAlpha
        )
      }

      // Bottom-right secondary blob
      val c2x = w * (0.78f + 0.10f * sin(driftPhase + 1.8f))
      val c2y = h * (0.76f + 0.09f * cos(driftPhase * 0.9f + 0.7f))
      val s2 = (0.94f + 0.07f * cos(morphPhase + 1.2f)) * (0.9f + 0.12f * activeIntensity)
      createOrganicBlobPath(
        center = Offset.Zero,
        baseRadius = rBase2 * s2,
        phase = -morphPhase * 0.9f + 1.4f,
        pointCount = 10,
        wobbleAmplitude = 0.15f,
        harmonicA = 3,
        harmonicB = 2,
        path = cache.blob2Path,
        xBuffer = cache.xBuffer,
        yBuffer = cache.yBuffer
      )
      translate(left = c2x, top = c2y) {
        drawPath(
          path = cache.blob2Path,
          brush = cache.brush2!!,
          alpha = (globalAlpha * 0.9f).coerceIn(0f, 1f)
        )
      }

      // Center-orbiting ambient core glow blob
      val c3x = w * (0.50f + 0.12f * sin(driftPhase * 1.5f + 2.5f))
      val c3y = h * (0.46f + 0.08f * cos(driftPhase * 1.1f + 3.1f))
      val s3 = (0.92f + 0.08f * sin(morphPhase * 1.4f)) * activeIntensity
      createOrganicBlobPath(
        center = Offset.Zero,
        baseRadius = rBase3 * s3,
        phase = morphPhase * 1.25f + 2.2f,
        pointCount = 12,
        wobbleAmplitude = 0.18f,
        harmonicA = 2,
        harmonicB = 4,
        path = cache.blob3Path,
        xBuffer = cache.xBuffer,
        yBuffer = cache.yBuffer
      )
      translate(left = c3x, top = c3y) {
        drawPath(
          path = cache.blob3Path,
          brush = cache.brush3!!,
          alpha = globalAlpha
        )
      }
    }
  }
}

/**
 * Adds elastic spring-physics compression on press using [interactionSource] via a stateless Modifier.Node
 * (zero extra composition scopes).
 */
fun Modifier.fluidPressScale(
  interactionSource: InteractionSource,
  pressedScale: Float = 0.94f
): Modifier = this then FluidPressScaleElement(interactionSource, pressedScale)

private data class FluidPressScaleElement(
  val interactionSource: InteractionSource,
  val pressedScale: Float
) : ModifierNodeElement<FluidPressScaleNode>() {
  override fun create(): FluidPressScaleNode = FluidPressScaleNode(interactionSource, pressedScale)

  override fun update(node: FluidPressScaleNode) {
    node.updateParams(interactionSource, pressedScale)
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "fluidPressScale"
    properties["pressedScale"] = pressedScale
  }
}

private class FluidPressScaleNode(
  private var interactionSource: InteractionSource,
  private var pressedScale: Float
) : Modifier.Node(), LayoutModifierNode {

  private val scaleAnim = Animatable(1f)
  private var collectJob: Job? = null
  private var animJob: Job? = null

  override fun onAttach() {
    startCollecting()
  }

  override fun onDetach() {
    collectJob?.cancel()
    collectJob = null
    animJob?.cancel()
    animJob = null
  }

  fun updateParams(newSource: InteractionSource, newPressedScale: Float) {
    val sourceChanged = interactionSource != newSource
    interactionSource = newSource
    pressedScale = newPressedScale
    if (isAttached && sourceChanged) {
      startCollecting()
    }
  }

  private fun startCollecting() {
    collectJob?.cancel()
    collectJob = coroutineScope.launch {
      var pressCount = 0
      interactionSource.interactions.collect { interaction ->
        val wasPressed = pressCount > 0
        when (interaction) {
          is PressInteraction.Press -> pressCount++
          is PressInteraction.Release, is PressInteraction.Cancel -> {
            pressCount = (pressCount - 1).coerceAtLeast(0)
          }
        }
        val isNowPressed = pressCount > 0
        if (wasPressed != isNowPressed) {
          animJob?.cancel()
          animJob = launch {
            scaleAnim.animateTo(
              targetValue = if (isNowPressed) pressedScale else 1f,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
              )
            )
          }
        }
      }
    }
  }

  override fun MeasureScope.measure(
    measurable: Measurable,
    constraints: Constraints
  ): MeasureResult {
    val placeable = measurable.measure(constraints)
    return layout(placeable.width, placeable.height) {
      placeable.placeWithLayer(0, 0) {
        val s = scaleAnim.value
        scaleX = s
        scaleY = s
      }
    }
  }
}

/**
 * Draws a soft breathing organic aura behind a primary control when [isActive] is true,
 * implemented with DrawModifierNode and cached radial gradient shaders.
 */
fun Modifier.fluidPulseHalo(
  color: Color,
  isActive: Boolean
): Modifier = this then FluidPulseHaloElement(color, isActive)

private data class FluidPulseHaloElement(
  val color: Color,
  val isActive: Boolean
) : ModifierNodeElement<FluidPulseHaloNode>() {
  override fun create(): FluidPulseHaloNode = FluidPulseHaloNode(color, isActive)

  override fun update(node: FluidPulseHaloNode) {
    node.updateParams(color, isActive)
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "fluidPulseHalo"
    properties["color"] = color
    properties["isActive"] = isActive
  }
}

private class FluidPulseHaloNode(
  private var color: Color,
  private var isActive: Boolean
) : Modifier.Node(), DrawModifierNode {

  private val alphaAnim = Animatable(if (isActive) 1f else 0f)
  private val pulseAnim = Animatable(0.92f)
  private var alphaJob: Job? = null
  private var pulseJob: Job? = null

  private var cachedColor: Color = Color.Unspecified
  private var cachedRadius: Float = -1f
  private var cachedBrush: Brush? = null

  override fun onAttach() {
    syncAnimations(immediate = true)
  }

  override fun onDetach() {
    alphaJob?.cancel()
    alphaJob = null
    pulseJob?.cancel()
    pulseJob = null
  }

  fun updateParams(newColor: Color, newActive: Boolean) {
    val activeChanged = isActive != newActive
    color = newColor
    isActive = newActive
    if (isAttached && activeChanged) {
      syncAnimations(immediate = false)
    }
  }

  private fun syncAnimations(immediate: Boolean) {
    alphaJob?.cancel()
    alphaJob = coroutineScope.launch {
      if (immediate) {
        alphaAnim.snapTo(if (isActive) 1f else 0f)
      } else {
        alphaAnim.animateTo(
          targetValue = if (isActive) 1f else 0f,
          animationSpec = tween(450, easing = FastOutSlowInEasing)
        )
      }
      if (!isActive && alphaAnim.value <= 0.01f) {
        pulseJob?.cancel()
        pulseJob = null
      }
    }

    if (isActive && pulseJob?.isActive != true) {
      pulseJob = coroutineScope.launch {
        pulseAnim.snapTo(0.92f)
        pulseAnim.animateTo(
          targetValue = 1.14f,
          animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
          )
        )
      }
    }
  }

  override fun ContentDrawScope.draw() {
    val alpha = alphaAnim.value
    if (alpha > 0.01f) {
      val baseRadius = (size.maxDimension * 0.68f).coerceAtLeast(1f)
      if (cachedBrush == null || cachedColor != color || cachedRadius != baseRadius) {
        cachedColor = color
        cachedRadius = baseRadius
        cachedBrush = Brush.radialGradient(
          colors = listOf(
            color.copy(alpha = 0.34f),
            color.copy(alpha = 0.12f),
            Color.Transparent
          ),
          center = Offset.Zero,
          radius = baseRadius
        )
      }
      val scaleFactor = pulseAnim.value
      withTransform({
        translate(left = center.x, top = center.y)
        scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
      }) {
        drawCircle(
          brush = cachedBrush!!,
          radius = baseRadius,
          center = Offset.Zero,
          alpha = alpha
        )
      }
    }
    drawContent()
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
