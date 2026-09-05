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

    fun startListening(): Boolean {
        if (pressureSensor != null) {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
            return true
        }
        return false
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    fun updateBaselinePressure(baselineHpa: Float) {
        this.baselinePressureHpa = baselineHpa
    }

    fun getEstimatedFloor(): String {
        if (pressureSensor == null) return "Unknown Floor"

        // H = 44330 * (1 - (P / P0)^0.1903)
        val relativeHeightMeters = 44330.0 * (1.0 - (currentPressure / baselinePressureHpa).toDouble().pow(0.1903))
        val floor = (relativeHeightMeters / 3.5).roundToInt()

        return when {
            floor <= 0 -> "Ground Floor"
            else -> "Floor $floor"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_PRESSURE) {
                currentPressure = it.values[0]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
