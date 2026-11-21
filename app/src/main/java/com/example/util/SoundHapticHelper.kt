package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SoundHapticHelper(private val context: Context) {

  private var toneGenerator: ToneGenerator? = null
  private var alertJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.Default)

  init {
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
    } catch (_: Exception) {
      // Ignored if device audio stream is unavailable
    }
  }

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vibratorManager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  fun playClickSound(soundEnabled: Boolean) {
    if (!soundEnabled) return
    try {
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
    } catch (_: Exception) {}
  }

  fun playLapSound(soundEnabled: Boolean) {
    if (!soundEnabled) return
    try {
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 50)
    } catch (_: Exception) {}
  }

  fun playCountdownBeep(soundEnabled: Boolean, isFinalGo: Boolean = false) {
    if (!soundEnabled) return
    try {
      val tone = if (isFinalGo) ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD else ToneGenerator.TONE_PROP_BEEP
      val duration = if (isFinalGo) 250 else 70
      toneGenerator?.startTone(tone, duration)
    } catch (_: Exception) {}
  }

  fun vibrateClick(vibrationEnabled: Boolean) {
    if (!vibrationEnabled) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(25)
      }
    } catch (_: Exception) {}
  }

  fun vibrateLap(vibrationEnabled: Boolean) {
    if (!vibrationEnabled) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 30), -1))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(longArrayOf(0, 30, 40, 30), -1)
      }
    } catch (_: Exception) {}
  }

  fun startAlarmAlert(soundEnabled: Boolean, vibrationEnabled: Boolean) {
    stopAlarmAlert()
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

  fun stopAlarmAlert() {
    alertJob?.cancel()
    alertJob = null
    try {
      vibrator?.cancel()
    } catch (_: Exception) {}
  }

  fun release() {
    stopAlarmAlert()
    toneGenerator?.release()
    toneGenerator = null
  }
}
