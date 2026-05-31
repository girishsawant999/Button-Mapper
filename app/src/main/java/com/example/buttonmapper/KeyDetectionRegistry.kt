package com.example.buttonmapper

object KeyDetectionRegistry {
    @Volatile
    var isDetectionModeActive: Boolean = false

    @Volatile
    var onKeyDetected: ((Int) -> Unit)? = null
}
