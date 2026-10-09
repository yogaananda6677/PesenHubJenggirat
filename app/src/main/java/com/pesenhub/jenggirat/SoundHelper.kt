package com.pesenhub.jenggirat

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

object SoundHelper {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Bunyikan bel notifikasi pesanan masuk / sukses (Checklist #24)
    fun playBell() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSuccess() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
