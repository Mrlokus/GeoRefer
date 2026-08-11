package co.georefer.app.map

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeoPdfViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeoPdfRepository(application)
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(GeoPdfUiState())
    val uiState: StateFlow<GeoPdfUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        preferences.getString(KEY_URI, null)?.let { savedUri ->
            load(Uri.parse(savedUri), persistAfterSuccess = false)
        }
    }

    fun import(uri: Uri) {
        load(uri, persistAfterSuccess = true)
    }

    private fun load(uri: Uri, persistAfterSuccess: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = GeoPdfUiState(
                status = GeoPdfStatus.LOADING,
                pendingName = uri.lastPathSegment,
            )

            runCatching {
                withContext(Dispatchers.IO) { repository.load(uri) }
            }.onSuccess { map ->
                if (persistAfterSuccess) {
                    preferences.edit().putString(KEY_URI, uri.toString()).apply()
                }
                _uiState.value = GeoPdfUiState(
                    status = GeoPdfStatus.READY,
                    map = map,
                )
            }.onFailure { error ->
                if (!persistAfterSuccess) {
                    preferences.edit().remove(KEY_URI).apply()
                }
                _uiState.value = GeoPdfUiState(
                    status = GeoPdfStatus.ERROR,
                    errorMessage = error.message ?: "No fue posible cargar el GeoPDF.",
                )
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "georefer_maps"
        const val KEY_URI = "active_geopdf_uri"
    }
}
