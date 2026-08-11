package co.georefer.app.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, 0)
    private val _northUpLocked = MutableStateFlow(
        preferences.getBoolean(KEY_NORTH_UP_LOCKED, false),
    )
    val northUpLocked: StateFlow<Boolean> = _northUpLocked.asStateFlow()

    fun setNorthUpLocked(locked: Boolean) {
        _northUpLocked.value = locked
        preferences.edit().putBoolean(KEY_NORTH_UP_LOCKED, locked).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "georefer_map_preferences"
        const val KEY_NORTH_UP_LOCKED = "north_up_locked"
    }
}
