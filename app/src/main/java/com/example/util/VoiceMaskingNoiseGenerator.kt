package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class NoiseProfile(
  val displayName: String,
  val shortLabel: String,
  val description: String
) {
  VOICE_MASKING(
    "Study Focus (Speech Masking)",
    "Speech Masking",
    "Concentrated 250 Hz – 3.5 kHz speech formant contour. Calibrated to mask surrounding chatter and human voices so you can study with deep focus."
  ),
  DEEP_VOICE_MASKING(
    "Deep Vocal Masking",
    "Deep Vocal",
    "Enhanced 150 Hz – 2.5 kHz envelope targeting baritone voices, café chatter, and deeper background murmurs."
  ),
  PINK_NOISE(
    "Calm Pink Noise",
    "Pink Noise",
    "Balanced 1/f noise spectrum resembling gentle rain, providing soothing background for reading and steady study focus."
  ),
  BROWN_NOISE(
    "Deep Brown Noise",
    "Brown Noise",
    "Deep 1/f² noise spectrum resembling a distant waterfall or heavy surf for deep immersion."
  )
}

/**
 * Real-time audio synthesizer for speech-masking white noise using native AudioTrack.
 * Features biquad digital filtering calibrated to human voice formant frequency bands.
 */
class VoiceMaskingNoiseGenerator {

  private var audioTrack: AudioTrack? = null
  private var streamJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.Default)
  private val random = Random()

  @Volatile
  var isPlaying: Boolean = false
    private set

  @Volatile
  var currentProfile: NoiseProfile = NoiseProfile.VOICE_MASKING

  @Volatile
  var targetVolume: Float = 0.7f

  @Volatile
  private var currentVolume: Float = 0f

  private val sampleRate = 44100
  private val bufferSamples = 4096

  // Digital filter states for voice masking (biquad cascaded band-pass & formant filters)
  private var hpX1 = 0f
  private var hpX2 = 0f
  private var hpY1 = 0f
  private var hpY2 = 0f

  private var lpX1 = 0f
  private var lpX2 = 0f
  private var lpY1 = 0f
  private var lpY2 = 0f

  private var bpX1 = 0f
  private var bpX2 = 0f
  private var bpY1 = 0f
  private var bpY2 = 0f

  // Pink noise generator state (Paul Kellet's refined 1/f filter)
  private var b0 = 0f
  private var b1 = 0f
  private var b2 = 0f
  private var b3 = 0f
  private var b4 = 0f
  private var b5 = 0f
  private var b6 = 0f

  // Brown noise integration state
  private var lastBrownSample = 0f

  private val trackLock = Any()

  private fun safeStopAndRelease(track: AudioTrack?) {
    if (track == null) return
    synchronized(trackLock) {
      try {
        if (track.state == AudioTrack.STATE_INITIALIZED && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
          track.stop()
        }
      } catch (_: IllegalStateException) {
      } catch (_: Exception) {
      }
      try {
        track.release()
      } catch (_: Exception) {
      }
      if (audioTrack === track) {
        audioTrack = null
      }
    }
  }

  fun start(profile: NoiseProfile = currentProfile, volume: Float = targetVolume) {
    currentProfile = profile
    targetVolume = volume.coerceIn(0f, 1f)
    if (isPlaying) return

    isPlaying = true
    resetFilters()

    streamJob?.cancel()
    lateinit var launchedJob: Job
    launchedJob = scope.launch {
      var track: AudioTrack? = null
      try {
        val minBufferSize = AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSizeInBytes = if (minBufferSize > 0) {
          maxOf(minBufferSize, bufferSamples * 2)
        } else {
          bufferSamples * 2
        }

        val createdTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(bufferSizeInBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        } else {
          @Suppress("DEPRECATION")
          AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes,
            AudioTrack.MODE_STREAM
          )
        }

        if (createdTrack.state != AudioTrack.STATE_INITIALIZED) {
          Log.w("VoiceMaskingNoise", "AudioTrack failed to initialize (state=${createdTrack.state})")
          safeStopAndRelease(createdTrack)
          return@launch
        }

        synchronized(trackLock) {
          if (!isActive || !isPlaying) {
            safeStopAndRelease(createdTrack)
            return@launch
          }
          track = createdTrack
          audioTrack = createdTrack
        }

        createdTrack.play()

        val pcmBuffer = ShortArray(bufferSamples)

        // Smooth fade-in
        currentVolume = 0.05f

        while (isActive && isPlaying) {
          // Ramp volume towards target
          if (currentVolume < targetVolume) {
            currentVolume = (currentVolume + 0.05f).coerceAtMost(targetVolume)
          } else if (currentVolume > targetVolume) {
            currentVolume = (currentVolume - 0.05f).coerceAtLeast(targetVolume)
          }

          generateNoiseChunk(pcmBuffer, currentProfile, currentVolume)
          val written = createdTrack.write(pcmBuffer, 0, pcmBuffer.size)
          if (written < 0) break
        }
      } catch (e: Exception) {
        if (isPlaying) {
          Log.e("VoiceMaskingNoise", "Audio synthesis error: ${e.message}")
        }
      } finally {
        safeStopAndRelease(track)
        if (streamJob === launchedJob) {
          isPlaying = false
        }
      }
    }
    streamJob = launchedJob
  }

  fun stop() {
    if (!isPlaying && audioTrack == null) return
    isPlaying = false
    val job = streamJob
    streamJob = null
    job?.cancel()
    val trackToStop = synchronized(trackLock) {
      val t = audioTrack
      audioTrack = null
      t
    }
    safeStopAndRelease(trackToStop)
  }

  fun updateVolume(newVolume: Float) {
    targetVolume = newVolume.coerceIn(0f, 1f)
  }

  fun updateProfile(newProfile: NoiseProfile) {
    if (currentProfile != newProfile) {
      currentProfile = newProfile
      resetFilters()
    }
  }

  private fun resetFilters() {
    hpX1 = 0f; hpX2 = 0f; hpY1 = 0f; hpY2 = 0f
    lpX1 = 0f; lpX2 = 0f; lpY1 = 0f; lpY2 = 0f
    bpX1 = 0f; bpX2 = 0f; bpY1 = 0f; bpY2 = 0f
    b0 = 0f; b1 = 0f; b2 = 0f; b3 = 0f; b4 = 0f; b5 = 0f; b6 = 0f
    lastBrownSample = 0f
  }

  /**
   * Generates a chunk of filtered noise samples.
   * For VOICE_MASKING, noise is shaped specifically to human speech formants:
   * - 2nd order Butterworth high-pass at 250 Hz
   * - 2nd order Butterworth low-pass at 3500 Hz
   * - Speech formant band-pass boost centered at 1500 Hz (Q=1.2)
   */
  private fun generateNoiseChunk(buffer: ShortArray, profile: NoiseProfile, volume: Float) {
    when (profile) {
      NoiseProfile.VOICE_MASKING -> generateVoiceMasking(buffer, volume, cutoffHp = 250f, cutoffLp = 3500f, centerBp = 1600f)
      NoiseProfile.DEEP_VOICE_MASKING -> generateVoiceMasking(buffer, volume, cutoffHp = 150f, cutoffLp = 2600f, centerBp = 900f)
      NoiseProfile.PINK_NOISE -> generatePinkNoise(buffer, volume)
      NoiseProfile.BROWN_NOISE -> generateBrownNoise(buffer, volume)
    }
  }

  private fun generateVoiceMasking(
    buffer: ShortArray,
    volume: Float,
    cutoffHp: Float,
    cutoffLp: Float,
    centerBp: Float
  ) {
    // Coefficients for High-Pass (Butterworth 2nd order)
    val (hpA0, hpA1, hpA2, hpB1, hpB2) = calculateHighPassCoeffs(cutoffHp, sampleRate.toFloat())
    // Coefficients for Low-Pass (Butterworth 2nd order)
    val (lpA0, lpA1, lpA2, lpB1, lpB2) = calculateLowPassCoeffs(cutoffLp, sampleRate.toFloat())
    // Coefficients for Formant Band-Pass (Center boost)
    val (bpA0, bpA1, bpA2, bpB1, bpB2) = calculateBandPassCoeffs(centerBp, 1.2f, sampleRate.toFloat())

    for (i in buffer.indices) {
      // White noise raw generator: Gaussian approx via sum of uniforms
      val white = ((random.nextFloat() * 2f - 1f) + (random.nextFloat() * 2f - 1f)) * 0.5f

      // Stage 1: High-pass (cut subsonic rumble below human voice)
      val hpOut = hpA0 * white + hpA1 * hpX1 + hpA2 * hpX2 - hpB1 * hpY1 - hpB2 * hpY2
      hpX2 = hpX1; hpX1 = white
      hpY2 = hpY1; hpY1 = hpOut

      // Stage 2: Low-pass (cut harsh hiss above speech intelligibility)
      val lpOut = lpA0 * hpOut + lpA1 * lpX1 + lpA2 * lpX2 - lpB1 * lpY1 - lpB2 * lpY2
      lpX2 = lpX1; lpX1 = hpOut
      lpY2 = lpY1; lpY1 = lpOut

      // Stage 3: Speech formant emphasis (boost voice masking energy at formant frequencies)
      val bpOut = bpA0 * lpOut + bpA1 * bpX1 + bpA2 * bpX2 - bpB1 * bpY1 - bpB2 * bpY2
      bpX2 = bpX1; bpX1 = lpOut
      bpY2 = bpY1; bpY1 = bpOut

      // Blend low-pass shaped noise with formant emphasis for maximum speech drowning power
      val blended = (lpOut * 0.65f + bpOut * 0.95f) * volume * 2.2f
      val clamped = blended.coerceIn(-1f, 1f)
      buffer[i] = (clamped * 32767f).toInt().toShort()
    }
  }

  private fun generatePinkNoise(buffer: ShortArray, volume: Float) {
    for (i in buffer.indices) {
      val white = random.nextFloat() * 2f - 1f
      b0 = 0.99886f * b0 + white * 0.0555179f
      b1 = 0.99332f * b1 + white * 0.0750759f
      b2 = 0.96900f * b2 + white * 0.1538520f
      b3 = 0.86650f * b3 + white * 0.3104856f
      b4 = 0.55000f * b4 + white * 0.5329522f
      b5 = -0.7616f * b5 - white * 0.0168980f
      val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f) * 0.11f
      b6 = white * 0.115926f
      val clamped = (pink * volume * 1.5f).coerceIn(-1f, 1f)
      buffer[i] = (clamped * 32767f).toInt().toShort()
    }
  }

  private fun generateBrownNoise(buffer: ShortArray, volume: Float) {
    for (i in buffer.indices) {
      val white = random.nextFloat() * 2f - 1f
      lastBrownSample = (lastBrownSample + (0.025f * white)) / 1.025f
      val clamped = (lastBrownSample * 3.5f * volume).coerceIn(-1f, 1f)
      buffer[i] = (clamped * 32767f).toInt().toShort()
    }
  }

  private data class FilterCoeffs(val a0: Float, val a1: Float, val a2: Float, val b1: Float, val b2: Float)

  private fun calculateHighPassCoeffs(cutoffHz: Float, fs: Float): FilterCoeffs {
    val omega = 2f * PI.toFloat() * cutoffHz / fs
    val alpha = sin(omega) / (2f * 0.7071f)
    val cosW = cos(omega)

    val a0 = (1f + cosW) / 2f
    val a1 = -(1f + cosW)
    val a2 = (1f + cosW) / 2f
    val b0 = 1f + alpha
    val b1 = -2f * cosW
    val b2 = 1f - alpha

    return FilterCoeffs(a0 / b0, a1 / b0, a2 / b0, b1 / b0, b2 / b0)
  }

  private fun calculateLowPassCoeffs(cutoffHz: Float, fs: Float): FilterCoeffs {
    val omega = 2f * PI.toFloat() * cutoffHz / fs
    val alpha = sin(omega) / (2f * 0.7071f)
    val cosW = cos(omega)

    val a0 = (1f - cosW) / 2f
    val a1 = 1f - cosW
    val a2 = (1f - cosW) / 2f
    val b0 = 1f + alpha
    val b1 = -2f * cosW
    val b2 = 1f - alpha

    return FilterCoeffs(a0 / b0, a1 / b0, a2 / b0, b1 / b0, b2 / b0)
  }

  private fun calculateBandPassCoeffs(centerHz: Float, q: Float, fs: Float): FilterCoeffs {
    val omega = 2f * PI.toFloat() * centerHz / fs
    val alpha = sin(omega) / (2f * q)
    val cosW = cos(omega)

    val a0 = alpha
    val a1 = 0f
    val a2 = -alpha
    val b0 = 1f + alpha
    val b1 = -2f * cosW
    val b2 = 1f - alpha

    return FilterCoeffs(a0 / b0, a1 / b0, a2 / b0, b1 / b0, b2 / b0)
  }
}
