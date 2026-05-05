package com.kapcode.open.macropad.kmps

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.KeyEvent
import com.kapcode.open.macropad.kmps.hardware.HardwareTriggerManager
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import com.kapcode.open.macropad.kmps.settings.SlamFireTrigger
import kotlinx.coroutines.*

class SlamFireManager(
    private val context: Context,
    private val settingsViewModel: SettingsViewModel,
    private val scope: CoroutineScope,
    private val onSlam: (isDouble: Boolean) -> Unit
) : SensorEventListener, HardwareTriggerManager {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private var lastProximityState: Boolean? = null
    private var lastTriggerTime = 0L
    private var triggerPending = false
    private var isHandlingSlam = false

    override fun start() {
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun handleKeyDown(keyCode: Int): Boolean {
        if (settingsViewModel.slamFireEnabled.value) {
            val trigger = settingsViewModel.slamFireTrigger.value
            val isMatch = when (trigger) {
                SlamFireTrigger.VolumeDown -> keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                SlamFireTrigger.VolumeUp -> keyCode == KeyEvent.KEYCODE_VOLUME_UP
                SlamFireTrigger.Power -> keyCode == KeyEvent.KEYCODE_POWER
                SlamFireTrigger.Bixby -> keyCode == 1082 // Common Bixby code
                SlamFireTrigger.Assistant -> keyCode == KeyEvent.KEYCODE_ASSIST || keyCode == KeyEvent.KEYCODE_VOICE_ASSIST
                else -> false
            }
            if (isMatch) {
                processTrigger()
                return true
            }
        }
        return false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY && settingsViewModel.slamFireEnabled.value) {
            val distance = event.values[0]
            val maxRange = event.sensor.maximumRange
            
            // Many sensors are binary (0 for near, maxRange for far). 
            // For non-binary sensors, 5cm is a common "near" threshold.
            val threshold = if (maxRange > 5f) 5f else maxRange / 2f
            val isCovered = distance < threshold
            
            if (isCovered != lastProximityState) {
                lastProximityState = isCovered
                val trigger = settingsViewModel.slamFireTrigger.value
                val isTriggered = (trigger == SlamFireTrigger.ProximityCovered && isCovered) || 
                                 (trigger == SlamFireTrigger.ProximityUncovered && !isCovered)
                
                if (isTriggered) {
                    processTrigger()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun processTrigger() {
        val now = System.currentTimeMillis()
        val threshold = settingsViewModel.slamFireDoubleThreshold.value
        
        if (now - lastTriggerTime < threshold) {
            // Double tap detected
            triggerPending = false
            invokeSlam(true)
            lastTriggerTime = 0 // Reset to prevent triple tap as another double
        } else {
            // Potential single tap, wait to see if it's a double
            triggerPending = true
            lastTriggerTime = now
            scope.launch {
                delay(threshold + 10)
                if (triggerPending && System.currentTimeMillis() - lastTriggerTime >= threshold) {
                    triggerPending = false
                    invokeSlam(false)
                }
            }
        }
    }

    private fun invokeSlam(isDouble: Boolean) {
        if (isHandlingSlam) return
        isHandlingSlam = true
        onSlam(isDouble)
        scope.launch {
            delay(500) // Cooldown period
            isHandlingSlam = false
        }
    }
}
