package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class GeocodedLocation(
    val name: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

class NominatimGeocodingService {
    private val TAG = "NominatimGeocoding"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun searchLocation(query: String): GeocodedLocation? = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext null
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1&addressdetails=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MediPulseHealthcareAndroid/1.0 (contact: support@medipulse.app)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val jsonArray = JSONArray(body)
            if (jsonArray.length() == 0) return@withContext null

            val item = jsonArray.getJSONObject(0)
            val lat = item.optDouble("lat")
            val lon = item.optDouble("lon")
            val displayName = item.optString("display_name")
            val name = item.optString("name").ifBlank { query }

            GeocodedLocation(
                name = name,
                displayName = displayName,
                latitude = lat,
                longitude = lon
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error geocoding location query '$query': ${e.message}")
            null
        }
    }
}
