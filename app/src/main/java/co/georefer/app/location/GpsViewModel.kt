package co.georefer.app.location

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class GpsViewModel(application: Application) : AndroidViewModel(application) {
    private val monitor = AndroidGpsMonitor(application)
    private val permission = MutableStateFlow(LocationPermissionLevel.NONE)
    private val stabilizer = GpsQualityStabilizer()
    private var lastStabilizedFixTimestamp: Long? = null
    private var stabilizedQuality = GpsQuality.SEARCHING

    private val ticker = flow {
        while (true) {
            emit(SystemClock.elapsedRealtimeNanos())
            delay(1_000L)
        }
    }

    val uiState: StateFlow<GpsUiState> = combine(
        permission,
        monitor.snapshot,
        ticker,
    ) { permissionLevel, snapshot, nowNanos ->
        buildUiState(permissionLevel, snapshot, nowNanos)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = GpsUiState(),
    )

    fun onForeground(permissionLevel: LocationPermissionLevel) {
        permission.value = permissionLevel
        monitor.refreshProviderState()
        if (permissionLevel == LocationPermissionLevel.PRECISE) {
            monitor.start()
        } else {
            monitor.stop()
            stabilizer.reset()
            stabilizedQuality = GpsQuality.SEARCHING
            lastStabilizedFixTimestamp = null
        }
    }

    fun onBackground() {
        monitor.stop()
    }

    private fun buildUiState(
        permissionLevel: LocationPermissionLevel,
        snapshot: GpsSnapshot,
        nowNanos: Long,
    ): GpsUiState {
        val availability = when {
            permissionLevel == LocationPermissionLevel.NONE -> GpsAvailability.NEEDS_PERMISSION
            permissionLevel == LocationPermissionLevel.APPROXIMATE -> GpsAvailability.APPROXIMATE_ONLY
            snapshot.errorMessage != null -> GpsAvailability.ERROR
            !snapshot.providerEnabled -> GpsAvailability.LOCATION_DISABLED
            else -> GpsAvailability.ACTIVE
        }

        val rawQuality = if (availability == GpsAvailability.ACTIVE) {
            GpsQualityClassifier.classify(snapshot.fix, nowNanos)
        } else {
            GpsQuality.SEARCHING
        }

        if (rawQuality == GpsQuality.SEARCHING) {
            stabilizer.reset()
            stabilizedQuality = GpsQuality.SEARCHING
            lastStabilizedFixTimestamp = null
        } else if (snapshot.fix?.capturedAtElapsedRealtimeNanos != lastStabilizedFixTimestamp) {
            stabilizedQuality = stabilizer.update(rawQuality)
            lastStabilizedFixTimestamp = snapshot.fix?.capturedAtElapsedRealtimeNanos
        }

        val ageSeconds = snapshot.fix?.let {
            ((nowNanos - it.capturedAtElapsedRealtimeNanos).coerceAtLeast(0L) / 1_000_000_000L)
        }

        return GpsUiState(
            availability = availability,
            quality = if (availability == GpsAvailability.ACTIVE) stabilizedQuality else GpsQuality.SEARCHING,
            fix = snapshot.fix,
            satellitesVisible = snapshot.satellitesVisible,
            satellitesUsed = snapshot.satellitesUsed,
            readingAgeSeconds = ageSeconds,
            errorMessage = snapshot.errorMessage,
        )
    }

    override fun onCleared() {
        monitor.stop()
        super.onCleared()
    }
}
