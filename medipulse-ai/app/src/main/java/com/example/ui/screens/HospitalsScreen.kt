package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.HealthcareCategory
import com.example.data.model.HealthcarePlace
import com.example.data.model.Hospital
import com.example.ui.components.BookingDialog
import com.example.ui.components.map.CategoryFilterBar
import com.example.ui.components.map.DirectionsBottomSheet
import com.example.ui.components.map.DistanceFilterBar
import com.example.ui.components.map.HealthcareLocationCard
import com.example.ui.components.map.InteractiveMapView
import com.example.ui.components.map.LocationDetailsBottomSheet
import com.example.ui.components.map.MapErrorStateCard
import com.example.ui.components.map.PermissionBanner
import com.example.ui.components.map.SearchBarComponent
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.PrimaryBlue
import com.example.viewmodel.HealthcareMapViewModel
import com.example.viewmodel.MediPulseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalsScreen(
    viewModel: MediPulseViewModel,
    modifier: Modifier = Modifier,
    mapViewModel: HealthcareMapViewModel = viewModel()
) {
    val context = LocalContext.current

    // State from HealthcareMapViewModel
    val userCoords by mapViewModel.userCoordinates.collectAsState()
    val mapCenter by mapViewModel.mapCenter.collectAsState()
    val zoomLevel by mapViewModel.zoomLevel.collectAsState()
    val selectedCategory by mapViewModel.selectedCategory.collectAsState()
    val selectedDistance by mapViewModel.selectedDistance.collectAsState()
    val isEmergencyMode by mapViewModel.isEmergencyMode.collectAsState()
    val isListView by mapViewModel.isListView.collectAsState()
    val searchQuery by mapViewModel.searchQuery.collectAsState()
    val displayedPlaces by mapViewModel.displayedPlaces.collectAsState()
    val selectedPlace by mapViewModel.selectedPlace.collectAsState()
    val activeRoute by mapViewModel.activeRoute.collectAsState()
    val selectedRouteMode by mapViewModel.selectedRouteMode.collectAsState()
    val isCalculatingRoute by mapViewModel.isCalculatingRoute.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()
    val errorMessage by mapViewModel.errorMessage.collectAsState()
    val isPermissionDenied by mapViewModel.isPermissionDenied.collectAsState()
    val isGpsDisabled by mapViewModel.isGpsDisabled.collectAsState()

    // State from parent ViewModel
    val latestTriage by viewModel.latestTriageRecord.collectAsState()

    // Booking Dialog state
    var hospitalToBook by remember { mutableStateOf<Hospital?>(null) }

    // Bottom sheet states
    val detailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val directionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Location Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            mapViewModel.requestCurrentLocation()
        }
    }

    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("healthcare_locations_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header: Title, GPS Status, Call Emergency Action
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Nearby Healthcare",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            // Subtitle with live GPS status
                            val gpsStatusText = when {
                                userCoords != null -> "GPS Active • ${displayedPlaces.size} facilities within ${selectedDistance.label}"
                                isGpsDisabled -> "GPS is turned off"
                                isPermissionDenied -> "Location permission denied"
                                else -> "Acquiring satellite GPS..."
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (userCoords != null) Color(0xFF16A34A)
                                            else if (isGpsDisabled || isPermissionDenied) Color(0xFFDC2626)
                                            else Color(0xFFEAB308)
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = gpsStatusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Call Emergency 911 Direct Button
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("header_call_emergency_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call 911", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Bar
                    SearchBarComponent(
                        query = searchQuery,
                        onQueryChange = { mapViewModel.onSearchQueryChanged(it) },
                        onSearch = { mapViewModel.onSearchQueryChanged(it) },
                        placeholderText = "Search hospital, doctor, pharmacy, clinic, or city..."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Healthcare Category Chips
                    CategoryFilterBar(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { mapViewModel.selectCategory(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Distance Filter Bar
                    DistanceFilterBar(
                        selectedDistance = selectedDistance,
                        onDistanceSelected = { mapViewModel.selectDistance(it) }
                    )
                }
            }

            // Location Permission Warning Banner
            if (!hasLocationPermission || isPermissionDenied) {
                PermissionBanner(
                    onGrantPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Main Content Area: Map View vs List View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isListView) {
                    // List View Mode
                    if (displayedPlaces.isEmpty() && !isLoading) {
                        MapErrorStateCard(
                            title = "No Healthcare Facilities Found",
                            message = if (searchQuery.isNotBlank()) "No facilities matching '$searchQuery'. Try adjusting your search or increasing distance."
                            else "No facilities found within ${selectedDistance.label}. Expand the distance filter or check another category.",
                            icon = Icons.Default.SearchOff,
                            actionButtonText = "Expand to 25 km",
                            onAction = { mapViewModel.selectDistance(com.example.data.model.DistanceFilter.TWENTY_FIVE_KM) },
                            secondaryButtonText = "Show All Categories",
                            onSecondaryAction = { mapViewModel.selectCategory(HealthcareCategory.ALL) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("healthcare_list_view"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${displayedPlaces.size} Locations Found",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Sorted by proximity",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(displayedPlaces, key = { it.id }) { place ->
                                HealthcareLocationCard(
                                    place = place,
                                    onSelect = { mapViewModel.selectPlace(place) },
                                    onGetDirections = {
                                        mapViewModel.selectPlace(place)
                                        mapViewModel.requestDirections(place)
                                    },
                                    onCall = { mapViewModel.callPlace(context, place.phoneNumber) }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(60.dp))
                            }
                        }
                    }

                    // Floating action button to switch back to Map
                    FloatingActionButton(
                        onClick = { mapViewModel.toggleViewMode() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("list_view_switch_to_map"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = "Switch to Map")
                    }
                } else {
                    // Map View Mode
                    val currentCenter = mapCenter ?: userCoords ?: Pair(37.7749, -122.4194)

                    InteractiveMapView(
                        centerLat = currentCenter.first,
                        centerLon = currentCenter.second,
                        zoomLevel = zoomLevel,
                        userLocation = userCoords,
                        places = displayedPlaces,
                        selectedPlace = selectedPlace,
                        activeRoute = activeRoute,
                        isEmergencyMode = isEmergencyMode,
                        onPlaceClick = { place -> mapViewModel.selectPlace(place) },
                        onCenterChange = { lat, lon -> mapViewModel.setMapCenter(lat, lon) },
                        onZoomChange = { z -> mapViewModel.zoomIn() },
                        onRecenter = { mapViewModel.recenterToUserLocation() },
                        onToggleEmergencyMode = { mapViewModel.toggleEmergencyMode() },
                        onToggleListView = { mapViewModel.toggleViewMode() }
                    )
                }
            }
        }

        // Location Details Bottom Sheet
        selectedPlace?.let { place ->
            if (activeRoute == null) {
                LocationDetailsBottomSheet(
                    place = place,
                    sheetState = detailsSheetState,
                    onDismiss = { mapViewModel.selectPlace(null) },
                    onGetDirections = { targetPlace ->
                        mapViewModel.requestDirections(targetPlace)
                    },
                    onCall = { targetPlace ->
                        mapViewModel.callPlace(context, targetPlace.phoneNumber)
                    },
                    onBookAppointment = { targetPlace ->
                        // Convert to Hospital model for BookingDialog
                        hospitalToBook = Hospital(
                            id = targetPlace.id,
                            name = targetPlace.name,
                            type = targetPlace.category.displayName,
                            specialistTypes = if (targetPlace.availableServices.isNotEmpty()) targetPlace.availableServices else listOf("General Practitioner", "Specialist"),
                            address = targetPlace.address,
                            distanceMiles = targetPlace.distanceMeters / 1609.34,
                            latitude = targetPlace.latitude,
                            longitude = targetPlace.longitude,
                            rating = targetPlace.rating ?: 4.8,
                            isOpen24Hours = targetPlace.isEmergencyAvailable,
                            erWaitTimeMinutes = if (targetPlace.isEmergencyAvailable) 12 else null,
                            phoneNumber = targetPlace.phoneNumber ?: "+1 (555) 019-4820",
                            availableSlots = listOf("Immediate Walk-in", "Today 2:00 PM", "Today 4:30 PM", "Tomorrow 10:00 AM")
                        )
                    }
                )
            }
        }

        // Directions Bottom Sheet
        if (activeRoute != null || isCalculatingRoute) {
            DirectionsBottomSheet(
                routeInfo = activeRoute,
                isCalculating = isCalculatingRoute,
                selectedMode = selectedRouteMode,
                onModeChange = { mode -> mapViewModel.setRouteMode(mode) },
                onStartNavigation = {
                    selectedPlace?.let { place ->
                        mapViewModel.launchExternalNavigation(context, place)
                    }
                },
                onClose = { mapViewModel.clearActiveRoute() },
                sheetState = directionsSheetState
            )
        }

        // Booking Dialog Modal
        hospitalToBook?.let { hospital ->
            BookingDialog(
                hospital = hospital,
                initialSpecialty = latestTriage?.recommendedSpecialist ?: hospital.specialistTypes.firstOrNull() ?: "General Physician",
                initialReason = latestTriage?.summaryForDoctor ?: "Outpatient healthcare consultation",
                onDismiss = { hospitalToBook = null },
                onConfirmBooking = { hospitalName, doctorName, specialty, date, slot, reason ->
                    viewModel.bookAppointment(hospitalName, doctorName, specialty, date, slot, reason)
                    hospitalToBook = null
                    Toast.makeText(context, "Consultation scheduled at $hospitalName!", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
