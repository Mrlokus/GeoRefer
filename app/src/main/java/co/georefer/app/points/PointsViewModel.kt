package co.georefer.app.points

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
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
        val now = System.currentTimeMillis()
        val nextNumber = _uiState.value.points.size + 1
        val point = FieldPoint(
            id = nextAvailableId(now),
            name = name.trim().ifBlank { "Punto $nextNumber" },
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
            if (point.id == id) point.copy(
                name = name.trim().ifBlank { point.name },
                note = note.trim(),
            ) else point
        }
        repository.save(updated)
        _uiState.value = _uiState.value.copy(points = updated, message = "Cambios guardados")
    }

    fun deletePoint(id: Long) {
        val updated = _uiState.value.points.filterNot { it.id == id }
        repository.save(updated)
        _uiState.value = _uiState.value.copy(points = updated)
    }

    fun exportPoints(uri: Uri, format: PointFileFormat) {
        runCatching {
            val content = PointsTransfer.export(_uiState.value.points, format)
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            } ?: error("No fue posible abrir el archivo")
        }.onSuccess {
            _uiState.value = _uiState.value.copy(
                message = "${_uiState.value.points.size} puntos exportados en ${format.label}",
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(message = "No se pudo exportar: ${error.message}")
        }
    }

    fun importPoints(uri: Uri) {
        runCatching {
            val resolver = getApplication<Application>().contentResolver
            val fileName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            val content = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("No fue posible abrir el archivo")
            PointsTransfer.import(content, fileName.orEmpty())
        }.onSuccess { imported ->
            var nextId = System.currentTimeMillis()
            val existingCoordinates = _uiState.value.points
                .map { coordinateKey(it.latitude, it.longitude) }
                .toHashSet()
            val newPoints = imported.mapNotNull { point ->
                if (!point.latitude.isFinite() || !point.longitude.isFinite()) return@mapNotNull null
                if (point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) return@mapNotNull null
                if (!existingCoordinates.add(coordinateKey(point.latitude, point.longitude))) return@mapNotNull null
                while (_uiState.value.points.any { it.id == nextId }) nextId++
                FieldPoint(
                    id = nextId++,
                    name = point.name.ifBlank { "Punto importado" },
                    note = point.note,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    createdAtMillis = System.currentTimeMillis(),
                )
            }
            val updated = (newPoints + _uiState.value.points).sortedByDescending { it.createdAtMillis }
            repository.save(updated)
            _uiState.value = _uiState.value.copy(
                points = updated,
                message = if (newPoints.isEmpty()) {
                    "No se encontraron puntos nuevos"
                } else {
                    "${newPoints.size} puntos importados"
                },
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(message = "No se pudo importar: ${error.message}")
        }
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

    private fun coordinateKey(latitude: Double, longitude: Double): String =
        "%.6f,%.6f".format(java.util.Locale.US, latitude, longitude)
}
