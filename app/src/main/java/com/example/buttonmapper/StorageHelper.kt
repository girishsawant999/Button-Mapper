package com.example.buttonmapper

import android.content.Context
import android.content.SharedPreferences

import org.json.JSONArray
import org.json.JSONObject

data class KeyMapping(val keyCode: Int, val action: String)
data class ScheduledAlarm(val id: String, val hour: Int, val minute: Int, val action: String, val value: Int)

object StorageHelper {
    private const val PREFS_NAME = "button_mapper_prefs"
    private const val KEY_MAPPINGS = "key_mappings"
    private const val SCHEDULED_ALARMS = "scheduled_alarms"

    fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveKeyMappings(context: Context, mappings: List<KeyMapping>) {
        val arr = JSONArray()
        mappings.forEach {
            val obj = JSONObject()
            obj.put("keyCode", it.keyCode)
            obj.put("action", it.action)
            arr.put(obj)
        }
        getPrefs(context).edit().putString(KEY_MAPPINGS, arr.toString()).apply()
    }

    fun loadKeyMappings(context: Context): List<KeyMapping> {
        val str = getPrefs(context).getString(KEY_MAPPINGS, null) ?: return emptyList()
        val arr = JSONArray(str)
        return List(arr.length()) {
            val obj = arr.getJSONObject(it)
            KeyMapping(obj.getInt("keyCode"), obj.getString("action"))
        }
    }

    fun saveScheduledAlarms(context: Context, alarms: List<ScheduledAlarm>) {
        val arr = JSONArray()
        alarms.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("hour", it.hour)
            obj.put("minute", it.minute)
            obj.put("action", it.action)
            obj.put("value", it.value)
            arr.put(obj)
        }
        getPrefs(context).edit().putString(SCHEDULED_ALARMS, arr.toString()).apply()
    }

    fun loadScheduledAlarms(context: Context): List<ScheduledAlarm> {
        val str = getPrefs(context).getString(SCHEDULED_ALARMS, null) ?: return emptyList()
        val arr = JSONArray(str)
        return List(arr.length()) {
            val obj = arr.getJSONObject(it)
            ScheduledAlarm(
                obj.getString("id"),
                obj.getInt("hour"),
                obj.getInt("minute"),
                obj.getString("action"),
                obj.getInt("value")
            )
        }
    }

    fun getActionLabel(context: Context, action: String): String {
        return when (action) {
            "volume_up" -> "Volume Up"
            "volume_down" -> "Volume Down"
            "brightness_up" -> "Brightness Up"
            "brightness_down" -> "Brightness Down"
            "launch_app" -> "Launch Button Mapper App"
            else -> {
                val pm = context.packageManager
                try {
                    val info = pm.getApplicationInfo(action, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (e: Exception) {
                    action
                }
            }
        }
    }
}