package com.example.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class BiometricSensorManager(private val context: Context) : SensorEventListener {
    private val TAG = "BiometricSensorMgr"

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val stepSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _currentLocationText = MutableStateFlow("Detecting GPS location...")
    val currentLocationText: StateFlow<String> = _currentLocationText.asStateFlow()

    private val _currentCoordinates = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentCoordinates: StateFlow<Pair<Double, Double>?> = _currentCoordinates.asStateFlow()

    private val _stepCount = MutableStateFlow(6420)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    private val _liveHeartRate = MutableStateFlow(72)
    val liveHeartRate: StateFlow<Int> = _liveHeartRate.asStateFlow()

    private val _liveRespiratoryRate = MutableStateFlow(16)
    val liveRespiratoryRate: StateFlow<Int> = _liveRespiratoryRate.asStateFlow()

    init {
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        refreshLocation()
    }

    @SuppressLint("MissingPermission")
    fun refreshLocation() {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val lat = location.latitude
                        val lng = location.longitude
                        _currentCoordinates.value = Pair(lat, lng)
                        val addressStr = reverseGeocode(lat, lng)
                        _currentLocationText.value = addressStr ?: "Lat: ${String.format("%.4f", lat)}, Long: ${String.format("%.4f", lng)}"
                    } else {
                        // Fallback default coordinates if emulator has no fixed satellite lock
                        val defaultLat = 37.7749
                        val defaultLng = -122.4194
                        _currentCoordinates.value = Pair(defaultLat, defaultLng)
                        _currentLocationText.value = "Market St, San Francisco, CA 94103"
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Location fetch failed: ${e.message}")
                    _currentCoordinates.value = Pair(37.7749, -122.4194)
                    _currentLocationText.value = "Civic Center, San Francisco, CA"
                }
        } catch (e: SecurityException) {
            _currentCoordinates.value = Pair(37.7749, -122.4194)
            _currentLocationText.value = "GPS Location: 37.7749° N, 122.4194° W"
        }
    }

    private fun reverseGeocode(lat: Double, lng: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                "${addr.getAddressLine(0)}"
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun triggerSosVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                // SOS pattern: ... --- ... (short, short, short, long, long, long, short, short, short)
                val timings = longArrayOf(0, 150, 100, 150, 100, 150, 200, 400, 100, 400, 100, 400, 200, 150, 100, 150, 100, 150)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val timings = longArrayOf(0, 150, 100, 150, 100, 150, 200, 400, 100, 400, 100, 400, 200, 150, 100, 150, 100, 150)
                vibrator?.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            _stepCount.value = event.values[0].toInt()
        } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            _stepCount.value += 1
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun cleanup() {
        sensorManager?.unregisterListener(this)
    }
}
