package co.georefer.app.orientation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class HeadingViewModel(application: Application) : AndroidViewModel(application) {
    private val monitor = AndroidHeadingMonitor(application)
    val uiState: StateFlow<HeadingUiState> = monitor.state

    fun start() = monitor.start()

    fun stop() = monitor.stop()

    fun updateLocation(latitude: Double, longitude: Double) {
        monitor.updateLocation(latitude, longitude)
    }

    override fun onCleared() {
        monitor.stop()
        super.onCleared()
    }
}
