package com.obhoy.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.obhoy.app.util.ElevationFormatter

class BarometerElevationEngine(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    @Volatile
    var currentPressure: Float = 1013.25f
        private set

    fun startListening(): Boolean {
        if (pressureSensor != null) {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
            return true
        }
        return false // Barometer hardware not present on device
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    fun getEstimatedFloor(): String {
        return ElevationFormatter.formatPressureToFloor(currentPressure)
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

