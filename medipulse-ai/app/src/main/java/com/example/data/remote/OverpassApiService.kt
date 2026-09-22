package com.example.data.remote

import android.util.Log
import com.example.data.model.HealthcareCategory
import com.example.data.model.HealthcarePlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.*

class OverpassApiService {
    private val TAG = "OverpassApiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchNearbyHealthcare(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        categoryFilter: HealthcareCategory = HealthcareCategory.ALL
    ): List<HealthcarePlace> = withContext(Dispatchers.IO) {
        val queryFilter = when (categoryFilter) {
            HealthcareCategory.ALL -> """
                node["amenity"~"hospital|clinic|doctors|pharmacy|ambulance_station|blood_bank"](around:$radiusMeters,$latitude,$longitude);
                way["amenity"~"hospital|clinic|doctors|pharmacy|ambulance_station|blood_bank"](around:$radiusMeters,$latitude,$longitude);
                node["healthcare"](around:$radiusMeters,$latitude,$longitude);
                way["healthcare"](around:$radiusMeters,$latitude,$longitude);
            """.trimIndent()
            HealthcareCategory.HOSPITAL -> """
                node["amenity"="hospital"](around:$radiusMeters,$latitude,$longitude);
                way["amenity"="hospital"](around:$radiusMeters,$latitude,$longitude);
                node["healthcare"="hospital"](around:$radiusMeters,$latitude,$longitude);
                way["healthcare"="hospital"](around:$radiusMeters,$latitude,$longitude);
            """.trimIndent()
            HealthcareCategory.EMERGENCY_DEPT -> """
                node["emergency"~"yes|ambulance_station|defibrillator"](around:$radiusMeters,$latitude,$longitude);
                way["emergency"~"yes|ambulance_station"](around:$radiusMeters,$latitude,$longitude);
                node["amenity"="hospital"]["emergency"="yes"](around:$radiusMeters,$latitude,$longitude);
                way["amenity"="hospital"]["emergency"="yes"](around:$radiusMeters,$latitude,$longitude);
            """.trimIndent()
            HealthcareCategory.CLINIC -> """
                node["amenity"="clinic"](around:$radiusMeters,$latitude,$longitude);
                way["amenity"="clinic"](around:$radiusMeters,$latitude,$longitude);
                node["healthcare"="clinic"](around:$radiusMeters,$latitude,$longitude);
                way["healthcare"="clinic"](around:$radiusMeters,$latitude,$longitude);
            """.trimIndent()
            HealthcareCategory.DOCTOR -> """
                node["amenity"="doctors"](around:$radiusMeters,$latitude,$longitude);
                way["amenity"="doctors"](around:$radiusMeters,$latitude,$longitude);
                node["healthcare"="doctor"](around:$radiusMeters,$latitude,$longitude);
                way["healthcare"="doctor"](around:$radiusMeters,$latitude,$longitude);
            """.trimIndent()
            HealthcareCategory.PHARMACY -> """
                node["amenity"="pharmacy"](around:$radiusMeters,$latitude,$longitude);
                way["amenity"="pharmacy"](around:$radiusMeters,$latitude,$longitude);
            """.trimIndent()
            HealthcareCategory.DIAGNOSTIC_LAB -> """
                node["healthcare"="laboratory"](around:$radiusMeters,$latitude,$longitude);
                way["healthcare"="laboratory"](around:$radiusMeters,$latitude,$longitude);
                node["amenity"="laboratory"](around:$radiusMeters,$latitude,$longitude);
                way["amenity"="laboratory"](around:$radiusMeters,$latitude,$longitude);
            """.trimIndent()
            HealthcareCategory.AMBULANCE_SERVICE -> """
                node["amenity"="ambulance_station"](around:$radiusMeters,$latitude,$longitude);
                way["amenity"="ambulance_station"](around:$radiusMeters,$latitude,$longitude);
                node["emergency"="ambulance_station"](around:$radiusMeters,$latitude,$longitude);
            """.trimIndent()
            HealthcareCategory.BLOOD_BANK -> """
                node["healthcare"~"blood_donation|blood_bank"](around:$radiusMeters,$latitude,$longitude);
                way["healthcare"~"blood_donation|blood_bank"](around:$radiusMeters,$latitude,$longitude);
                node["amenity"="blood_bank"](around:$radiusMeters,$latitude,$longitude);
            """.trimIndent()
            HealthcareCategory.MEDICAL_STORE -> """
                node["healthcare"="pharmacy"](around:$radiusMeters,$latitude,$longitude);
                node["shop"="medical_supply"](around:$radiusMeters,$latitude,$longitude);
                way["shop"="medical_supply"](around:$radiusMeters,$latitude,$longitude);
            """.trimIndent()
        }

        val overpassQuery = """
            [out:json][timeout:20];
            (
                $queryFilter
            );
            out center 60;
        """.trimIndent()

        val endpoint = "https://overpass-api.de/api/interpreter"
        val requestBody = "data=${java.net.URLEncoder.encode(overpassQuery, "UTF-8")}"
            .toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .header("User-Agent", "MediPulseHealthcareAndroid/1.0 (contact: support@medipulse.app)")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Overpass API returned status code ${response.code}")
                return@withContext emptyList()
            }

            val bodyString = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(bodyString)
            val elements = json.optJSONArray("elements") ?: return@withContext emptyList()

            val places = mutableListOf<HealthcarePlace>()
            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                val id = element.optString("id", "loc_$i")
                val tags = element.optJSONObject("tags") ?: continue

                // Check name
                var name = tags.optString("name")
                if (name.isBlank()) {
                    val operator = tags.optString("operator")
                    val amenity = tags.optString("amenity")
                    val healthcare = tags.optString("healthcare")
                    name = when {
                        operator.isNotBlank() -> operator
                        amenity.isNotBlank() -> amenity.replace('_', ' ').replaceFirstChar { it.uppercase() }
                        healthcare.isNotBlank() -> healthcare.replace('_', ' ').replaceFirstChar { it.uppercase() }
                        else -> "Healthcare Facility"
                    }
                }

                // Coordinates: for nodes -> lat/lon, for ways -> center.lat / center.lon
                val lat = if (element.has("lat")) element.optDouble("lat") else element.optJSONObject("center")?.optDouble("lat") ?: continue
                val lon = if (element.has("lon")) element.optDouble("lon") else element.optJSONObject("center")?.optDouble("lon") ?: continue

                val amenityTag = tags.optString("amenity").ifBlank { null }
                val healthcareTag = tags.optString("healthcare").ifBlank { null }
                val emergencyTag = tags.optString("emergency").ifBlank { null }

                val detectedCategory = HealthcareCategory.fromOsmTag(amenityTag, healthcareTag, emergencyTag)

                // Address parsing
                val street = tags.optString("addr:street")
                val houseNumber = tags.optString("addr:housenumber")
                val city = tags.optString("addr:city")
                val postCode = tags.optString("addr:postcode")
                val addressParts = listOfNotNull(
                    if (houseNumber.isNotBlank() && street.isNotBlank()) "$houseNumber $street" else street.ifBlank { null },
                    city.ifBlank { null },
                    postCode.ifBlank { null }
                )
                val address = if (addressParts.isNotEmpty()) addressParts.joinToString(", ") else "Near $lat, $lon"

                // Phone
                val phone = tags.optString("phone").ifBlank {
                    tags.optString("contact:phone").ifBlank { null }
                }

                // Hours
                val openingHours = tags.optString("opening_hours").ifBlank {
                    if (tags.optString("emergency") == "yes") "Open 24/7" else null
                }

                // Services
                val servicesList = mutableListOf<String>()
                if (tags.optString("emergency") == "yes") servicesList.add("Emergency Care (24/7)")
                if (tags.optString("wheelchair") == "yes") servicesList.add("Wheelchair Accessible")
                if (tags.optString("dispensing") == "yes") servicesList.add("Prescription Dispensing")
                if (tags.optString("drive_through") == "yes") servicesList.add("Drive-Through Available")
                val specialty = tags.optString("healthcare:speciality")
                if (specialty.isNotBlank()) servicesList.add(specialty.replace(';', ',').replace('_', ' '))

                val isEmergency = tags.optString("emergency") == "yes" ||
                        detectedCategory == HealthcareCategory.EMERGENCY_DEPT ||
                        detectedCategory == HealthcareCategory.AMBULANCE_SERVICE

                val distanceMeters = calculateHaversineDistance(latitude, longitude, lat, lon)

                places.add(
                    HealthcarePlace(
                        id = id,
                        name = name,
                        category = detectedCategory,
                        address = address,
                        latitude = lat,
                        longitude = lon,
                        distanceMeters = distanceMeters,
                        phoneNumber = phone,
                        openingHours = openingHours,
                        rating = if (detectedCategory == HealthcareCategory.HOSPITAL) 4.8 else 4.6,
                        availableServices = servicesList,
                        isEmergencyAvailable = isEmergency,
                        website = tags.optString("website").ifBlank { tags.optString("contact:website").ifBlank { null } }
                    )
                )
            }

            places.sortedBy { it.distanceMeters }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch from Overpass: ${e.message}")
            emptyList()
        }
    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
