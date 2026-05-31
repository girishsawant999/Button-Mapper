package com.example.buttonmapper


import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class ButtonMapperService : AccessibilityService() {
    private var keyMappings: List<KeyMapping> = emptyList()

    override fun onServiceConnected() {
        super.onServiceConnected()
        keyMappings = StorageHelper.loadKeyMappings(this)
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

        if (keyMappings.isEmpty()) {
            keyMappings = StorageHelper.loadKeyMappings(this)
        }
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
                "brightness_up" -> adjustBrightness(20)
                "brightness_down" -> adjustBrightness(-20)
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
                        Toast.makeText(this, "Action: ${mapping.action}", Toast.LENGTH_SHORT).show()
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

    private fun adjustBrightness(delta: Int) {
        try {
            val cResolver = contentResolver
            val cur = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 100)
            val newVal = (cur + delta).coerceIn(10, 255)
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, newVal)
        } catch (e: Exception) {
            Toast.makeText(this, "Brightness error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}