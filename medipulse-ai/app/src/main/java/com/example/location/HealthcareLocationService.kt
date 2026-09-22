package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class LocationState {
    object Idle : LocationState()
    object Loading : LocationState()
    data class Success(val latitude: Double, val longitude: Double, val accuracy: Float) : LocationState()
    data class Error(val message: String, val isPermissionDenied: Boolean = false, val isGpsDisabled: Boolean = false) : LocationState()
}

class HealthcareLocationService(private val context: Context) {
    private val TAG = "HealthcareLocationSvc"
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val _lastKnownLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val lastKnownLocation: StateFlow<Pair<Double, Double>?> = _lastKnownLocation.asStateFlow()

    fun isGpsProviderEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        _locationState.value = LocationState.Loading
        if (!isGpsProviderEnabled()) {
            _locationState.value = LocationState.Error(
                message = "GPS is disabled on your device. Please turn on location services in settings.",
                isGpsDisabled = true
            )
            return null
        }

        try {
            // First check last location for fast instant response
            val lastLoc = fusedLocationClient.lastLocation.await()
            if (lastLoc != null) {
                _lastKnownLocation.value = Pair(lastLoc.latitude, lastLoc.longitude)
                _locationState.value = LocationState.Success(lastLoc.latitude, lastLoc.longitude, lastLoc.accuracy)
            }

            // Then fetch fresh high accuracy location
            val freshLoc = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            if (freshLoc != null) {
                _lastKnownLocation.value = Pair(freshLoc.latitude, freshLoc.longitude)
                _locationState.value = LocationState.Success(freshLoc.latitude, freshLoc.longitude, freshLoc.accuracy)
                return freshLoc
            } else if (lastLoc != null) {
                return lastLoc
            } else {
                _locationState.value = LocationState.Error("Location is currently unavailable. Ensure you have clear sky visibility.")
                return null
            }
        } catch (e: SecurityException) {
            _locationState.value = LocationState.Error(
                message = "Location permission was denied. Please grant location access to see healthcare facilities near you.",
                isPermissionDenied = true
            )
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get location: ${e.message}")
            _locationState.value = LocationState.Error("Unable to retrieve GPS signal: ${e.localizedMessage}")
            return null
        }
    }
}
