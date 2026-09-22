package com.example.data.remote

import android.util.Log
import com.example.data.model.RouteInfo
import com.example.data.model.RouteMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class OsrmRoutingService {
    private val TAG = "OsrmRoutingService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        destinationName: String,
        mode: RouteMode
    ): RouteInfo = withContext(Dispatchers.IO) {
        val profile = if (mode == RouteMode.WALKING) "foot" else "car"
        // OSRM coordinates format: {longitude},{latitude};{longitude},{latitude}
        val url = "https://router.project-osrm.org/route/v1/$profile/$startLon,$startLat;$endLon,$endLat?overview=full&geometries=geojson&steps=true"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MediPulseHealthcareAndroid/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val json = JSONObject(body)
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val firstRoute = routes.getJSONObject(0)
                        val distanceMeters = firstRoute.optDouble("distance", 0.0)
                        val durationSeconds = firstRoute.optDouble("duration", 0.0)
                        val distanceKm = (distanceMeters / 1000.0)
                        val durationMinutes = maxOf(1, (durationSeconds / 60.0).roundToInt())

                        val geometry = firstRoute.optJSONObject("geometry")
                        val coordinatesArray = geometry?.optJSONArray("coordinates")
                        val points = mutableListOf<Pair<Double, Double>>()

                        if (coordinatesArray != null) {
                            for (i in 0 until coordinatesArray.length()) {
                                val coord = coordinatesArray.getJSONArray(i)
                                val lon = coord.getDouble(0)
                                val lat = coord.getDouble(1)
                                points.add(Pair(lat, lon))
                            }
                        }

                        // Steps instructions
                        val stepsList = mutableListOf<String>()
                        val legs = firstRoute.optJSONArray("legs")
                        if (legs != null && legs.length() > 0) {
                            val steps = legs.getJSONObject(0).optJSONArray("steps")
                            if (steps != null) {
                                for (s in 0 until minOf(steps.length(), 6)) {
                                    val step = steps.getJSONObject(s)
                                    val maneuver = step.optJSONObject("maneuver")
                                    val instruction = maneuver?.optString("instruction")
                                    val name = step.optString("name")
                                    val text = if (!instruction.isNullOrBlank()) {
                                        instruction
                                    } else if (name.isNotBlank()) {
                                        "Follow $name"
                                    } else {
                                        "Continue on current route"
                                    }
                                    stepsList.add(text)
                                }
                            }
                        }

                        return@withContext RouteInfo(
                            destinationName = destinationName,
                            distanceKm = String.format("%.1f", distanceKm).toDouble(),
                            durationMinutes = durationMinutes,
                            mode = mode,
                            polylinePoints = points,
                            steps = stepsList
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OSRM routing failed: ${e.message}")
        }

        // Fallback straight-line estimation if offline or OSRM rate-limited
        val straightDistanceKm = calculateDirectDistanceKm(startLat, startLon, endLat, endLon)
        val speedKmh = if (mode == RouteMode.WALKING) 4.5 else 40.0
        val durationMins = maxOf(1, ((straightDistanceKm / speedKmh) * 60).roundToInt())

        RouteInfo(
            destinationName = destinationName,
            distanceKm = String.format("%.1f", straightDistanceKm).toDouble(),
            durationMinutes = durationMins,
            mode = mode,
            polylinePoints = listOf(
                Pair(startLat, startLon),
                Pair((startLat + endLat) / 2.0, (startLon + endLon) / 2.0),
                Pair(endLat, endLon)
            ),
            steps = listOf(
                "Head towards $destinationName",
                "Proceed on main arterial route",
                "Arrive at destination"
            )
        )
    }

    private fun calculateDirectDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
