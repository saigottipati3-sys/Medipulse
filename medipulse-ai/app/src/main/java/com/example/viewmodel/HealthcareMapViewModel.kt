package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DistanceFilter
import com.example.data.model.HealthcareCategory
import com.example.data.model.HealthcarePlace
import com.example.data.model.RouteInfo
import com.example.data.model.RouteMode
import com.example.location.HealthcareLocationService
import com.example.location.LocationState
import com.example.repository.DirectionsRepository
import com.example.repository.DirectionsRepositoryImpl
import com.example.repository.HealthcarePlacesRepository
import com.example.repository.HealthcarePlacesRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HealthcareMapViewModel(
    application: Application,
    private val placesRepository: HealthcarePlacesRepository = HealthcarePlacesRepositoryImpl(),
    private val directionsRepository: DirectionsRepository = DirectionsRepositoryImpl(),
    val locationService: HealthcareLocationService = HealthcareLocationService(application)
) : AndroidViewModel(application) {

    private val TAG = "HealthcareMapVM"

    // Location state
    private val _userCoordinates = MutableStateFlow<Pair<Double, Double>?>(null)
    val userCoordinates: StateFlow<Pair<Double, Double>?> = _userCoordinates.asStateFlow()

    private val _mapCenter = MutableStateFlow<Pair<Double, Double>?>(null)
    val mapCenter: StateFlow<Pair<Double, Double>?> = _mapCenter.asStateFlow()

    private val _zoomLevel = MutableStateFlow(14.0f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    // Filters and mode
    private val _selectedCategory = MutableStateFlow(HealthcareCategory.ALL)
    val selectedCategory: StateFlow<HealthcareCategory> = _selectedCategory.asStateFlow()

    private val _selectedDistance = MutableStateFlow(DistanceFilter.FIVE_KM)
    val selectedDistance: StateFlow<DistanceFilter> = _selectedDistance.asStateFlow()

    private val _isEmergencyMode = MutableStateFlow(false)
    val isEmergencyMode: StateFlow<Boolean> = _isEmergencyMode.asStateFlow()

    private val _isListView = MutableStateFlow(false)
    val isListView: StateFlow<Boolean> = _isListView.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private var searchJob: Job? = null

    // Places data
    private val _allPlaces = MutableStateFlow<List<HealthcarePlace>>(emptyList())
    val allPlaces: StateFlow<List<HealthcarePlace>> = _allPlaces.asStateFlow()

    private val _displayedPlaces = MutableStateFlow<List<HealthcarePlace>>(emptyList())
    val displayedPlaces: StateFlow<List<HealthcarePlace>> = _displayedPlaces.asStateFlow()

    private val _selectedPlace = MutableStateFlow<HealthcarePlace?>(null)
    val selectedPlace: StateFlow<HealthcarePlace?> = _selectedPlace.asStateFlow()

    // Routing / Directions
    private val _activeRoute = MutableStateFlow<RouteInfo?>(null)
    val activeRoute: StateFlow<RouteInfo?> = _activeRoute.asStateFlow()

    private val _selectedRouteMode = MutableStateFlow(RouteMode.DRIVING)
    val selectedRouteMode: StateFlow<RouteMode> = _selectedRouteMode.asStateFlow()

    private val _isCalculatingRoute = MutableStateFlow(false)
    val isCalculatingRoute: StateFlow<Boolean> = _isCalculatingRoute.asStateFlow()

    // Loading & Error UI States
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isPermissionDenied = MutableStateFlow(false)
    val isPermissionDenied: StateFlow<Boolean> = _isPermissionDenied.asStateFlow()

    private val _isGpsDisabled = MutableStateFlow(false)
    val isGpsDisabled: StateFlow<Boolean> = _isGpsDisabled.asStateFlow()

    init {
        // Collect location state
        viewModelScope.launch {
            locationService.locationState.collect { state ->
                when (state) {
                    is LocationState.Loading -> {
                        _isLoading.value = true
                        _errorMessage.value = null
                    }
                    is LocationState.Success -> {
                        _isLoading.value = false
                        _userCoordinates.value = Pair(state.latitude, state.longitude)
                        if (_mapCenter.value == null) {
                            _mapCenter.value = Pair(state.latitude, state.longitude)
                        }
                        _isPermissionDenied.value = false
                        _isGpsDisabled.value = false
                        loadPlacesForLocation(state.latitude, state.longitude)
                    }
                    is LocationState.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = state.message
                        _isPermissionDenied.value = state.isPermissionDenied
                        _isGpsDisabled.value = state.isGpsDisabled
                    }
                    is LocationState.Idle -> {}
                }
            }
        }
        requestCurrentLocation()
    }

    fun requestCurrentLocation() {
        viewModelScope.launch {
            locationService.getCurrentLocation()
        }
    }

    fun recenterToUserLocation() {
        val userCoords = _userCoordinates.value
        if (userCoords != null) {
            _mapCenter.value = userCoords
            _zoomLevel.value = 14.5f
        } else {
            requestCurrentLocation()
        }
    }

    fun zoomIn() {
        _zoomLevel.value = minOf(18f, _zoomLevel.value + 1f)
    }

    fun zoomOut() {
        _zoomLevel.value = maxOf(10f, _zoomLevel.value - 1f)
    }

    fun setMapCenter(lat: Double, lon: Double) {
        _mapCenter.value = Pair(lat, lon)
    }

    fun toggleViewMode() {
        _isListView.value = !_isListView.value
    }

    fun selectCategory(category: HealthcareCategory) {
        _selectedCategory.value = category
        applyFilters()
    }

    fun selectDistance(distance: DistanceFilter) {
        _selectedDistance.value = distance
        val center = _mapCenter.value ?: _userCoordinates.value
        if (center != null) {
            loadPlacesForLocation(center.first, center.second)
        }
    }

    fun toggleEmergencyMode() {
        _isEmergencyMode.value = !_isEmergencyMode.value
        applyFilters()
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350) // Debounce search
            performSearch(newQuery)
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            applyFilters()
            return
        }

        _isLoading.value = true
        val center = _mapCenter.value ?: _userCoordinates.value ?: Pair(37.7749, -122.4194)
        val radius = _selectedDistance.value.radiusMeters

        // Check if query is an address or location search
        val geocoded = placesRepository.geocodeAddress(query)
        if (geocoded != null) {
            _mapCenter.value = geocoded
            loadPlacesForLocation(geocoded.first, geocoded.second)
            return
        }

        val result = placesRepository.searchPlaces(query, center.first, center.second, radius)
        _isLoading.value = false
        result.onSuccess { places ->
            _displayedPlaces.value = places
        }.onFailure {
            _errorMessage.value = "Failed to search places: ${it.message}"
        }
    }

    fun selectPlace(place: HealthcarePlace?) {
        _selectedPlace.value = place
        if (place != null) {
            _mapCenter.value = Pair(place.latitude, place.longitude)
        }
    }

    fun requestDirections(place: HealthcarePlace, mode: RouteMode = _selectedRouteMode.value) {
        val userLoc = _userCoordinates.value
        if (userLoc == null) {
            _errorMessage.value = "Cannot calculate directions without your current GPS location."
            return
        }
        _selectedRouteMode.value = mode
        _isCalculatingRoute.value = true
        viewModelScope.launch {
            try {
                val route = directionsRepository.calculateRoute(
                    startLat = userLoc.first,
                    startLon = userLoc.second,
                    endLat = place.latitude,
                    endLon = place.longitude,
                    destinationName = place.name,
                    mode = mode
                )
                _activeRoute.value = route
            } catch (e: Exception) {
                Log.e(TAG, "Route calculation failed: ${e.message}")
            } finally {
                _isCalculatingRoute.value = false
            }
        }
    }

    fun setRouteMode(mode: RouteMode) {
        _selectedRouteMode.value = mode
        val place = _selectedPlace.value
        if (place != null) {
            requestDirections(place, mode)
        }
    }

    fun clearActiveRoute() {
        _activeRoute.value = null
    }

    fun launchExternalNavigation(context: Context, place: HealthcarePlace) {
        val gmmIntentUri = Uri.parse("google.navigation:q=${place.latitude},${place.longitude}&mode=${if (_selectedRouteMode.value == RouteMode.WALKING) "w" else "d"}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            // Fallback to web browser or any map app
            val fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${place.latitude},${place.longitude}")
            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
        }
    }

    fun callPlace(context: Context, phoneNumber: String?) {
        val number = phoneNumber ?: "911"
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot launch dialer: ${e.message}")
        }
    }

    private fun loadPlacesForLocation(lat: Double, lon: Double) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val radius = _selectedDistance.value.radiusMeters
            val result = placesRepository.getNearbyHealthcarePlaces(
                latitude = lat,
                longitude = lon,
                radiusMeters = radius,
                category = HealthcareCategory.ALL
            )
            _isLoading.value = false
            result.onSuccess { places ->
                _allPlaces.value = places
                applyFilters()
            }.onFailure { e ->
                _errorMessage.value = "Failed to load healthcare places: ${e.message}"
            }
        }
    }

    private fun applyFilters() {
        var list = _allPlaces.value

        // Distance filter
        val maxDist = _selectedDistance.value.radiusMeters
        list = list.filter { it.distanceMeters <= maxDist }

        // Emergency mode filter
        if (_isEmergencyMode.value) {
            list = list.filter {
                it.isEmergencyAvailable ||
                it.category == HealthcareCategory.EMERGENCY_DEPT ||
                it.category == HealthcareCategory.AMBULANCE_SERVICE ||
                it.category == HealthcareCategory.HOSPITAL
            }.sortedWith(compareByDescending<HealthcarePlace> { it.isEmergencyAvailable }.thenBy { it.distanceMeters })
        } else {
            // Category filter
            val cat = _selectedCategory.value
            if (cat != HealthcareCategory.ALL) {
                list = list.filter { it.category == cat }
            }
        }

        // Search query filter if present
        val q = _searchQuery.value.trim()
        if (q.isNotBlank()) {
            list = list.filter {
                it.name.contains(q, ignoreCase = true) ||
                it.address.contains(q, ignoreCase = true) ||
                it.category.displayName.contains(q, ignoreCase = true) ||
                it.availableServices.any { s -> s.contains(q, ignoreCase = true) }
            }
        }

        _displayedPlaces.value = list
    }
}
