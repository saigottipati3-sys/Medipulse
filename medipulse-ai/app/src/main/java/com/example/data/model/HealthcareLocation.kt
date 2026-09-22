package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class HealthcareCategory(
    val id: String,
    val displayName: String,
    val hexColor: Long,
    val isEmergencyPriority: Boolean = false
) {
    ALL("all", "All Places", 0xFF0284C7),
    HOSPITAL("hospital", "Hospitals", 0xFF0284C7, isEmergencyPriority = true),
    EMERGENCY_DEPT("emergency", "Emergency Depts", 0xFFDC2626, isEmergencyPriority = true),
    CLINIC("clinic", "Clinics", 0xFF0D9488),
    DOCTOR("doctor", "Doctors", 0xFF2563EB),
    PHARMACY("pharmacy", "Pharmacies", 0xFF16A34A),
    DIAGNOSTIC_LAB("lab", "Diagnostic / Labs", 0xFF7C3AED),
    AMBULANCE_SERVICE("ambulance", "Ambulance", 0xFFEA580C, isEmergencyPriority = true),
    BLOOD_BANK("blood_bank", "Blood Banks", 0xFFBE123C),
    MEDICAL_STORE("medical_store", "Medical Stores", 0xFF059669);

    companion object {
        fun fromOsmTag(amenity: String?, healthcare: String?, emergency: String?): HealthcareCategory {
            if (emergency != null && emergency.isNotBlank() && emergency != "no") {
                return if (amenity == "ambulance_station" || emergency == "ambulance_station") {
                    AMBULANCE_SERVICE
                } else {
                    EMERGENCY_DEPT
                }
            }
            if (amenity == "ambulance_station") return AMBULANCE_SERVICE
            if (amenity == "hospital" || healthcare == "hospital") {
                return if (emergency == "yes") EMERGENCY_DEPT else HOSPITAL
            }
            if (amenity == "clinic" || healthcare == "clinic") return CLINIC
            if (amenity == "doctors" || healthcare == "doctor") return DOCTOR
            if (amenity == "pharmacy") return PHARMACY
            if (amenity == "laboratory" || healthcare == "laboratory" || healthcare == "sample_collection") return DIAGNOSTIC_LAB
            if (amenity == "blood_bank" || healthcare == "blood_donation" || healthcare == "blood_bank") return BLOOD_BANK
            if (healthcare == "pharmacy" || amenity == "chemist") return MEDICAL_STORE
            return HOSPITAL
        }
    }
}

enum class DistanceFilter(val label: String, val radiusMeters: Int) {
    ONE_KM("1 km", 1_000),
    FIVE_KM("5 km", 5_000),
    TEN_KM("10 km", 10_000),
    TWENTY_FIVE_KM("25 km", 25_000);
}

enum class RouteMode(val label: String) {
    DRIVING("Driving"),
    WALKING("Walking")
}

data class HealthcarePlace(
    val id: String,
    val name: String,
    val category: HealthcareCategory,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val phoneNumber: String? = null,
    val openingHours: String? = null,
    val rating: Double? = null,
    val availableServices: List<String> = emptyList(),
    val isEmergencyAvailable: Boolean = false,
    val website: String? = null
) {
    val formattedDistance: String
        get() {
            return if (distanceMeters < 1000) {
                "${distanceMeters.toInt()} m"
            } else {
                val km = distanceMeters / 1000.0
                String.format("%.1f km", km)
            }
        }
}

data class RouteInfo(
    val destinationName: String,
    val distanceKm: Double,
    val durationMinutes: Int,
    val mode: RouteMode,
    val polylinePoints: List<Pair<Double, Double>>,
    val steps: List<String> = emptyList()
)
