package com.example.repository

import com.example.data.model.RouteInfo
import com.example.data.model.RouteMode
import com.example.data.remote.OsrmRoutingService

interface DirectionsRepository {
    suspend fun calculateRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        destinationName: String,
        mode: RouteMode = RouteMode.DRIVING
    ): RouteInfo
}

class DirectionsRepositoryImpl(
    private val osrmRoutingService: OsrmRoutingService = OsrmRoutingService()
) : DirectionsRepository {
    override suspend fun calculateRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        destinationName: String,
        mode: RouteMode
    ): RouteInfo {
        return osrmRoutingService.getRoute(
            startLat = startLat,
            startLon = startLon,
            endLat = endLat,
            endLon = endLon,
            destinationName = destinationName,
            mode = mode
        )
    }
}
