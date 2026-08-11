package co.georefer.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.GnssStatusCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidGpsMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val executor = ContextCompat.getMainExecutor(appContext)

    private val _snapshot = MutableStateFlow(GpsSnapshot())
    val snapshot: StateFlow<GpsSnapshot> = _snapshot.asStateFlow()

    private var started = false

    private val locationListener = object : LocationListenerCompat {
        override fun onLocationChanged(location: Location) {
            if (!location.hasAccuracy()) return
            val fix = GpsFix(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                capturedAtElapsedRealtimeNanos = location.elapsedRealtimeNanos,
            )
            _snapshot.value = _snapshot.value.copy(
                fix = fix,
                providerEnabled = providerIsEnabled(),
                errorMessage = null,
            )
        }

        override fun onProviderEnabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) {
                _snapshot.value = _snapshot.value.copy(providerEnabled = true)
            }
        }

        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) {
                _snapshot.value = _snapshot.value.copy(providerEnabled = false)
            }
        }
    }

    private val gnssCallback = object : GnssStatusCompat.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatusCompat) {
            var used = 0
            for (index in 0 until status.satelliteCount) {
                if (status.usedInFix(index)) used += 1
            }
            _snapshot.value = _snapshot.value.copy(
                satellitesVisible = status.satelliteCount,
                satellitesUsed = used,
            )
        }

        override fun onStopped() {
            _snapshot.value = _snapshot.value.copy(
                satellitesVisible = 0,
                satellitesUsed = 0,
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (started) return
        started = true

        val providerEnabled = providerIsEnabled()
        _snapshot.value = _snapshot.value.copy(
            fix = null,
            providerEnabled = providerEnabled,
            satellitesVisible = 0,
            satellitesUsed = 0,
            errorMessage = null,
        )

        try {
            val request = LocationRequestCompat.Builder(1_000L)
                .setMinUpdateIntervalMillis(500L)
                .setMinUpdateDistanceMeters(0f)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .build()

            LocationManagerCompat.requestLocationUpdates(
                locationManager,
                LocationManager.GPS_PROVIDER,
                request,
                executor,
                locationListener,
            )
            LocationManagerCompat.registerGnssStatusCallback(
                locationManager,
                executor,
                gnssCallback,
            )
        } catch (error: SecurityException) {
            cleanupCallbacks()
            started = false
            _snapshot.value = _snapshot.value.copy(
                fix = null,
                satellitesVisible = 0,
                satellitesUsed = 0,
                errorMessage = "Android no concedió acceso a la ubicación precisa.",
            )
        } catch (error: IllegalArgumentException) {
            cleanupCallbacks()
            started = false
            _snapshot.value = _snapshot.value.copy(
                fix = null,
                satellitesVisible = 0,
                satellitesUsed = 0,
                errorMessage = "El proveedor GPS no está disponible en este dispositivo.",
            )
        }
    }

    fun stop() {
        cleanupCallbacks()
        started = false
        _snapshot.value = _snapshot.value.copy(
            fix = null,
            satellitesVisible = 0,
            satellitesUsed = 0,
            errorMessage = null,
        )
    }

    fun refreshProviderState() {
        _snapshot.value = _snapshot.value.copy(providerEnabled = providerIsEnabled())
    }

    private fun providerIsEnabled(): Boolean = runCatching {
        LocationManagerCompat.isLocationEnabled(locationManager) &&
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }.getOrDefault(false)

    private fun cleanupCallbacks() {
        runCatching {
            LocationManagerCompat.removeUpdates(locationManager, locationListener)
        }
        runCatching {
            LocationManagerCompat.unregisterGnssStatusCallback(locationManager, gnssCallback)
        }
    }
}
