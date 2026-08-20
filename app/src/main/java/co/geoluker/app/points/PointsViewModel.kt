package co.geoluker.app.points

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PointsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PointsRepository(application)
    private val _uiState = MutableStateFlow(
        PointsUiState(points = repository.load().sortedByDescending { it.createdAtMillis }),
    )
    val uiState: StateFlow<PointsUiState> = _uiState.asStateFlow()

    fun beginPoint(latitude: Double, longitude: Double) {
        if (!latitude.isFinite() || !longitude.isFinite()) return
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return
        _uiState.value = _uiState.value.copy(
            pendingPoint = PointCandidate(latitude = latitude, longitude = longitude),
        )
    }

    fun cancelPendingPoint() {
        _uiState.value = _uiState.value.copy(pendingPoint = null)
    }

    fun savePendingPoint(name: String, note: String) {
        val candidate = _uiState.value.pendingPoint ?: return
        val duplicate = _uiState.value.points.firstOrNull { point ->
            distanceBetweenCoordinates(
                candidate.latitude,
                candidate.longitude,
                point.latitude,
                point.longitude,
            ) <= DUPLICATE_RADIUS_METERS
        }
        if (duplicate != null) {
            _uiState.value = _uiState.value.copy(
                pendingPoint = null,
                message = "Ya existe ${duplicate.name} en este lugar",
            )
            return
        }
        val now = System.currentTimeMillis()
        val point = FieldPoint(
            id = nextAvailableId(now),
            name = name.trim().ifBlank { "Punto ${_uiState.value.points.size + 1}" },
            note = note.trim(),
            latitude = candidate.latitude,
            longitude = candidate.longitude,
            createdAtMillis = now,
        )
        val updated = listOf(point) + _uiState.value.points
        repository.save(updated)
        _uiState.value = PointsUiState(points = updated, message = "Punto guardado")
    }

    fun updatePoint(id: Long, name: String, note: String) {
        val updated = _uiState.value.points.map { point ->
            if (point.id == id) {
                point.copy(
                    name = name.trim().ifBlank { point.name },
                    note = note.trim(),
                )
            } else {
                point
            }
        }
        repository.save(updated)
        _uiState.value = _uiState.value.copy(points = updated, message = "Cambios guardados")
    }

    fun deletePoint(id: Long) {
        val updated = _uiState.value.points.filterNot { it.id == id }
        repository.save(updated)
        _uiState.value = _uiState.value.copy(points = updated, message = "Punto eliminado")
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun nextAvailableId(initial: Long): Long {
        val existing = _uiState.value.points.asSequence().map { it.id }.toHashSet()
        var candidate = initial
        while (candidate in existing) candidate += 1
        return candidate
    }

    private companion object {
        const val DUPLICATE_RADIUS_METERS = 1.5
    }
}
