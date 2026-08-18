package com.example.notificationlog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notificationlog.App
import com.example.notificationlog.data.InstalledApp
import com.example.notificationlog.data.InstalledAppsRepository
import com.example.notificationlog.data.MessageRepository
import com.example.notificationlog.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val targetPackages: StateFlow<Set<String>> =
        settingsRepository.targetPackages
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            _installedApps.value = installedAppsRepository.loadLaunchableApps()
            _loading.value = false
        }
    }

    fun setEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.togglePackage(packageName, enabled)
        }
    }

    fun clearHistory() {
        viewModelScope.launch { messageRepository.clearAll() }
    }

    class Factory(private val app: App) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(
                app.settingsRepository,
                app.installedAppsRepository,
                app.messageRepository
            ) as T
    }
}
