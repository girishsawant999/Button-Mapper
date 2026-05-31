package com.example.buttonmapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action") ?: return
        val value = intent.getIntExtra("value", 0)
        when (action) {
            "volume" -> adjustVolume(context, value)
            "brightness" -> adjustBrightness(context, value)
        }
    }

    private fun adjustVolume(context: Context, value: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVol = value.coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, AudioManager.FLAG_SHOW_UI)
    }

    private fun adjustBrightness(context: Context, value: Int) {
        try {
            val cResolver = context.contentResolver
            val newVal = value.coerceIn(10, 255)
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, newVal)
        } catch (_: Exception) {}
    }
}