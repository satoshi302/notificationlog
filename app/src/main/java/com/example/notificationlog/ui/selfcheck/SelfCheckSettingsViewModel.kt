package com.example.notificationlog.ui.selfcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notificationlog.App
import com.example.notificationlog.data.SelfCheckRepository
import com.example.notificationlog.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SelfCheckSettingsViewModel(
    private val settings: SettingsRepository,
    private val selfCheckRepo: SelfCheckRepository
) : ViewModel() {

    val enabled: StateFlow<Boolean> =
        settings.selfCheckEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val threshold: StateFlow<Int> =
        settings.selfCheckThreshold.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 2)

    val checkCount: StateFlow<Int> =
        selfCheckRepo.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setEnabled(v: Boolean) {
        viewModelScope.launch { settings.setSelfCheckEnabled(v) }
    }

    fun setThreshold(v: Int) {
        viewModelScope.launch { settings.setSelfCheckThreshold(v) }
    }

    fun clearHistory() {
        viewModelScope.launch { selfCheckRepo.clearHistory() }
    }

    class Factory(private val app: App) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SelfCheckSettingsViewModel(app.settingsRepository, app.selfCheckRepository) as T
    }
}
