package com.obhoy.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.pow
import kotlin.math.roundToInt

class BarometerElevationEngine(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    @Volatile
    var currentPressure: Float = 1013.25f
        private set

    @Volatile
    var baselinePressureHpa: Float = 1013.25f

    // NEW: tracks whether we've received a real sensor reading yet,
    // so we never report a floor based on default/uninitialized pressure.
    @Volatile
    var hasReading: Boolean = false
        private set

    // NEW: smoothing factor for the exponential moving average.
    // Lower = smoother/slower to react, higher = more responsive/noisier.
    // Exposed as a var so it can be tuned without touching logic.
    var smoothingAlpha: Float = 0.25f

    // NEW: configurable floor-to-floor height, defaults to prior hardcoded value.
    var floorHeightMeters: Double = 3.5

    private var isListening = false

    fun startListening(): Boolean {
        if (pressureSensor != null) {
            // Guard against double-registration if called more than once
            // without an intervening stopListening().
            if (!isListening) {
                sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
                isListening = true
            }
            return true
        }
        return false
    }

    fun stopListening() {
        if (isListening) {
            sensorManager.unregisterListener(this)
            isListening = false
        }
    }

    fun updateBaselinePressure(baselineHpa: Float) {
        this.baselinePressureHpa = baselineHpa
    }

    fun getEstimatedFloor(): String {
        if (pressureSensor == null) return "Unknown Floor"

        // Don't report a confident floor number before we've actually
        // received a real pressure sample — avoids silently reporting
        // "Ground Floor" from default/uninitialized values.
        if (!hasReading) return "Floor Unknown"

        // H = 44330 * (1 - (P / P0)^0.1903)
        val relativeHeightMeters = 44330.0 * (1.0 - (currentPressure / baselinePressureHpa).toDouble().pow(0.1903))
        val floor = (relativeHeightMeters / floorHeightMeters).roundToInt()

        return when {
            floor == 0 -> "Ground Floor"
            floor > 0 -> "Floor $floor"
            else -> "${-floor} floor(s) underground"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_PRESSURE) {
                val newReading = it.values[0]
                currentPressure = if (!hasReading) {
                    // First real sample — take it directly rather than
                    // blending with the placeholder default.
                    newReading
                } else {
                    // Exponential moving average to smooth out transient
                    // noise (HVAC cycling, nearby doors/elevators, etc.)
                    smoothingAlpha * newReading + (1 - smoothingAlpha) * currentPressure
                }
                hasReading = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
