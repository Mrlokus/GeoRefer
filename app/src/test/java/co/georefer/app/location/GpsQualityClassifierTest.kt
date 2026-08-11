package co.georefer.app.location

import org.junit.Assert.assertEquals
import org.junit.Test

class GpsQualityClassifierTest {
    private val now = 20_000_000_000L

    @Test
    fun `sin lectura permanece buscando`() {
        assertEquals(
            GpsQuality.SEARCHING,
            GpsQualityClassifier.classify(null, now),
        )
    }

    @Test
    fun `lectura mayor a diez segundos se considera vencida`() {
        val fix = fix(accuracy = 3f, ageNanos = 10_001_000_000L)
        assertEquals(GpsQuality.SEARCHING, GpsQualityClassifier.classify(fix, now))
    }

    @Test
    fun `cinco metros es calidad buena`() {
        assertEquals(GpsQuality.GOOD, GpsQualityClassifier.classify(fix(5f), now))
    }

    @Test
    fun `mas de cinco metros es calidad aceptable`() {
        assertEquals(GpsQuality.ACCEPTABLE, GpsQualityClassifier.classify(fix(5.01f), now))
    }

    @Test
    fun `quince metros es calidad aceptable`() {
        assertEquals(GpsQuality.ACCEPTABLE, GpsQualityClassifier.classify(fix(15f), now))
    }

    @Test
    fun `mas de quince metros es calidad baja`() {
        assertEquals(GpsQuality.LOW, GpsQualityClassifier.classify(fix(15.01f), now))
    }

    @Test
    fun `precision invalida permanece buscando`() {
        assertEquals(
            GpsQuality.SEARCHING,
            GpsQualityClassifier.classify(fix(Float.NaN), now),
        )
        assertEquals(
            GpsQuality.SEARCHING,
            GpsQualityClassifier.classify(fix(0f), now),
        )
    }

    @Test
    fun `primera lectura valida se muestra inmediatamente`() {
        val stabilizer = GpsQualityStabilizer(readingsToImprove = 2)

        assertEquals(GpsQuality.GOOD, stabilizer.update(GpsQuality.GOOD))
    }

    @Test
    fun `mejora posterior exige dos lecturas y degradacion es inmediata`() {
        val stabilizer = GpsQualityStabilizer(readingsToImprove = 2)

        assertEquals(GpsQuality.LOW, stabilizer.update(GpsQuality.LOW))
        assertEquals(GpsQuality.LOW, stabilizer.update(GpsQuality.GOOD))
        assertEquals(GpsQuality.GOOD, stabilizer.update(GpsQuality.GOOD))
        assertEquals(GpsQuality.LOW, stabilizer.update(GpsQuality.LOW))
    }

    private fun fix(
        accuracy: Float,
        ageNanos: Long = 1_000_000_000L,
    ) = GpsFix(
        latitude = 4.578214,
        longitude = -72.836150,
        accuracyMeters = accuracy,
        capturedAtElapsedRealtimeNanos = now - ageNanos,
    )
}
