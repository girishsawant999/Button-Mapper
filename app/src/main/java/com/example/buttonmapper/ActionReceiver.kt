package com.example.buttonmapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.widget.Toast

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action") ?: return
        val value = intent.getIntExtra("value", 0)
        val alarmId = intent.getStringExtra("alarm_id") ?: ""

        when (action) {
            "volume" -> {
                adjustVolume(context, value)
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val pct = Math.round((value.toFloat() / max) * 100)
                Toast.makeText(context, "Alarm Executed: Volume set to $pct% ($value)", Toast.LENGTH_LONG).show()
            }
            "brightness" -> {
                adjustBrightness(context, value)
                val pct = Math.round((value.toFloat() / 255f) * 100)
                Toast.makeText(context, "Alarm Executed: Brightness set to $pct% ($value)", Toast.LENGTH_LONG).show()
            }
        }

        // Reschedule for tomorrow
        if (alarmId.isNotEmpty()) {
            val alarms = StorageHelper.loadScheduledAlarms(context)
            val alarm = alarms.find { it.id == alarmId }
            if (alarm != null) {
                AlarmScheduler.scheduleAlarm(context, alarm)
            }
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
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, newVal)
        } catch (_: Exception) {}
    }
}