package co.geoluker.app.location

import kotlin.math.abs

object GpsQualityClassifier {
    const val STALE_AFTER_NANOS: Long = 10_000_000_000L
    private const val GOOD_MAX_METERS = 5f
    private const val ACCEPTABLE_MAX_METERS = 15f

    fun classify(
        fix: GpsFix?,
        nowElapsedRealtimeNanos: Long,
    ): GpsQuality {
        if (fix == null || !fix.isValid()) return GpsQuality.SEARCHING

        val ageNanos = nowElapsedRealtimeNanos - fix.capturedAtElapsedRealtimeNanos
        if (ageNanos < 0L || ageNanos > STALE_AFTER_NANOS) {
            return GpsQuality.SEARCHING
        }

        return when {
            fix.accuracyMeters <= GOOD_MAX_METERS -> GpsQuality.GOOD
            fix.accuracyMeters <= ACCEPTABLE_MAX_METERS -> GpsQuality.ACCEPTABLE
            else -> GpsQuality.LOW
        }
    }

    private fun GpsFix.isValid(): Boolean =
        latitude.isFinite() &&
            longitude.isFinite() &&
            abs(latitude) <= 90.0 &&
            abs(longitude) <= 180.0 &&
            accuracyMeters.isFinite() &&
            accuracyMeters > 0f
}

class GpsQualityStabilizer(
    private val readingsToImprove: Int = 2,
) {
    private var current = GpsQuality.SEARCHING
    private var pendingImprovement: GpsQuality? = null
    private var pendingCount = 0

    fun update(raw: GpsQuality): GpsQuality {
        // Una primera lectura GPS válida debe mostrarse inmediatamente. Esperar dos
        // lecturas de la misma categoría podía dejar la UI en "Buscando señal"
        // cuando la precisión alternaba entre categorías aunque ya hubiera un fix.
        if (current == GpsQuality.SEARCHING && raw != GpsQuality.SEARCHING) {
            current = raw
            pendingImprovement = null
            pendingCount = 0
            return current
        }

        if (raw.rank <= current.rank) {
            current = raw
            pendingImprovement = null
            pendingCount = 0
            return current
        }

        if (pendingImprovement == raw) {
            pendingCount += 1
        } else {
            pendingImprovement = raw
            pendingCount = 1
        }

        if (pendingCount >= readingsToImprove) {
            current = raw
            pendingImprovement = null
            pendingCount = 0
        }
        return current
    }

    fun reset() {
        current = GpsQuality.SEARCHING
        pendingImprovement = null
        pendingCount = 0
    }
}
