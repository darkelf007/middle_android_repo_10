package ru.yandex.buggyweatherapp.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import ru.yandex.buggyweatherapp.model.Location
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ILocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<Location> =
        suspendCancellableCoroutine { continuation ->

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                continuation.resume(Result.failure(SecurityException("Location permission not granted.")))
                return@suspendCancellableCoroutine
            }


            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null && continuation.isActive) {
                        val userLocation =
                            Location(latitude = location.latitude, longitude = location.longitude)
                        continuation.resume(Result.success(userLocation))
                    } else {
                        // Если нет последнего местоположения, запрашиваем новое
                        requestFreshLocation(continuation)
                    }
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(e))
                    }
                }
        }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(continuation: kotlinx.coroutines.CancellableContinuation<Result<Location>>) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (continuation.isActive) {
                        val userLocation =
                            Location(latitude = location.latitude, longitude = location.longitude)
                        continuation.resume(Result.success(userLocation))
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            }
        }


        continuation.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override suspend fun getCityNameFromLocation(location: Location): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                val cityName = if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    address.locality ?: address.subAdminArea ?: address.adminArea
                } else {
                    null
                }
                Result.success(cityName)
            } catch (e: Exception) {
                Log.e("LocationRepository", "Error getting city name", e)
                Result.failure(e)
            }
        }
    }
}