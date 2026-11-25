package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Dispatches ToneGenerator and Vibrator triggers inside a dedicated [CoroutineScope] backed by
 * [Dispatchers.Default] using conflated channels so high-frequency events never block the UI thread
 * or accumulate a backlog of stale audio/haptic IPC calls.
 */
class SoundHapticHelper(
  private val context: Context,
  dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

  private sealed interface SoundCommand {
    data object Click : SoundCommand
    data object Lap : SoundCommand
    data class CountdownBeep(val isFinalGo: Boolean) : SoundCommand
  }

  private sealed interface VibrationCommand {
    data object Click : VibrationCommand
    data object Lap : VibrationCommand
    data object Cancel : VibrationCommand
  }

  private val scope = CoroutineScope(SupervisorJob() + dispatcher)
  private val soundChannel = Channel<SoundCommand>(Channel.CONFLATED)
  private val vibrationChannel = Channel<VibrationCommand>(Channel.CONFLATED)

  private val isReleased = AtomicBoolean(false)
  private val toneLock = Any()
  private val vibratorLock = Any()
  private val alertLock = Any()

  @Volatile
  private var toneGenerator: ToneGenerator? = null

  @Volatile
  private var cachedVibrator: Vibrator? = null

  @Volatile
  private var vibratorResolved = false

  private var alertJob: Job? = null

  init {
    scope.launch {
      ensureToneGenerator()
    }
    scope.launch {
      for (command in soundChannel) {
        if (isReleased.get()) break
        when (command) {
          SoundCommand.Click -> playToneInternal(ToneGenerator.TONE_PROP_BEEP, 35)
          SoundCommand.Lap -> playToneInternal(ToneGenerator.TONE_PROP_ACK, 50)
          is SoundCommand.CountdownBeep -> {
            val tone = if (command.isFinalGo) {
              ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            } else {
              ToneGenerator.TONE_PROP_BEEP
            }
            val duration = if (command.isFinalGo) 250 else 70
            playToneInternal(tone, duration)
          }
        }
      }
    }
    scope.launch {
      for (command in vibrationChannel) {
        if (isReleased.get()) break
        when (command) {
          VibrationCommand.Click -> vibrateOneShotInternal(25L)
          VibrationCommand.Lap -> vibrateWaveformInternal(longArrayOf(0, 30, 40, 30))
          VibrationCommand.Cancel -> cancelVibrationInternal()
        }
      }
    }
  }

  private fun ensureToneGenerator(): ToneGenerator? {
    if (isReleased.get()) return null
    toneGenerator?.let { return it }
    return synchronized(toneLock) {
      if (isReleased.get()) return@synchronized null
      toneGenerator ?: try {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85).also {
          toneGenerator = it
        }
      } catch (_: Exception) {
        null
      }
    }
  }

  private fun getVibrator(): Vibrator? {
    if (vibratorResolved) return cachedVibrator
    return synchronized(vibratorLock) {
      if (!vibratorResolved) {
        cachedVibrator = try {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
              context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
          } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
          }
        } catch (_: Exception) {
          null
        }
        vibratorResolved = true
      }
      cachedVibrator
    }
  }

  private fun playToneInternal(toneType: Int, durationMs: Int) {
    if (isReleased.get()) return
    synchronized(toneLock) {
      if (isReleased.get()) return
      try {
        val generator = toneGenerator ?: ensureToneGenerator()
        generator?.startTone(toneType, durationMs)
      } catch (_: Exception) {
      }
    }
  }

  private fun vibrateOneShotInternal(durationMs: Long) {
    if (isReleased.get()) return
    try {
      val vib = getVibrator() ?: return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vib.vibrate(durationMs)
      }
    } catch (_: Exception) {
    }
  }

  private fun vibrateWaveformInternal(timings: LongArray) {
    if (isReleased.get()) return
    try {
      val vib = getVibrator() ?: return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vib.vibrate(VibrationEffect.createWaveform(timings, -1))
      } else {
        @Suppress("DEPRECATION")
        vib.vibrate(timings, -1)
      }
    } catch (_: Exception) {
    }
  }

  private fun cancelVibrationInternal() {
    try {
      getVibrator()?.cancel()
    } catch (_: Exception) {
    }
  }

  fun playClickSound(soundEnabled: Boolean) {
    if (!soundEnabled || isReleased.get()) return
    soundChannel.trySend(SoundCommand.Click)
  }

  fun playLapSound(soundEnabled: Boolean) {
    if (!soundEnabled || isReleased.get()) return
    soundChannel.trySend(SoundCommand.Lap)
  }

  fun playCountdownBeep(soundEnabled: Boolean, isFinalGo: Boolean = false) {
    if (!soundEnabled || isReleased.get()) return
    soundChannel.trySend(SoundCommand.CountdownBeep(isFinalGo))
  }

  fun vibrateClick(vibrationEnabled: Boolean) {
    if (!vibrationEnabled || isReleased.get()) return
    vibrationChannel.trySend(VibrationCommand.Click)
  }

  fun vibrateLap(vibrationEnabled: Boolean) {
    if (!vibrationEnabled || isReleased.get()) return
    vibrationChannel.trySend(VibrationCommand.Lap)
  }

  fun startAlarmAlert(soundEnabled: Boolean, vibrationEnabled: Boolean) {
    if (isReleased.get()) return
    synchronized(alertLock) {
      alertJob?.cancel()
      if (!soundEnabled && !vibrationEnabled) {
        alertJob = null
        return
      }
      alertJob = scope.launch {
        repeat(10) {
          if (!isActive || isReleased.get()) return@launch
          if (soundEnabled) {
            playToneInternal(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 400)
          }
          if (vibrationEnabled) {
            vibrateWaveformInternal(longArrayOf(0, 200, 100, 200))
          }
          delay(700)
        }
      }
    }
  }

  fun stopAlarmAlert() {
    synchronized(alertLock) {
      alertJob?.cancel()
      alertJob = null
    }
    if (!isReleased.get()) {
      vibrationChannel.trySend(VibrationCommand.Cancel)
    }
  }

  fun release() {
    if (!isReleased.compareAndSet(false, true)) return
    synchronized(alertLock) {
      alertJob?.cancel()
      alertJob = null
    }
    soundChannel.close()
    vibrationChannel.close()
    scope.launch {
      cancelVibrationInternal()
      synchronized(toneLock) {
        try {
          toneGenerator?.release()
        } catch (_: Exception) {
        }
        toneGenerator = null
      }
      scope.cancel()
    }
  }
}

