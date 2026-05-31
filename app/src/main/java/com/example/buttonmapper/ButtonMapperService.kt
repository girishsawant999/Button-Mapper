package com.example.buttonmapper


import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class ButtonMapperService : AccessibilityService() {
    private var keyMappings: List<KeyMapping> = emptyList()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "key_mappings") {
            reloadKeyMappings()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        StorageHelper.getPrefs(this).registerOnSharedPreferenceChangeListener(prefListener)
        reloadKeyMappings()
    }

    private fun reloadKeyMappings() {
        keyMappings = StorageHelper.loadKeyMappings(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            StorageHelper.getPrefs(this).unregisterOnSharedPreferenceChangeListener(prefListener)
        } catch (_: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for key events, handled in onKeyEvent
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        if (KeyDetectionRegistry.isDetectionModeActive) {
            val keyCode = event.keyCode
            val isNavKey = keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    keyCode == KeyEvent.KEYCODE_ENTER ||
                    keyCode == KeyEvent.KEYCODE_BACK ||
                    keyCode == 160 // KeyCode for NUMPAD_ENTER if not explicitly defined

            if (!isNavKey) {
                KeyDetectionRegistry.onKeyDetected?.invoke(keyCode)
                return true // Consume key event to restrict further click behavior
            }
        }

        // Search the cached keyMappings list (in-memory) to avoid disk read and JSON parsing on hot key presses
        val mapping = keyMappings.find { it.keyCode == event.keyCode }
        if (mapping != null) {
            when (mapping.action) {
                "launch_app" -> {
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                }
                "volume_up" -> adjustVolume(AudioManager.ADJUST_RAISE)
                "volume_down" -> adjustVolume(AudioManager.ADJUST_LOWER)
                // Add more actions as needed
                else -> {
                    var launchIntent = packageManager.getLaunchIntentForPackage(mapping.action)
                    if (launchIntent == null) {
                        launchIntent = packageManager.getLeanbackLaunchIntentForPackage(mapping.action)
                    }
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                    } else {
                        Toast.makeText(this, "Action: ${mapping.action} not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return true // Consume event
        }
        return false // Let system handle
    }

    private fun adjustVolume(direction: Int) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

}