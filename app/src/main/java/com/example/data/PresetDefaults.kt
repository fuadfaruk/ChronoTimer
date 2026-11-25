package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.IntervalConfig
import com.example.model.IntervalStep
import com.example.model.TimerPreset
import com.example.util.NoiseProfile
import org.json.JSONArray
import org.json.JSONObject

object PresetDefaults {
  val defaultTimerPresets = listOf(
    TimerPreset(
      id = "preset_pomodoro",
      title = "Pomodoro Study",
      durationSeconds = 1500, // 25 min
      category = "Study",
      iconName = "Schedule",
      colorHex = 0xFF00E5FF
    ),
    TimerPreset(
      id = "preset_deep_work",
      title = "Deep Focus Block",
      durationSeconds = 3000, // 50 min
      category = "Study",
      iconName = "School",
      colorHex = 0xFF00E676
    ),
    TimerPreset(
      id = "preset_short_break",
      title = "Study Break",
      durationSeconds = 300, // 5 min
      category = "Break",
      iconName = "SelfImprovement",
      colorHex = 0xFFB388FF
    ),
    TimerPreset(
      id = "preset_reading",
      title = "Reading Sprint",
      durationSeconds = 2700, // 45 min
      category = "Study",
      iconName = "MenuBook",
      colorHex = 0xFFFF9100
    ),
    TimerPreset(
      id = "preset_exam_review",
      title = "Exam Review",
      durationSeconds = 3600, // 60 min
      category = "Study",
      iconName = "Psychology",
      colorHex = 0xFFFF5252
    ),
    TimerPreset(
      id = "preset_quick_quiz",
      title = "Flashcard Drill",
      durationSeconds = 900, // 15 min
      category = "Study",
      iconName = "Speed",
      colorHex = 0xFFFFD600
    )
  )

  val defaultIntervalConfigs = listOf(
    IntervalConfig(
      id = "pomodoro_cycle",
      name = "Pomodoro (25m Study / 5m Break)",
      warmupSec = 0,
      workSec = 1500, // 25 min
      restSec = 300,  // 5 min
      sets = 4
    ),
    IntervalConfig(
      id = "focus_50_10",
      name = "Deep Focus (50m Study / 10m Break)",
      warmupSec = 0,
      workSec = 3000, // 50 min
      restSec = 600,  // 10 min
      sets = 3
    ),
    IntervalConfig(
      id = "sprint_review",
      name = "Review Sprint (15m Study / 3m Break)",
      warmupSec = 0,
      workSec = 900,  // 15 min
      restSec = 180,  // 3 min
      sets = 4
    ),
    IntervalConfig(
      id = "quick_tabata",
      name = "Quick Movement Break (20s / 10s)",
      warmupSec = 5,
      workSec = 20,
      restSec = 10,
      sets = 8
    )
  )
}

class PresetRepository(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("chrono_timer_prefs", Context.MODE_PRIVATE)

  private val KEY_CUSTOM_PRESETS = "custom_timer_presets"
  private val KEY_CUSTOM_INTERVAL_PRESETS = "custom_interval_presets"
  private val KEY_SOUND_ENABLED = "setting_sound_enabled"
  private val KEY_VIBE_ENABLED = "setting_vibe_enabled"
  private val KEY_KEEP_SCREEN = "setting_keep_screen"
  private val KEY_WHITE_NOISE_ENABLED = "setting_white_noise_enabled"
  private val KEY_WHITE_NOISE_PROFILE = "setting_white_noise_profile"
  private val KEY_WHITE_NOISE_VOLUME = "setting_white_noise_volume"
  private val KEY_CUSTOM_AUDIO_URI = "setting_custom_audio_uri"
  private val KEY_CUSTOM_AUDIO_NAME = "setting_custom_audio_name"
  private val KEY_QUICK_ADD_SECONDS = "setting_quick_add_seconds"
  private val KEY_THEME_MODE = "setting_theme_mode"
  private val KEY_DYNAMIC_COLOR = "setting_dynamic_color"

  fun getSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)
  fun setSoundEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

  fun getVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBE_ENABLED, true)
  fun setVibrationEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_VIBE_ENABLED, value).apply()

  fun getKeepScreenOn(): Boolean = prefs.getBoolean(KEY_KEEP_SCREEN, true)
  fun setKeepScreenOn(value: Boolean) = prefs.edit().putBoolean(KEY_KEEP_SCREEN, value).apply()

  fun getWhiteNoiseEnabled(): Boolean = prefs.getBoolean(KEY_WHITE_NOISE_ENABLED, true)
  fun setWhiteNoiseEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_WHITE_NOISE_ENABLED, value).apply()

  fun getWhiteNoiseProfile(): String = prefs.getString(KEY_WHITE_NOISE_PROFILE, "VOICE_MASKING") ?: "VOICE_MASKING"
  fun setWhiteNoiseProfile(profile: String) = prefs.edit().putString(KEY_WHITE_NOISE_PROFILE, profile).apply()

  fun getWhiteNoiseVolume(): Float = prefs.getFloat(KEY_WHITE_NOISE_VOLUME, 0.7f)
  fun setWhiteNoiseVolume(volume: Float) = prefs.edit().putFloat(KEY_WHITE_NOISE_VOLUME, volume).apply()

  fun getCustomAudioUri(): String? = prefs.getString(KEY_CUSTOM_AUDIO_URI, null)
  fun setCustomAudioUri(uri: String?) {
    if (uri.isNullOrBlank()) {
      prefs.edit().remove(KEY_CUSTOM_AUDIO_URI).apply()
    } else {
      prefs.edit().putString(KEY_CUSTOM_AUDIO_URI, uri).apply()
    }
  }

  fun getCustomAudioName(): String? = prefs.getString(KEY_CUSTOM_AUDIO_NAME, null)
  fun setCustomAudioName(name: String?) {
    if (name.isNullOrBlank()) {
      prefs.edit().remove(KEY_CUSTOM_AUDIO_NAME).apply()
    } else {
      prefs.edit().putString(KEY_CUSTOM_AUDIO_NAME, name).apply()
    }
  }

  fun getQuickAddSeconds(): Int = prefs.getInt(KEY_QUICK_ADD_SECONDS, 300).coerceAtLeast(1)
  fun setQuickAddSeconds(seconds: Int) = prefs.edit().putInt(KEY_QUICK_ADD_SECONDS, seconds.coerceAtLeast(1)).apply()

  fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
  fun setThemeMode(mode: String) = prefs.edit().putString(KEY_THEME_MODE, mode).apply()

  fun getDynamicColorEnabled(): Boolean = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
  fun setDynamicColorEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()

  fun loadCustomPresets(): List<TimerPreset> {
    val raw = prefs.getString(KEY_CUSTOM_PRESETS, null) ?: return emptyList()
    return try {
      val jsonArray = JSONArray(raw)
      val list = mutableListOf<TimerPreset>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(
          TimerPreset(
            id = obj.getString("id"),
            title = obj.getString("title"),
            durationSeconds = obj.getLong("durationSeconds"),
            category = obj.optString("category", "Custom"),
            iconName = obj.optString("iconName", "Timer"),
            colorHex = obj.optLong("colorHex", 0xFF00E5FF)
          )
        )
      }
      list
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun saveCustomPresets(presets: List<TimerPreset>) {
    try {
      val jsonArray = JSONArray()
      for (preset in presets) {
        val obj = JSONObject()
        obj.put("id", preset.id)
        obj.put("title", preset.title)
        obj.put("durationSeconds", preset.durationSeconds)
        obj.put("category", preset.category)
        obj.put("iconName", preset.iconName)
        obj.put("colorHex", preset.colorHex)
        jsonArray.put(obj)
      }
      prefs.edit().putString(KEY_CUSTOM_PRESETS, jsonArray.toString()).apply()
    } catch (_: Exception) {}
  }

  fun loadCustomIntervalPresets(): List<IntervalConfig> {
    val raw = prefs.getString(KEY_CUSTOM_INTERVAL_PRESETS, null) ?: return emptyList()
    return try {
      val jsonArray = JSONArray(raw)
      val list = mutableListOf<IntervalConfig>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val stepsArray = obj.optJSONArray("steps") ?: JSONArray()
        val steps = mutableListOf<IntervalStep>()
        for (j in 0 until stepsArray.length()) {
          val stepObj = stepsArray.getJSONObject(j)
          val soundName = if (stepObj.isNull("backgroundSound")) null else stepObj.optString("backgroundSound", "")
          val sound = if (soundName.isNullOrBlank()) null else try {
            NoiseProfile.valueOf(soundName)
          } catch (_: Exception) {
            null
          }
          steps.add(
            IntervalStep(
              id = stepObj.optString("id", "step_$j"),
              name = stepObj.optString("name", "Interval ${j + 1}"),
              durationSeconds = stepObj.optInt("durationSeconds", 60).coerceAtLeast(1),
              backgroundSound = sound,
              colorHex = stepObj.optLong("colorHex", 0xFF00E676)
            )
          )
        }
        if (steps.isNotEmpty()) {
          list.add(
            IntervalConfig(
              id = obj.getString("id"),
              name = obj.getString("name"),
              warmupSec = 0,
              workSec = steps.first().durationSeconds,
              restSec = 0,
              sets = obj.optInt("repeatCount", 1).coerceAtLeast(1),
              steps = steps,
              repeatCount = obj.optInt("repeatCount", 1).coerceAtLeast(1),
              isCustom = true
            )
          )
        }
      }
      list
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun saveCustomIntervalPresets(presets: List<IntervalConfig>) {
    try {
      val jsonArray = JSONArray()
      for (preset in presets) {
        val obj = JSONObject()
        obj.put("id", preset.id)
        obj.put("name", preset.name)
        obj.put("repeatCount", preset.repeatCount.coerceAtLeast(1))
        val stepsArray = JSONArray()
        for (step in preset.steps) {
          val stepObj = JSONObject()
          stepObj.put("id", step.id)
          stepObj.put("name", step.name)
          stepObj.put("durationSeconds", step.durationSeconds)
          if (step.backgroundSound != null) {
            stepObj.put("backgroundSound", step.backgroundSound.name)
          } else {
            stepObj.put("backgroundSound", JSONObject.NULL)
          }
          stepObj.put("colorHex", step.colorHex)
          stepsArray.put(stepObj)
        }
        obj.put("steps", stepsArray)
        jsonArray.put(obj)
      }
      prefs.edit().putString(KEY_CUSTOM_INTERVAL_PRESETS, jsonArray.toString()).apply()
    } catch (_: Exception) {}
  }
}
