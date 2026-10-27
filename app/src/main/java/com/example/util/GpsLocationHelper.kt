package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object GpsLocationHelper {

    private const val TAG = "GpsLocationHelper"

    data class LocationResult(
        val cityName: String,
        val latitude: Double,
        val longitude: Double,
        val isSuccess: Boolean
    )

    @SuppressLint("MissingPermission")
    suspend fun getDeviceLocation(
        context: Context,
        fallbackCity: String = "İstanbul",
        fallbackLat: Double = 41.0082,
        fallbackLng: Double = 28.9784
    ): LocationResult = withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                return@withContext LocationResult(fallbackCity, fallbackLat, fallbackLng, false)
            }

            var bestLocation: Location? = null

            // 1. Check last known locations
            try {
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )
                for (provider in providers) {
                    if (locationManager.isProviderEnabled(provider)) {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null) {
                            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                                bestLocation = loc
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Last known location check: ${e.message}")
            }

            // 2. If last known is null, try active one-shot location request with timeout
            if (bestLocation == null) {
                try {
                    bestLocation = kotlinx.coroutines.withTimeoutOrNull(3500L) {
                        kotlinx.coroutines.suspendCancellableCoroutine<Location?> { cont ->
                            val listener = object : android.location.LocationListener {
                                override fun onLocationChanged(location: Location) {
                                    try {
                                        locationManager.removeUpdates(this)
                                    } catch (_: Exception) {}
                                    if (cont.isActive) cont.resume(location) {}
                                }
                                @Deprecated("Deprecated in Java")
                                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                                override fun onProviderEnabled(provider: String) {}
                                override fun onProviderDisabled(provider: String) {}
                            }

                            val handler = android.os.Handler(android.os.Looper.getMainLooper())
                            handler.post {
                                try {
                                    var requested = false
                                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, android.os.Looper.getMainLooper())
                                        requested = true
                                    }
                                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, android.os.Looper.getMainLooper())
                                        requested = true
                                    }
                                    if (!requested && cont.isActive) {
                                        cont.resume(null) {}
                                    }
                                } catch (e: Exception) {
                                    if (cont.isActive) cont.resume(null) {}
                                }
                            }

                            cont.invokeOnCancellation {
                                handler.post {
                                    try {
                                        locationManager.removeUpdates(listener)
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Active GPS fix error: ${e.message}")
                }
            }

            if (bestLocation == null) {
                Log.w(TAG, "GPS konumu tespit edilemedi, mevcut ayarlar korunuyor ($fallbackCity).")
                return@withContext LocationResult(fallbackCity, fallbackLat, fallbackLng, false)
            }

            val lat = bestLocation.latitude
            val lng = bestLocation.longitude

            // Reverse geocode to find real city name
            var resolvedCity = fallbackCity.ifBlank { "Mevcut Konum" }
            try {
                val geocoder = Geocoder(context, Locale("tr", "TR"))
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val address = addresses?.firstOrNull()
                if (address != null) {
                    resolvedCity = address.adminArea
                        ?: address.subAdminArea
                        ?: address.locality
                        ?: address.subLocality
                        ?: fallbackCity
                }
            } catch (e: Exception) {
                Log.w(TAG, "Geocoder error: ${e.message}")
                if (resolvedCity.isBlank() || resolvedCity == "İstanbul") {
                    resolvedCity = "GPS Konumu"
                }
            }

            return@withContext LocationResult(
                cityName = resolvedCity,
                latitude = lat,
                longitude = lng,
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining device location: ${e.message}")
            return@withContext LocationResult(fallbackCity, fallbackLat, fallbackLng, false)
        }
    }
}
