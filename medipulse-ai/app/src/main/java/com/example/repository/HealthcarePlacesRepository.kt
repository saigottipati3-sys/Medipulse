package com.example.repository

import com.example.data.model.HealthcareCategory
import com.example.data.model.HealthcarePlace

interface HealthcarePlacesRepository {
    suspend fun getNearbyHealthcarePlaces(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        category: HealthcareCategory = HealthcareCategory.ALL,
        forceRefresh: Boolean = false
    ): Result<List<HealthcarePlace>>

    suspend fun searchPlaces(
        query: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): Result<List<HealthcarePlace>>

    suspend fun geocodeAddress(query: String): Pair<Double, Double>?
}
