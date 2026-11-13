package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object AlertAudioPlayer {
  suspend fun playOutageChime() {
    withContext(Dispatchers.IO) {
      var generator: ToneGenerator? = null
      try {
        generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
        generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
        delay(240)
        generator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        delay(240)
      } catch (_: Throwable) {
        // Audio hardware unavailable in headless/muted environments
      } finally {
        runCatching { generator?.release() }
      }
    }
  }
}
