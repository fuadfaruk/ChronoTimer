package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dispatches ToneGenerator and Vibrator calls asynchronously on a single serialized
 * background dispatcher so button presses and tab switches never block the UI thread.
 */
class SoundHapticHelper(private val context: Context) {

  @OptIn(ExperimentalCoroutinesApi::class)
  private val feedbackDispatcher = Dispatchers.Default.limitedParallelism(1)
  private val scope = CoroutineScope(SupervisorJob() + feedbackDispatcher)

  @Volatile
  private var toneGenerator: ToneGenerator? = null
  private var alertJob: Job? = null

  private val vibrator: Vibrator? by lazy {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
    } catch (_: Exception) {
      null
    }
  }

  init {
    scope.launch {
      try {
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
      } catch (_: Exception) {
        // Ignored if device audio stream is unavailable
      }
    }
  }

  fun playClickSound(soundEnabled: Boolean) {
    if (!soundEnabled) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
      } catch (_: Exception) {}
    }
  }

  fun playLapSound(soundEnabled: Boolean) {
    if (!soundEnabled) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 50)
      } catch (_: Exception) {}
    }
  }

  fun playCountdownBeep(soundEnabled: Boolean, isFinalGo: Boolean = false) {
    if (!soundEnabled) return
    scope.launch {
      try {
        val tone = if (isFinalGo) ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD else ToneGenerator.TONE_PROP_BEEP
        val duration = if (isFinalGo) 250 else 70
        toneGenerator?.startTone(tone, duration)
      } catch (_: Exception) {}
    }
  }

  fun vibrateClick(vibrationEnabled: Boolean) {
    if (!vibrationEnabled) return
    scope.launch {
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(25)
        }
      } catch (_: Exception) {}
    }
  }

  fun vibrateLap(vibrationEnabled: Boolean) {
    if (!vibrationEnabled) return
    scope.launch {
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 30), -1))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(longArrayOf(0, 30, 40, 30), -1)
        }
      } catch (_: Exception) {}
    }
  }

  fun startAlarmAlert(soundEnabled: Boolean, vibrationEnabled: Boolean) {
    scope.launch {
      alertJob?.cancel()
      alertJob = scope.launch {
        repeat(10) {
          if (soundEnabled) {
            try {
              toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 400)
            } catch (_: Exception) {}
          }
          if (vibrationEnabled) {
            try {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
              } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 200, 100, 200), -1)
              }
            } catch (_: Exception) {}
          }
          delay(700)
        }
      }
    }
  }

  fun stopAlarmAlert() {
    scope.launch {
      alertJob?.cancel()
      alertJob = null
      try {
        vibrator?.cancel()
      } catch (_: Exception) {}
    }
  }

  fun release() {
    scope.launch {
      alertJob?.cancel()
      alertJob = null
      try {
        vibrator?.cancel()
      } catch (_: Exception) {}
      try {
        toneGenerator?.release()
      } catch (_: Exception) {}
      toneGenerator = null
      scope.cancel()
    }
  }
}
