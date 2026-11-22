package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PresetRepository
import com.example.model.TimeFormatter
import com.example.util.NoiseProfile
import com.example.util.VoiceMaskingNoiseGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ChronoTimer", appName)
  }

  @Test
  fun `test time formatter stopwatch format`() {
    val formatted = TimeFormatter.formatStopwatch(65430L) // 1m 5s .43
    assertEquals("01:05.43", formatted)

    val parts = TimeFormatter.formatStopwatchParts(65430L)
    assertEquals("01:05", parts.first)
    assertEquals(".43", parts.second)
  }

  @Test
  fun `test time formatter timer format`() {
    val formattedTimer = TimeFormatter.formatTimer(300000L) // 5 minutes
    assertEquals("05:00", formattedTimer)
  }

  @Test
  fun `test voice masking noise profiles and repository persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = PresetRepository(context)

    // Verify default white noise setting is enabled
    assertTrue(repository.getWhiteNoiseEnabled())
    assertEquals(NoiseProfile.VOICE_MASKING.name, repository.getWhiteNoiseProfile())

    // Update settings
    repository.setWhiteNoiseEnabled(false)
    assertFalse(repository.getWhiteNoiseEnabled())

    repository.setWhiteNoiseProfile(NoiseProfile.DEEP_VOICE_MASKING.name)
    assertEquals(NoiseProfile.DEEP_VOICE_MASKING.name, repository.getWhiteNoiseProfile())

    repository.setWhiteNoiseVolume(0.85f)
    assertEquals(0.85f, repository.getWhiteNoiseVolume(), 0.001f)

    // Test generator state
    val generator = VoiceMaskingNoiseGenerator()
    assertFalse(generator.isPlaying)
    assertEquals(NoiseProfile.VOICE_MASKING, generator.currentProfile)

    generator.updateProfile(NoiseProfile.VOICE_MASKING)
    generator.updateVolume(0.75f)
    assertEquals(0.75f, generator.targetVolume, 0.001f)
  }

  @Test
  fun `test custom multi-step interval preset persistence with per-interval background sounds`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = PresetRepository(context)

    val customPreset = com.example.model.IntervalConfig(
      id = "study_flow_1",
      name = "Morning Deep Study",
      repeatCount = 2,
      isCustom = true,
      steps = listOf(
        com.example.model.IntervalStep(
          id = "step_1",
          name = "Warm-up Reading",
          durationSeconds = 300,
          backgroundSound = NoiseProfile.PINK_NOISE,
          colorHex = 0xFFFF9100L
        ),
        com.example.model.IntervalStep(
          id = "step_2",
          name = "Deep Problem Set",
          durationSeconds = 1500,
          backgroundSound = NoiseProfile.VOICE_MASKING,
          colorHex = 0xFF00E676L
        ),
        com.example.model.IntervalStep(
          id = "step_3",
          name = "Silent Break",
          durationSeconds = 180,
          backgroundSound = null,
          colorHex = 0xFF00E5FFL
        )
      )
    )

    repository.saveCustomIntervalPresets(listOf(customPreset))
    val loaded = repository.loadCustomIntervalPresets()

    assertEquals(1, loaded.size)
    val restored = loaded.first()
    assertEquals("study_flow_1", restored.id)
    assertEquals("Morning Deep Study", restored.name)
    assertEquals(2, restored.repeatCount)
    assertTrue(restored.isCustom)
    assertTrue(restored.isMultiStep)
    assertEquals(3, restored.steps.size)
    assertEquals("Warm-up Reading", restored.steps[0].name)
    assertEquals(NoiseProfile.PINK_NOISE, restored.steps[0].backgroundSound)
    assertEquals("Deep Problem Set", restored.steps[1].name)
    assertEquals(NoiseProfile.VOICE_MASKING, restored.steps[1].backgroundSound)
    assertEquals("Silent Break", restored.steps[2].name)
    assertEquals(null, restored.steps[2].backgroundSound)
    assertEquals((300 + 1500 + 180) * 2, restored.totalDurationSeconds)
  }

  @Test
  fun `test ChronoViewModel custom interval preset save, skip, and delete`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.viewmodel.ChronoViewModel(app)

    val preset = com.example.model.IntervalConfig(
      id = "vm_test_preset",
      name = "Focus & Chill",
      repeatCount = 1,
      isCustom = true,
      steps = listOf(
        com.example.model.IntervalStep(
          id = "s1",
          name = "Focus",
          durationSeconds = 60,
          backgroundSound = NoiseProfile.BROWN_NOISE
        ),
        com.example.model.IntervalStep(
          id = "s2",
          name = "Rest",
          durationSeconds = 30,
          backgroundSound = null
        )
      )
    )

    vm.saveCustomIntervalPreset(preset)
    assertEquals("vm_test_preset", vm.intervalState.value.config.id)
    assertEquals(0, vm.intervalState.value.currentStepIndex)
    assertEquals(60, vm.intervalState.value.phaseRemainingSeconds)
    assertEquals(NoiseProfile.BROWN_NOISE, vm.intervalState.value.currentStep?.backgroundSound)

    vm.skipToNextIntervalStep()
    assertEquals(1, vm.intervalState.value.currentStepIndex)
    assertEquals(30, vm.intervalState.value.phaseRemainingSeconds)
    assertEquals(null, vm.intervalState.value.currentStep?.backgroundSound)

    vm.deleteCustomIntervalPreset("vm_test_preset")
    assertTrue(vm.customIntervalPresets.value.none { it.id == "vm_test_preset" })
  }

  @Test
  fun `test customizable timer quick add increment persistence and timer update`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val repository = PresetRepository(app)

    // Default should be 300s (5m)
    assertEquals(300, repository.getQuickAddSeconds())

    val vm = com.example.viewmodel.ChronoViewModel(app)
    assertEquals(300, vm.settings.value.quickAddSeconds)

    // Customize to 2m 45s (165s)
    vm.setQuickAddIncrement(minutes = 2, seconds = 45)
    assertEquals(165, vm.settings.value.quickAddSeconds)
    assertEquals(165, repository.getQuickAddSeconds())

    val timerId = vm.timerState.value.selectedTimerId
    val beforeMillis = vm.timerState.value.timers.first { it.id == timerId }.remainingMillis
    vm.addTimeToTimer(timerId, vm.settings.value.quickAddSeconds.toLong())
    val afterMillis = vm.timerState.value.timers.first { it.id == timerId }.remainingMillis
    assertEquals(beforeMillis + 165_000L, afterMillis)
  }

  @Test
  fun `test timer hasStarted full-screen focus state lifecycle`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.viewmodel.ChronoViewModel(app)
    val timerId = vm.timerState.value.selectedTimerId

    // Initial state: not started (normal mode)
    var timer = vm.timerState.value.timers.first { it.id == timerId }
    assertFalse(timer.hasStarted)
    assertFalse(timer.isRunning)

    // Start timer -> enters full-screen focus mode
    vm.startTimer(timerId)
    timer = vm.timerState.value.timers.first { it.id == timerId }
    assertTrue(timer.hasStarted)
    assertTrue(timer.isRunning)

    // Pause timer -> remains in full-screen focus mode so Reset becomes visible
    vm.pauseTimer(timerId)
    timer = vm.timerState.value.timers.first { it.id == timerId }
    assertTrue(timer.hasStarted)
    assertFalse(timer.isRunning)

    // Add time while paused -> still in full-screen focus mode
    vm.addTimeToTimer(timerId, 60L)
    timer = vm.timerState.value.timers.first { it.id == timerId }
    assertTrue(timer.hasStarted)

    // Reset timer -> exits full-screen focus mode and returns to normal mode
    vm.resetTimer(timerId)
    timer = vm.timerState.value.timers.first { it.id == timerId }
    assertFalse(timer.hasStarted)
    assertFalse(timer.isRunning)

    // Launch a preset immediately -> enters full-screen focus mode for the new preset timer
    val preset = com.example.model.TimerPreset(
      id = "focus_test",
      title = "Quick Sprint",
      durationSeconds = 600L,
      category = "Study",
      iconName = "school",
      colorHex = 0xFF00E5FFL
    )
    vm.applyPresetToTimer(preset, startImmediately = true)
    val launched = vm.timerState.value.timers.first { it.id == vm.timerState.value.selectedTimerId }
    assertTrue(launched.hasStarted)
    assertTrue(launched.isRunning)
    vm.pauseTimer(launched.id)
  }

  @Test
  fun `test Material You dynamic color and system theme mode defaults and persistence`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val repository = PresetRepository(app)

    assertEquals("SYSTEM", repository.getThemeMode())
    assertTrue(repository.getDynamicColorEnabled())

    val vm = com.example.viewmodel.ChronoViewModel(app)
    assertEquals(com.example.ui.theme.AppThemeMode.SYSTEM, vm.settings.value.themeMode)
    assertTrue(vm.settings.value.dynamicColorEnabled)

    vm.setThemeMode(com.example.ui.theme.AppThemeMode.LIGHT)
    vm.toggleDynamicColor()
    assertEquals(com.example.ui.theme.AppThemeMode.LIGHT, vm.settings.value.themeMode)
    assertFalse(vm.settings.value.dynamicColorEnabled)
    assertEquals("LIGHT", repository.getThemeMode())
    assertFalse(repository.getDynamicColorEnabled())
  }
}
