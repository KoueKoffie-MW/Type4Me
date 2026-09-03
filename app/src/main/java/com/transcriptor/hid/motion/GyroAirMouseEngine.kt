package com.transcriptor.hid.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.transcriptor.hid.service.HidTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * 3D Gyroscope Air Mouse & Presentation Pointer Engine.
 *
 * Translates phone pitch and yaw angular velocity into relative HID mouse motion reports ($dX, dY$)
 * with adaptive tremor deadband filtering, non-linear cubic ballistics acceleration,
 * and a "Hold-to-Aim" dead-man switch to prevent unintended cursor drift.
 */
class GyroAirMouseEngine(
    context: Context,
    private val hidTransport: HidTransport,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _isAiming = MutableStateFlow(false)
    val isAiming: StateFlow<Boolean> = _isAiming.asStateFlow()

    private val _isAvailable = MutableStateFlow(gyroscope != null)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    // Sensitivity and ballistics configuration
    var sensitivity: Float = 1.2f
    private val deadbandThreshold = 0.045f // rad/s to filter physiological hand tremor

    /**
     * Starts aiming: registers gyroscope listener at highest rate (SENSOR_DELAY_GAME: ~50Hz).
     */
    fun startAiming() {
        if (_isAiming.value || gyroscope == null) return
        _isAiming.value = true
        sensorManager?.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
    }

    /**
     * Stops aiming: immediately unregisters sensor listener to preserve battery and freezes cursor.
     */
    fun stopAiming() {
        if (!_isAiming.value) return
        _isAiming.value = false
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_isAiming.value) return
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        // Angular velocities in rad/sec:
        // event.values[0] = pitch rate (up/down)
        // event.values[1] = roll rate
        // event.values[2] = yaw rate (left/right)
        val pitchRate = event.values[0]
        val yawRate = event.values[2]

        // Apply deadband filter to eliminate natural hand tremors
        val cleanPitch = if (abs(pitchRate) > deadbandThreshold) pitchRate else 0f
        val cleanYaw = if (abs(yawRate) > deadbandThreshold) yawRate else 0f

        if (cleanPitch == 0f && cleanYaw == 0f) return

        // Non-linear cubic ballistics curve: enables micro-precision on tiny icons
        // and swift sweeps across multi-monitor 4K displays
        val dX = calculateBallisticDelta(-cleanYaw, sensitivity)
        val dY = calculateBallisticDelta(-cleanPitch, sensitivity)

        if (dX != 0.toByte() || dY != 0.toByte()) {
            scope.launch {
                hidTransport.sendMouseReport(0, dX.toInt(), dY.toInt(), 0)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Sends mouse click event while in Air Mouse mode.
     */
    fun sendClick(buttonMask: Byte) {
        scope.launch {
            hidTransport.sendMouseReport(buttonMask.toInt(), 0, 0, 0)
            kotlinx.coroutines.delay(15)
            hidTransport.sendMouseReport(0, 0, 0, 0)
        }
    }

    fun destroy() {
        stopAiming()
    }

    companion object {
        fun calculateBallisticDelta(radPerSec: Float, sensitivity: Float): Byte {
            val speed = abs(radPerSec)
            if (speed < 0.01f) return 0

            val linearComponent = speed * 18.0f
            val cubicComponent = (speed * speed * speed) * 35.0f
            val magnitude = (linearComponent + cubicComponent) * sensitivity

            val rawDelta = sign(radPerSec) * magnitude
            return rawDelta.toInt().coerceIn(-127, 127).toByte()
        }
    }
}
