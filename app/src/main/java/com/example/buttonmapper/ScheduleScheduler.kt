package com.example.buttonmapper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.os.Build
import android.widget.Toast
import java.util.Calendar

object ScheduleScheduler {
    private const val REQUEST_CODE = 9999
    private const val ACTION_HOURLY_VALIDATION = "com.example.buttonmapper.HOURLY_VALIDATION"

    fun startHourlyValidation(context: Context, force: Boolean = false) {
        val intent = Intent(context, ActionReceiver::class.java).apply {
            action = ACTION_HOURLY_VALIDATION
        }

        val existing = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // If alarm is already set and we don't want to force reschedule, do nothing
        if (existing != null && !force) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Calculate the next top of the hour
        val cal = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback for security/permission restrictions on exact alarms on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    fun validateAndExecute(context: Context) {
        val now = Calendar.getInstance()
        val tasks = StorageHelper.loadScheduledTasks(context)
        if (tasks.isEmpty()) return

        // 1. Calculate the most recent past occurrence time for each task
        val taskOccurrences = tasks.mapNotNull { task ->
            val occurrence = getMostRecentPastOccurrence(task, now)
            if (occurrence != null) {
                task to occurrence
            } else {
                null
            }
        }

        // 2. Group by action ("volume", "brightness")
        val grouped = taskOccurrences.groupBy { it.first.action }

        for ((action, occurrences) in grouped) {
            // Find the one with the latest occurrence time (most recent in the past)
            val latest = occurrences.maxByOrNull { it.second.timeInMillis } ?: continue
            val task = latest.first

            when (action) {
                "volume" -> {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    val currentVal = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (currentVal != task.value) {
                        adjustVolume(context, task.value)
                        val pct = Math.round((task.value.toFloat() / max) * 100)
                        Toast.makeText(context, "Schedule Executed: Volume set to $pct% (${task.value})", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Schedule Skipped: Volume is already at target", Toast.LENGTH_SHORT).show()
                    }
                }
                "brightness" -> {
                    val currentVal = try {
                        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                    } catch (e: Exception) {
                        -1
                    }
                    if (currentVal != task.value) {
                        adjustBrightness(context, task.value)
                        val pct = Math.round((task.value.toFloat() / 255f) * 100)
                        Toast.makeText(context, "Schedule Executed: Brightness set to $pct% (${task.value})", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Schedule Skipped: Brightness is already at target", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getMostRecentPastOccurrence(task: ScheduledTask, now: Calendar): Calendar? {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (task.dayOfWeek == 0) {
            // Everyday schedule
            cal.set(Calendar.HOUR_OF_DAY, task.hour)
            if (cal.after(now)) {
                // If it is after the current time today, then its last occurrence was yesterday
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            return cal
        } else {
            // Weekly schedule
            cal.set(Calendar.DAY_OF_WEEK, task.dayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, task.hour)
            if (cal.after(now)) {
                // If it is after the current time this week, then its last occurrence was last week
                cal.add(Calendar.WEEK_OF_YEAR, -1)
            }
            return cal
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
