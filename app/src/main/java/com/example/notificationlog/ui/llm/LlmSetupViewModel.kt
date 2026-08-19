package com.example.notificationlog.ui.llm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notificationlog.App
import com.example.notificationlog.data.prefs.SettingsRepository
import com.example.notificationlog.llm.LlmAdvisor
import com.example.notificationlog.llm.LlmModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LlmSetupViewModel(
    private val modelManager: LlmModelManager,
    private val settings: SettingsRepository,
    private val advisor: LlmAdvisor
) : ViewModel() {

    data class UiState(
        val downloaded: Boolean = false,
        val sizeBytes: Long = 0,
        val downloading: Boolean = false,
        val downloadedBytes: Long = 0,
        val totalBytes: Long = -1,
        val error: String? = null,
        val url: String = "",
        val token: String = ""
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                downloaded = modelManager.isDownloaded(),
                sizeBytes = modelManager.modelSizeBytes(),
                url = settings.currentLlmModelUrl(),
                token = settings.currentHfToken()
            )
        }
    }

    fun setUrl(v: String) { _state.value = _state.value.copy(url = v) }
    fun setToken(v: String) { _state.value = _state.value.copy(token = v) }

    fun download() {
        val s = _state.value
        if (s.downloading) return
        _state.value = s.copy(downloading = true, error = null, downloadedBytes = 0, totalBytes = -1)
        viewModelScope.launch {
            settings.setLlmModelUrl(s.url)
            settings.setHfToken(s.token)
            var lastEmit = 0L
            val result = modelManager.download(s.url, s.token.ifBlank { null }) { d, t ->
                if (d - lastEmit > 1_000_000L || d == t) {
                    lastEmit = d
                    _state.value = _state.value.copy(downloadedBytes = d, totalBytes = t)
                }
            }
            _state.value = _state.value.copy(
                downloading = false,
                downloaded = modelManager.isDownloaded(),
                sizeBytes = modelManager.modelSizeBytes(),
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun delete() {
        advisor.release()
        modelManager.delete()
        _state.value = _state.value.copy(downloaded = false, sizeBytes = 0)
    }

    class Factory(private val app: App) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LlmSetupViewModel(app.llmModelManager, app.settingsRepository, app.llmAdvisor) as T
    }
}
