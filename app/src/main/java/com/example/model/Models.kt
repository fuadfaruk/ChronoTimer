package com.example.model

import androidx.compose.runtime.Immutable
import com.example.util.NoiseProfile
import java.util.Locale
import java.util.UUID

@Immutable
data class LapItem(
  val lapNumber: Int,
  val lapTimeMillis: Long,
  val overallTimeMillis: Long
)

@Immutable
data class StopwatchStats(
  val bestLapIndex: Int? = null,
  val worstLapIndex: Int? = null,
  val averageLapMillis: Long = 0L
)

@Immutable
data class TimerItem(
  val id: String,
  val title: String,
  val initialDurationMillis: Long,
  val remainingMillis: Long,
  val isRunning: Boolean = false,
  val isFinished: Boolean = false,
  val hasStarted: Boolean = false,
  val colorHex: Long = 0xFF00E5FF
) {
  val progress: Float
    get() {
      if (initialDurationMillis <= 0L) return 0f
      return (remainingMillis.toFloat() / initialDurationMillis.toFloat()).coerceIn(0f, 1f)
    }
}

enum class IntervalPhase {
  READY,
  PREPARE,
  WORK,
  REST,
  FINISHED
}

@Immutable
data class IntervalStep(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val durationSeconds: Int,
  val backgroundSound: NoiseProfile? = null,
  val colorHex: Long = 0xFF00E676,
  val customAudioUri: String? = null,
  val customAudioName: String? = null
)

@Immutable
data class IntervalConfig(
  val id: String = "custom",
  val name: String = "Tabata Standard",
  val warmupSec: Int = 10,
  val workSec: Int = 20,
  val restSec: Int = 10,
  val sets: Int = 8,
  val steps: List<IntervalStep> = emptyList(),
  val repeatCount: Int = 1,
  val isCustom: Boolean = false
) {
  val isMultiStep: Boolean
    get() = steps.isNotEmpty()

  val totalDurationSeconds: Int
    get() = if (steps.isNotEmpty()) {
      steps.sumOf { it.durationSeconds } * repeatCount.coerceAtLeast(1)
    } else {
      warmupSec + (workSec + restSec) * sets
    }

  fun toEditableSteps(): List<IntervalStep> {
    if (steps.isNotEmpty()) return steps
    val generated = mutableListOf<IntervalStep>()
    if (warmupSec > 0) {
      generated.add(
        IntervalStep(
          name = "Get Ready",
          durationSeconds = warmupSec.coerceAtLeast(5),
          backgroundSound = null,
          colorHex = 0xFFFF9100L
        )
      )
    }
    generated.add(
      IntervalStep(
        name = "Study Focus",
        durationSeconds = workSec.coerceAtLeast(5),
        backgroundSound = NoiseProfile.VOICE_MASKING,
        colorHex = 0xFF00E676L
      )
    )
    if (restSec > 0) {
      generated.add(
        IntervalStep(
          name = "Break Time",
          durationSeconds = restSec.coerceAtLeast(5),
          backgroundSound = null,
          colorHex = 0xFF00E5FFL
        )
      )
    }
    return generated
  }
}

@Immutable
data class TimerPreset(
  val id: String,
  val title: String,
  val durationSeconds: Long,
  val category: String,
  val iconName: String,
  val colorHex: Long
)

object TimeFormatter {
  /**
   * Format stopwatch time: "00:00.00" or "00:00:00.00"
   */
  fun formatStopwatch(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val hundredths = (millis % 1000) / 10

    return if (hours > 0) {
      String.format(Locale.US, "%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
    } else {
      String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
    }
  }

  /**
   * Return parts of stopwatch for split rendering: Pair("00:00:00", ".00")
   */
  fun formatStopwatchParts(millis: Long): Pair<String, String> {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val hundredths = (millis % 1000) / 10

    val main = if (hours > 0) {
      String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
    val decimal = String.format(Locale.US, ".%02d", hundredths)
    return Pair(main, decimal)
  }

  /**
   * Format timer countdown: "HH:MM:SS" or "MM:SS"
   */
  fun formatTimer(millis: Long): String {
    val totalSeconds = ((millis + 999) / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
      String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
  }

  /**
   * Short duration summary, e.g. "25 min" or "1h 30m"
   */
  fun formatDurationSummary(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val parts = mutableListOf<String>()
    if (h > 0) parts.add("${h}h")
    if (m > 0) parts.add("${m}m")
    if (s > 0 && h == 0L) parts.add("${s}s")
    if (parts.isEmpty()) return "0s"
    return parts.joinToString(" ")
  }
}
