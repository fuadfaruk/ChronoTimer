package com.example

import com.example.data.PresetDefaults
import com.example.model.IntervalConfig
import com.example.model.IntervalStep
import com.example.model.TimeFormatter
import com.example.util.NoiseProfile
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun intervalConfig_toEditableSteps_convertsStandardPresetCorrectly() {
    val pomodoro = PresetDefaults.defaultIntervalConfigs.first()
    val steps = pomodoro.toEditableSteps()
    assertEquals(2, steps.size)
    assertEquals("Study Focus", steps[0].name)
    assertEquals(pomodoro.workSec, steps[0].durationSeconds)
    assertEquals(NoiseProfile.VOICE_MASKING, steps[0].backgroundSound)
    assertEquals("Break Time", steps[1].name)
    assertEquals(pomodoro.restSec, steps[1].durationSeconds)
    assertNull(steps[1].backgroundSound)

    val movement = PresetDefaults.defaultIntervalConfigs.last()
    val movementSteps = movement.toEditableSteps()
    assertEquals(3, movementSteps.size)
    assertEquals("Get Ready", movementSteps[0].name)
    assertEquals(movement.warmupSec, movementSteps[0].durationSeconds)
  }

  @Test
  fun intervalConfig_multiStepDurationAndCustomAudioFields_arePreserved() {
    val step1 = IntervalStep(
      id = "s1",
      name = "Focus",
      durationSeconds = 900,
      backgroundSound = NoiseProfile.CUSTOM_AUDIO,
      customAudioUri = "content://audio/track1.mp3",
      customAudioName = "track1.mp3"
    )
    val step2 = IntervalStep(
      id = "s2",
      name = "Break",
      durationSeconds = 300,
      backgroundSound = null
    )
    val config = IntervalConfig(
      id = "custom_1",
      name = "Study Flow",
      repeatCount = 2,
      isCustom = true,
      steps = listOf(step1, step2)
    )
    assertTrue(config.isMultiStep)
    assertEquals(2400, config.totalDurationSeconds)
    assertEquals("40m", TimeFormatter.formatDurationSummary(config.totalDurationSeconds.toLong()))
    assertEquals("content://audio/track1.mp3", config.steps[0].customAudioUri)
    assertEquals("track1.mp3", config.steps[0].customAudioName)
    assertNull(config.steps[1].customAudioUri)
  }
}
