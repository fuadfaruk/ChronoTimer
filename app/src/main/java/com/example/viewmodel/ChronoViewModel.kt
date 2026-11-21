package com.example.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PresetDefaults
import com.example.data.PresetRepository
import com.example.model.IntervalConfig
import com.example.model.IntervalPhase
import com.example.model.IntervalStep
import com.example.model.LapItem
import com.example.model.StopwatchStats
import com.example.model.TimerItem
import com.example.model.TimerPreset
import com.example.util.NoiseProfile
import com.example.util.SoundHapticHelper
import com.example.util.VoiceMaskingNoiseGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab {
  STOPWATCH,
  TIMER,
  INTERVAL,
  PRESETS
}

data class StopwatchUiState(
  val elapsedMillis: Long = 0L,
  val isRunning: Boolean = false,
  val laps: List<LapItem> = emptyList(),
  val stats: StopwatchStats = StopwatchStats()
)

data class IntervalUiState(
  val config: IntervalConfig = PresetDefaults.defaultIntervalConfigs.first(),
  val phase: IntervalPhase = IntervalPhase.READY,
  val phaseRemainingSeconds: Int = 0,
  val currentSet: Int = 1,
  val currentStepIndex: Int = 0,
  val isRunning: Boolean = false,
  val progressInPhase: Float = 1f
) {
  val currentStep: IntervalStep?
    get() = config.steps.getOrNull(currentStepIndex)
}

data class TimerUiState(
  val timers: List<TimerItem> = listOf(
    TimerItem(
      id = "default_timer",
      title = "Study Session",
      initialDurationMillis = 25 * 60 * 1000L,
      remainingMillis = 25 * 60 * 1000L,
      isRunning = false,
      colorHex = 0xFF00E5FF
    )
  ),
  val selectedTimerId: String = "default_timer",
  val ringingTimerId: String? = null
)

data class SettingsState(
  val soundEnabled: Boolean = true,
  val vibrationEnabled: Boolean = true,
  val keepScreenOn: Boolean = true,
  val isDarkTheme: Boolean = true,
  val whiteNoiseEnabled: Boolean = true,
  val whiteNoiseProfile: NoiseProfile = NoiseProfile.VOICE_MASKING,
  val whiteNoiseVolume: Float = 0.7f,
  val isWhiteNoisePlaying: Boolean = false,
  val previewingProfile: NoiseProfile? = null
)

class ChronoViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = PresetRepository(application)
  val soundHapticHelper = SoundHapticHelper(application)
  val noiseGenerator = VoiceMaskingNoiseGenerator()
  private var isPreviewActive = false
  private var previewProfile: NoiseProfile? = null

  private val _currentTab = MutableStateFlow(AppTab.TIMER)
  val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

  // Stopwatch state
  private val _stopwatchState = MutableStateFlow(StopwatchUiState())
  val stopwatchState: StateFlow<StopwatchUiState> = _stopwatchState.asStateFlow()
  private var stopwatchJob: Job? = null
  private var stopwatchStartTime: Long = 0L
  private var stopwatchAccumulatedTime: Long = 0L

  // Timer state
  private val _timerState = MutableStateFlow(TimerUiState())
  val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()
  private var timerTickerJob: Job? = null

  // Interval state
  private val _intervalState = MutableStateFlow(
    PresetDefaults.defaultIntervalConfigs.first().let { initial ->
      IntervalUiState(
        config = initial,
        phaseRemainingSeconds = initialIntervalSeconds(initial)
      )
    }
  )
  val intervalState: StateFlow<IntervalUiState> = _intervalState.asStateFlow()
  private var intervalJob: Job? = null

  // Presets state
  private val _customPresets = MutableStateFlow<List<TimerPreset>>(emptyList())
  val customPresets: StateFlow<List<TimerPreset>> = _customPresets.asStateFlow()

  private val _customIntervalPresets = MutableStateFlow<List<IntervalConfig>>(emptyList())
  val customIntervalPresets: StateFlow<List<IntervalConfig>> = _customIntervalPresets.asStateFlow()

  // Settings
  private val _settings = MutableStateFlow(
    SettingsState(
      soundEnabled = repository.getSoundEnabled(),
      vibrationEnabled = repository.getVibrationEnabled(),
      keepScreenOn = repository.getKeepScreenOn(),
      isDarkTheme = true,
      whiteNoiseEnabled = repository.getWhiteNoiseEnabled(),
      whiteNoiseProfile = try {
        NoiseProfile.valueOf(repository.getWhiteNoiseProfile())
      } catch (_: Exception) {
        NoiseProfile.VOICE_MASKING
      },
      whiteNoiseVolume = repository.getWhiteNoiseVolume(),
      isWhiteNoisePlaying = false
    )
  )
  val settings: StateFlow<SettingsState> = _settings.asStateFlow()

  init {
    _customPresets.value = repository.loadCustomPresets()
    _customIntervalPresets.value = repository.loadCustomIntervalPresets()
    startTimerTicker()
  }

  fun setTab(tab: AppTab) {
    _currentTab.value = tab
    soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)
  }

  // -------------------------------------------------------------
  // STOPWATCH CONTROLS
  // -------------------------------------------------------------
  fun startStopwatch() {
    if (_stopwatchState.value.isRunning) return
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
    soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)

    stopwatchStartTime = SystemClock.elapsedRealtime()
    _stopwatchState.update { it.copy(isRunning = true) }

    stopwatchJob?.cancel()
    stopwatchJob = viewModelScope.launch {
      while (isActive && _stopwatchState.value.isRunning) {
        val now = SystemClock.elapsedRealtime()
        val total = stopwatchAccumulatedTime + (now - stopwatchStartTime)
        _stopwatchState.update { it.copy(elapsedMillis = total) }
        delay(16) // ~60fps smooth visual update
      }
    }
  }

  fun pauseStopwatch() {
    if (!_stopwatchState.value.isRunning) return
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
    soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)

    val now = SystemClock.elapsedRealtime()
    stopwatchAccumulatedTime += (now - stopwatchStartTime)
    stopwatchJob?.cancel()
    stopwatchJob = null
    _stopwatchState.update { it.copy(isRunning = false, elapsedMillis = stopwatchAccumulatedTime) }
  }

  fun resetStopwatch() {
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
    soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)

    stopwatchJob?.cancel()
    stopwatchJob = null
    stopwatchStartTime = 0L
    stopwatchAccumulatedTime = 0L
    _stopwatchState.value = StopwatchUiState()
  }

  fun recordLap() {
    val current = _stopwatchState.value
    if (current.elapsedMillis == 0L) return

    soundHapticHelper.playLapSound(_settings.value.soundEnabled)
    soundHapticHelper.vibrateLap(_settings.value.vibrationEnabled)

    val previousTotal = current.laps.firstOrNull()?.overallTimeMillis ?: 0L
    val lapDelta = current.elapsedMillis - previousTotal
    val newLapNumber = current.laps.size + 1

    val newLap = LapItem(
      lapNumber = newLapNumber,
      lapTimeMillis = lapDelta,
      overallTimeMillis = current.elapsedMillis
    )

    val updatedLaps = listOf(newLap) + current.laps

    // Recalculate best and worst laps (if >= 2 laps)
    var bestIdx: Int? = null
    var worstIdx: Int? = null
    var totalTime = 0L

    if (updatedLaps.size >= 2) {
      var minTime = Long.MAX_VALUE
      var maxTime = Long.MIN_VALUE
      updatedLaps.forEachIndexed { index, lap ->
        totalTime += lap.lapTimeMillis
        if (lap.lapTimeMillis < minTime) {
          minTime = lap.lapTimeMillis
          bestIdx = index
        }
        if (lap.lapTimeMillis > maxTime) {
          maxTime = lap.lapTimeMillis
          worstIdx = index
        }
      }
    } else {
      totalTime = updatedLaps.firstOrNull()?.lapTimeMillis ?: 0L
    }

    val avg = if (updatedLaps.isNotEmpty()) totalTime / updatedLaps.size else 0L
    val stats = StopwatchStats(
      bestLapIndex = bestIdx,
      worstLapIndex = worstIdx,
      averageLapMillis = avg
    )

    _stopwatchState.update {
      it.copy(laps = updatedLaps, stats = stats)
    }
  }

  // -------------------------------------------------------------
  // TIMER CONTROLS
  // -------------------------------------------------------------
  private fun startTimerTicker() {
    timerTickerJob?.cancel()
    timerTickerJob = viewModelScope.launch {
      var lastTick = SystemClock.elapsedRealtime()
      while (isActive) {
        delay(100)
        val now = SystemClock.elapsedRealtime()
        val delta = now - lastTick
        lastTick = now

        var ringingId: String? = _timerState.value.ringingTimerId

        val updated = _timerState.value.timers.map { timer ->
          if (timer.isRunning) {
            val newRemaining = (timer.remainingMillis - delta).coerceAtLeast(0L)
            if (newRemaining == 0L && !timer.isFinished) {
              ringingId = timer.id
              soundHapticHelper.startAlarmAlert(
                _settings.value.soundEnabled,
                _settings.value.vibrationEnabled
              )
              timer.copy(remainingMillis = 0L, isRunning = false, isFinished = true)
            } else {
              timer.copy(remainingMillis = newRemaining)
            }
          } else {
            timer
          }
        }

        _timerState.update { it.copy(timers = updated, ringingTimerId = ringingId) }
        syncWhiteNoise()
      }
    }
  }

  fun selectTimer(timerId: String) {
    _timerState.update { it.copy(selectedTimerId = timerId) }
  }

  fun startTimer(timerId: String) {
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
    soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)

    _timerState.update { state ->
      val updated = state.timers.map { timer ->
        if (timer.id == timerId) {
          val remaining = if (timer.remainingMillis <= 0L) timer.initialDurationMillis else timer.remainingMillis
          timer.copy(isRunning = true, remainingMillis = remaining, isFinished = false)
        } else {
          timer
        }
      }
      state.copy(timers = updated)
    }
    syncWhiteNoise()
  }

  fun pauseTimer(timerId: String) {
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
    soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)

    _timerState.update { state ->
      val updated = state.timers.map { timer ->
        if (timer.id == timerId) timer.copy(isRunning = false) else timer
      }
      state.copy(timers = updated)
    }
    syncWhiteNoise()
  }

  fun resetTimer(timerId: String) {
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
    soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)

    _timerState.update { state ->
      val updated = state.timers.map { timer ->
        if (timer.id == timerId) {
          timer.copy(isRunning = false, remainingMillis = timer.initialDurationMillis, isFinished = false)
        } else {
          timer
        }
      }
      val ringing = if (state.ringingTimerId == timerId) {
        soundHapticHelper.stopAlarmAlert()
        null
      } else {
        state.ringingTimerId
      }
      state.copy(timers = updated, ringingTimerId = ringing)
    }
    syncWhiteNoise()
  }

  fun addTimeToTimer(timerId: String, additionalSeconds: Long) {
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
    soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)

    val addMillis = additionalSeconds * 1000L
    _timerState.update { state ->
      val updated = state.timers.map { timer ->
        if (timer.id == timerId) {
          val newRem = timer.remainingMillis + addMillis
          val newInit = if (timer.initialDurationMillis < newRem) newRem else timer.initialDurationMillis
          timer.copy(remainingMillis = newRem, initialDurationMillis = newInit, isFinished = false)
        } else {
          timer
        }
      }
      state.copy(timers = updated)
    }
  }

  fun createTimer(title: String, durationSeconds: Long, colorHex: Long = 0xFF00E5FF) {
    val durationMillis = (durationSeconds.coerceAtLeast(1L)) * 1000L
    val newId = UUID.randomUUID().toString()
    val newTimer = TimerItem(
      id = newId,
      title = title.ifBlank { "Timer" },
      initialDurationMillis = durationMillis,
      remainingMillis = durationMillis,
      isRunning = false,
      colorHex = colorHex
    )
    _timerState.update {
      it.copy(timers = it.timers + newTimer, selectedTimerId = newId)
    }
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
  }

  fun updateCurrentTimerDuration(timerId: String, durationSeconds: Long) {
    val durationMillis = (durationSeconds.coerceAtLeast(1L)) * 1000L
    _timerState.update { state ->
      val updated = state.timers.map { timer ->
        if (timer.id == timerId) {
          timer.copy(
            initialDurationMillis = durationMillis,
            remainingMillis = durationMillis,
            isRunning = false,
            isFinished = false
          )
        } else {
          timer
        }
      }
      state.copy(timers = updated)
    }
  }

  fun deleteTimer(timerId: String) {
    _timerState.update { state ->
      val filtered = state.timers.filter { it.id != timerId }
      val safeList = if (filtered.isEmpty()) {
        listOf(
          TimerItem(
            id = "default_timer",
            title = "Quick Timer",
            initialDurationMillis = 5 * 60 * 1000L,
            remainingMillis = 5 * 60 * 1000L,
            isRunning = false
          )
        )
      } else {
        filtered
      }
      val nextSelected = if (state.selectedTimerId == timerId) safeList.first().id else state.selectedTimerId
      state.copy(timers = safeList, selectedTimerId = nextSelected)
    }
    syncWhiteNoise()
  }

  fun dismissAlarm() {
    soundHapticHelper.stopAlarmAlert()
    _timerState.update { it.copy(ringingTimerId = null) }
  }

  fun applyPresetToTimer(preset: TimerPreset, startImmediately: Boolean = true) {
    val durationMillis = preset.durationSeconds * 1000L
    val newId = UUID.randomUUID().toString()
    val newTimer = TimerItem(
      id = newId,
      title = preset.title,
      initialDurationMillis = durationMillis,
      remainingMillis = durationMillis,
      isRunning = startImmediately,
      colorHex = preset.colorHex
    )
    _timerState.update {
      it.copy(timers = listOf(newTimer) + it.timers, selectedTimerId = newId)
    }
    _currentTab.value = AppTab.TIMER
    if (startImmediately) {
      soundHapticHelper.playClickSound(_settings.value.soundEnabled)
      syncWhiteNoise()
    }
  }

  // -------------------------------------------------------------
  // INTERVAL / HIIT CONTROLS
  // -------------------------------------------------------------
  private fun initialIntervalSeconds(config: IntervalConfig): Int {
    return if (config.isMultiStep) {
      config.steps.firstOrNull()?.durationSeconds ?: 0
    } else {
      if (config.warmupSec > 0) config.warmupSec else config.workSec
    }
  }

  fun selectIntervalConfig(config: IntervalConfig) {
    pauseInterval(playClick = false)
    _intervalState.value = IntervalUiState(
      config = config,
      phase = IntervalPhase.READY,
      phaseRemainingSeconds = initialIntervalSeconds(config),
      currentSet = 1,
      currentStepIndex = 0,
      isRunning = false,
      progressInPhase = 1f
    )
    syncWhiteNoise()
  }

  fun applyIntervalPreset(config: IntervalConfig, startImmediately: Boolean = true) {
    selectIntervalConfig(config)
    _currentTab.value = AppTab.INTERVAL
    if (startImmediately) {
      startInterval()
    }
  }

  fun startInterval() {
    if (_intervalState.value.isRunning) return
    val current = _intervalState.value

    soundHapticHelper.playCountdownBeep(_settings.value.soundEnabled, isFinalGo = true)
    soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)

    if (current.config.isMultiStep) {
      val steps = current.config.steps
      if (steps.isEmpty()) return

      val isStartingFresh = current.phase == IntervalPhase.READY || current.phase == IntervalPhase.FINISHED
      val startStepIndex = if (isStartingFresh) 0 else current.currentStepIndex.coerceIn(0, steps.lastIndex)
      val startSet = if (isStartingFresh) 1 else current.currentSet.coerceIn(1, current.config.repeatCount.coerceAtLeast(1))
      val stepDuration = steps[startStepIndex].durationSeconds.coerceAtLeast(1)
      val initialSeconds = if (isStartingFresh || current.phaseRemainingSeconds <= 0) {
        stepDuration
      } else {
        current.phaseRemainingSeconds
      }

      _intervalState.update {
        it.copy(
          isRunning = true,
          phase = IntervalPhase.WORK,
          currentStepIndex = startStepIndex,
          currentSet = startSet,
          phaseRemainingSeconds = initialSeconds,
          progressInPhase = initialSeconds.toFloat() / stepDuration.toFloat()
        )
      }
    } else {
      val nextPhase = if (current.phase == IntervalPhase.READY || current.phase == IntervalPhase.FINISHED) {
        if (current.config.warmupSec > 0) IntervalPhase.PREPARE else IntervalPhase.WORK
      } else {
        current.phase
      }

      val initialSeconds = when (nextPhase) {
        IntervalPhase.PREPARE -> if (current.phaseRemainingSeconds > 0) current.phaseRemainingSeconds else current.config.warmupSec
        IntervalPhase.WORK -> if (current.phaseRemainingSeconds > 0) current.phaseRemainingSeconds else current.config.workSec
        IntervalPhase.REST -> if (current.phaseRemainingSeconds > 0) current.phaseRemainingSeconds else current.config.restSec
        else -> current.config.workSec
      }

      _intervalState.update {
        it.copy(
          isRunning = true,
          phase = nextPhase,
          phaseRemainingSeconds = initialSeconds
        )
      }
    }

    syncWhiteNoise()

    intervalJob?.cancel()
    intervalJob = viewModelScope.launch {
      while (isActive && _intervalState.value.isRunning) {
        delay(1000)
        val s = _intervalState.value
        val nextSec = s.phaseRemainingSeconds - 1

        if (nextSec in 1..3) {
          soundHapticHelper.playCountdownBeep(_settings.value.soundEnabled, isFinalGo = false)
        }

        if (nextSec <= 0) {
          soundHapticHelper.playCountdownBeep(_settings.value.soundEnabled, isFinalGo = true)
          soundHapticHelper.vibrateLap(_settings.value.vibrationEnabled)
          advanceIntervalPhase()
        } else {
          val totalSec = if (s.config.isMultiStep) {
            s.currentStep?.durationSeconds ?: 1
          } else {
            when (s.phase) {
              IntervalPhase.PREPARE -> s.config.warmupSec
              IntervalPhase.WORK -> s.config.workSec
              IntervalPhase.REST -> s.config.restSec
              else -> 1
            }
          }.coerceAtLeast(1)
          val progress = nextSec.toFloat() / totalSec.toFloat()
          _intervalState.update {
            it.copy(phaseRemainingSeconds = nextSec, progressInPhase = progress)
          }
        }
      }
    }
  }

  private fun advanceIntervalPhase() {
    val s = _intervalState.value
    if (s.config.isMultiStep) {
      val steps = s.config.steps
      val totalCycles = s.config.repeatCount.coerceAtLeast(1)
      if (s.currentStepIndex + 1 < steps.size) {
        val nextIdx = s.currentStepIndex + 1
        val nextStep = steps[nextIdx]
        _intervalState.update {
          it.copy(
            phase = IntervalPhase.WORK,
            currentStepIndex = nextIdx,
            phaseRemainingSeconds = nextStep.durationSeconds,
            progressInPhase = 1f
          )
        }
      } else if (s.currentSet < totalCycles) {
        val nextStep = steps.first()
        _intervalState.update {
          it.copy(
            phase = IntervalPhase.WORK,
            currentStepIndex = 0,
            currentSet = it.currentSet + 1,
            phaseRemainingSeconds = nextStep.durationSeconds,
            progressInPhase = 1f
          )
        }
      } else {
        soundHapticHelper.startAlarmAlert(_settings.value.soundEnabled, _settings.value.vibrationEnabled)
        intervalJob?.cancel()
        _intervalState.update {
          it.copy(
            phase = IntervalPhase.FINISHED,
            phaseRemainingSeconds = 0,
            isRunning = false,
            progressInPhase = 0f
          )
        }
      }
      syncWhiteNoise()
      return
    }

    when (s.phase) {
      IntervalPhase.PREPARE -> {
        _intervalState.update {
          it.copy(
            phase = IntervalPhase.WORK,
            phaseRemainingSeconds = it.config.workSec,
            progressInPhase = 1f
          )
        }
      }
      IntervalPhase.WORK -> {
        if (s.currentSet >= s.config.sets) {
          soundHapticHelper.startAlarmAlert(_settings.value.soundEnabled, _settings.value.vibrationEnabled)
          intervalJob?.cancel()
          _intervalState.update {
            it.copy(
              phase = IntervalPhase.FINISHED,
              phaseRemainingSeconds = 0,
              isRunning = false,
              progressInPhase = 0f
            )
          }
        } else {
          _intervalState.update {
            it.copy(
              phase = IntervalPhase.REST,
              phaseRemainingSeconds = it.config.restSec,
              progressInPhase = 1f
            )
          }
        }
      }
      IntervalPhase.REST -> {
        _intervalState.update {
          it.copy(
            phase = IntervalPhase.WORK,
            phaseRemainingSeconds = it.config.workSec,
            currentSet = it.currentSet + 1,
            progressInPhase = 1f
          )
        }
      }
      else -> {}
    }
    syncWhiteNoise()
  }

  fun skipToNextIntervalStep() {
    val s = _intervalState.value
    if (!s.config.isMultiStep) return
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
    soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)
    if (s.phase == IntervalPhase.READY || s.phase == IntervalPhase.FINISHED) {
      val nextIdx = if (s.config.steps.size > 1) 1 else 0
      val step = s.config.steps[nextIdx]
      _intervalState.update {
        it.copy(
          phase = IntervalPhase.WORK,
          currentStepIndex = nextIdx,
          currentSet = 1,
          phaseRemainingSeconds = step.durationSeconds,
          progressInPhase = 1f
        )
      }
      syncWhiteNoise()
    } else {
      advanceIntervalPhase()
    }
  }

  fun jumpToIntervalStep(stepIndex: Int) {
    val s = _intervalState.value
    if (!s.config.isMultiStep || stepIndex !in s.config.steps.indices) return
    val step = s.config.steps[stepIndex]
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
    _intervalState.update {
      it.copy(
        currentStepIndex = stepIndex,
        phase = if (it.isRunning) IntervalPhase.WORK else IntervalPhase.READY,
        phaseRemainingSeconds = step.durationSeconds,
        progressInPhase = 1f
      )
    }
    syncWhiteNoise()
  }

  fun pauseInterval(playClick: Boolean = true) {
    intervalJob?.cancel()
    intervalJob = null
    _intervalState.update { it.copy(isRunning = false) }
    syncWhiteNoise()
    if (playClick) {
      soundHapticHelper.playClickSound(_settings.value.soundEnabled)
      soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)
    }
  }

  fun resetInterval() {
    pauseInterval(playClick = false)
    val config = _intervalState.value.config
    _intervalState.update {
      IntervalUiState(
        config = config,
        phase = IntervalPhase.READY,
        phaseRemainingSeconds = initialIntervalSeconds(config),
        currentSet = 1,
        currentStepIndex = 0,
        isRunning = false,
        progressInPhase = 1f
      )
    }
    syncWhiteNoise()
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
    soundHapticHelper.vibrateClick(_settings.value.vibrationEnabled)
  }

  fun updateCustomInterval(warmup: Int, work: Int, rest: Int, sets: Int) {
    val updated = IntervalConfig(
      id = "custom",
      name = "Custom Interval",
      warmupSec = warmup.coerceAtLeast(0),
      workSec = work.coerceAtLeast(5),
      restSec = rest.coerceAtLeast(0),
      sets = sets.coerceAtLeast(1)
    )
    selectIntervalConfig(updated)
  }

  fun saveCustomIntervalPreset(preset: IntervalConfig) {
    val normalized = preset.copy(
      id = preset.id.ifBlank { UUID.randomUUID().toString() },
      name = preset.name.trim().ifBlank { "Custom Interval" },
      repeatCount = preset.repeatCount.coerceAtLeast(1),
      isCustom = true
    )
    val existing = _customIntervalPresets.value
    val updated = if (existing.any { it.id == normalized.id }) {
      existing.map { if (it.id == normalized.id) normalized else it }
    } else {
      listOf(normalized) + existing
    }
    _customIntervalPresets.value = updated
    repository.saveCustomIntervalPresets(updated)
    selectIntervalConfig(normalized)
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
  }

  fun deleteCustomIntervalPreset(presetId: String) {
    val updated = _customIntervalPresets.value.filter { it.id != presetId }
    _customIntervalPresets.value = updated
    repository.saveCustomIntervalPresets(updated)
    if (_intervalState.value.config.id == presetId) {
      selectIntervalConfig(PresetDefaults.defaultIntervalConfigs.first())
    }
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
  }

  // -------------------------------------------------------------
  // PRESET MANAGEMENT
  // -------------------------------------------------------------
  fun addCustomPreset(title: String, durationSeconds: Long, category: String, colorHex: Long) {
    val newPreset = TimerPreset(
      id = UUID.randomUUID().toString(),
      title = title.ifBlank { "Custom Timer" },
      durationSeconds = durationSeconds.coerceAtLeast(5L),
      category = category.ifBlank { "Custom" },
      iconName = "Timer",
      colorHex = colorHex
    )
    val updated = _customPresets.value + newPreset
    _customPresets.value = updated
    repository.saveCustomPresets(updated)
    soundHapticHelper.playClickSound(_settings.value.soundEnabled)
  }

  fun deleteCustomPreset(presetId: String) {
    val updated = _customPresets.value.filter { it.id != presetId }
    _customPresets.value = updated
    repository.saveCustomPresets(updated)
  }

  // -------------------------------------------------------------
  // SETTINGS
  // -------------------------------------------------------------
  fun toggleSound() {
    val next = !_settings.value.soundEnabled
    _settings.update { it.copy(soundEnabled = next) }
    repository.setSoundEnabled(next)
    if (next) soundHapticHelper.playClickSound(true)
  }

  fun toggleVibration() {
    val next = !_settings.value.vibrationEnabled
    _settings.update { it.copy(vibrationEnabled = next) }
    repository.setVibrationEnabled(next)
    if (next) soundHapticHelper.vibrateClick(true)
  }

  fun toggleKeepScreenOn() {
    val next = !_settings.value.keepScreenOn
    _settings.update { it.copy(keepScreenOn = next) }
    repository.setKeepScreenOn(next)
  }

  fun toggleTheme() {
    _settings.update { it.copy(isDarkTheme = !it.isDarkTheme) }
  }

  fun syncWhiteNoise() {
    val anyTimerRunning = _timerState.value.timers.any { it.isRunning }
    val interval = _intervalState.value
    val activePreview = if (isPreviewActive) (previewProfile ?: _settings.value.whiteNoiseProfile) else null

    var shouldPlay = false
    var targetProfile = _settings.value.whiteNoiseProfile

    if (activePreview != null) {
      shouldPlay = true
      targetProfile = activePreview
    } else if (interval.isRunning) {
      if (interval.config.isMultiStep) {
        val stepSound = interval.currentStep?.backgroundSound
        if (stepSound != null) {
          shouldPlay = true
          targetProfile = stepSound
        } else if (_settings.value.whiteNoiseEnabled && anyTimerRunning) {
          shouldPlay = true
          targetProfile = _settings.value.whiteNoiseProfile
        }
      } else {
        if (_settings.value.whiteNoiseEnabled && (interval.phase == IntervalPhase.WORK || anyTimerRunning)) {
          shouldPlay = true
          targetProfile = _settings.value.whiteNoiseProfile
        }
      }
    } else if (_settings.value.whiteNoiseEnabled && anyTimerRunning) {
      shouldPlay = true
      targetProfile = _settings.value.whiteNoiseProfile
    }

    if (shouldPlay) {
      if (!noiseGenerator.isPlaying) {
        noiseGenerator.start(targetProfile, _settings.value.whiteNoiseVolume)
      } else {
        noiseGenerator.updateVolume(_settings.value.whiteNoiseVolume)
        if (noiseGenerator.currentProfile != targetProfile) {
          noiseGenerator.updateProfile(targetProfile)
        }
      }
      _settings.update { it.copy(isWhiteNoisePlaying = true, previewingProfile = activePreview) }
    } else {
      if (noiseGenerator.isPlaying) {
        noiseGenerator.stop()
      }
      _settings.update { it.copy(isWhiteNoisePlaying = false, previewingProfile = null) }
    }
  }

  fun toggleWhiteNoise() {
    val next = !_settings.value.whiteNoiseEnabled
    _settings.update { it.copy(whiteNoiseEnabled = next) }
    repository.setWhiteNoiseEnabled(next)
    syncWhiteNoise()
  }

  fun setWhiteNoiseProfile(profile: NoiseProfile) {
    _settings.update { it.copy(whiteNoiseProfile = profile) }
    repository.setWhiteNoiseProfile(profile.name)
    noiseGenerator.updateProfile(profile)
    syncWhiteNoise()
  }

  fun setWhiteNoiseVolume(volume: Float) {
    val clamped = volume.coerceIn(0.05f, 1.0f)
    _settings.update { it.copy(whiteNoiseVolume = clamped) }
    repository.setWhiteNoiseVolume(clamped)
    noiseGenerator.updateVolume(clamped)
  }

  fun toggleWhiteNoisePreview() {
    if (isPreviewActive) {
      isPreviewActive = false
      previewProfile = null
    } else {
      isPreviewActive = true
      previewProfile = _settings.value.whiteNoiseProfile
    }
    syncWhiteNoise()
  }

  fun toggleNoiseProfilePreview(profile: NoiseProfile) {
    if (isPreviewActive && previewProfile == profile) {
      isPreviewActive = false
      previewProfile = null
    } else {
      isPreviewActive = true
      previewProfile = profile
    }
    syncWhiteNoise()
  }

  fun stopWhiteNoisePreview() {
    if (isPreviewActive || previewProfile != null) {
      isPreviewActive = false
      previewProfile = null
      syncWhiteNoise()
    }
  }

  override fun onCleared() {
    super.onCleared()
    stopwatchJob?.cancel()
    timerTickerJob?.cancel()
    intervalJob?.cancel()
    soundHapticHelper.release()
    noiseGenerator.stop()
  }
}
