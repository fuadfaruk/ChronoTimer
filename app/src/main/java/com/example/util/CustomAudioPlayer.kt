package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Plays a user-selected device audio Uri on a seamless loop using Android MediaPlayer.
 * Lifecycle operations (data source setup, prepare, stop, release) run off the UI thread.
 */
class CustomAudioPlayer(private val context: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val playerLock = Any()

  private var mediaPlayer: MediaPlayer? = null
  private var prepareJob: Job? = null
  private var activeSessionId: Long = 0L

  @Volatile
  var isPlaying: Boolean = false
    private set

  @Volatile
  var currentUriString: String? = null
    private set

  @Volatile
  var targetVolume: Float = 0.7f
    private set

  fun start(
    uri: Uri,
    volume: Float,
    onError: (String) -> Unit = {}
  ) {
    val clampedVolume = volume.coerceIn(0f, 1f)
    val uriStr = uri.toString()

    synchronized(playerLock) {
      if (isPlaying && currentUriString == uriStr && mediaPlayer != null) {
        targetVolume = clampedVolume
        try {
          mediaPlayer?.setVolume(clampedVolume, clampedVolume)
        } catch (_: Exception) {
        }
        return
      }

      activeSessionId++
      val sessionId = activeSessionId
      targetVolume = clampedVolume
      currentUriString = uriStr
      isPlaying = true

      prepareJob?.cancel()
      releasePlayerLocked()

      prepareJob = scope.launch {
        var player: MediaPlayer? = null
        try {
          val mp = MediaPlayer().apply {
            setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            )
            setDataSource(context, uri)
            isLooping = true
            setOnErrorListener { _, what, extra ->
              Log.w("CustomAudioPlayer", "MediaPlayer error: what=$what extra=$extra")
              val shouldReport = synchronized(playerLock) {
                if (activeSessionId == sessionId) {
                  this@CustomAudioPlayer.isPlaying = false
                  this@CustomAudioPlayer.currentUriString = null
                  releasePlayerLocked()
                  true
                } else {
                  false
                }
              }
              if (shouldReport) {
                onError("Unable to play the selected audio file.")
              }
              true
            }
          }
          player = mp
          mp.prepare()

          val shouldStart = synchronized(playerLock) {
            if (!isActive || !isPlaying || activeSessionId != sessionId) {
              false
            } else {
              mediaPlayer = mp
              val vol = targetVolume
              mp.setVolume(vol, vol)
              true
            }
          }

          if (shouldStart) {
            mp.start()
          } else {
            safeRelease(mp)
          }
        } catch (e: Exception) {
          Log.w("CustomAudioPlayer", "Failed to start custom audio: ${e.message}")
          safeRelease(player)
          val shouldReport = synchronized(playerLock) {
            if (activeSessionId == sessionId) {
              isPlaying = false
              currentUriString = null
              mediaPlayer = null
              true
            } else {
              false
            }
          }
          if (shouldReport) {
            onError("Could not open the selected audio file.")
          }
        }
      }
    }
  }

  fun updateVolume(newVolume: Float) {
    val clamped = newVolume.coerceIn(0f, 1f)
    targetVolume = clamped
    synchronized(playerLock) {
      try {
        mediaPlayer?.setVolume(clamped, clamped)
      } catch (_: Exception) {
      }
    }
  }

  fun stop() {
    val jobToCancel: Job?
    val playerToRelease: MediaPlayer?
    synchronized(playerLock) {
      if (!isPlaying && mediaPlayer == null && prepareJob == null) return
      activeSessionId++
      isPlaying = false
      currentUriString = null
      jobToCancel = prepareJob
      prepareJob = null
      playerToRelease = mediaPlayer
      mediaPlayer = null
    }
    jobToCancel?.cancel()
    if (playerToRelease != null) {
      scope.launch {
        safeRelease(playerToRelease)
      }
    }
  }

  fun release() {
    stop()
  }

  private fun releasePlayerLocked() {
    val old = mediaPlayer
    mediaPlayer = null
    if (old != null) {
      scope.launch {
        safeRelease(old)
      }
    }
  }

  private fun safeRelease(mp: MediaPlayer?) {
    if (mp == null) return
    try {
      if (mp.isPlaying) {
        mp.stop()
      }
    } catch (_: Exception) {
    }
    try {
      mp.reset()
    } catch (_: Exception) {
    }
    try {
      mp.release()
    } catch (_: Exception) {
    }
  }
}
