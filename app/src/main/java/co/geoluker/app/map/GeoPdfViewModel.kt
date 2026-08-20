package co.geoluker.app.map

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeoPdfViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeoPdfRepository(application)
    private val _uiState = MutableStateFlow(
        GeoPdfUiState(status = GeoPdfStatus.LOADING, pendingName = "Luker Agrícola"),
    )
    val uiState: StateFlow<GeoPdfUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.loadBundled() }
            }.onSuccess { map ->
                _uiState.value = GeoPdfUiState(status = GeoPdfStatus.READY, map = map)
            }.onFailure { error ->
                Log.e(LOG_TAG, "No fue posible cargar el mapa oficial", error)
                _uiState.value = GeoPdfUiState(
                    status = GeoPdfStatus.ERROR,
                    errorMessage = error.message ?: "No fue posible cargar el mapa oficial.",
                )
            }
        }
    }

    override fun onCleared() {
        _uiState.value.map?.tileRenderer?.close()
        super.onCleared()
    }

    private companion object {
        const val LOG_TAG = "GeoLukerMap"
    }
}
