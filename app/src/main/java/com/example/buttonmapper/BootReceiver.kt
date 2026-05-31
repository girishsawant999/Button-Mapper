package com.example.buttonmapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val alarms = StorageHelper.loadScheduledAlarms(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            for (alarm in alarms) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alarm.hour)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
                }
                val intentAction = Intent(context, ActionReceiver::class.java).apply {
                    putExtra("action", alarm.action)
                    putExtra("value", alarm.value)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarm.id.hashCode(),
                    intentAction,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}