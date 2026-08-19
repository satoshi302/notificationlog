package com.example.notificationlog.ui.selfcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notificationlog.App
import com.example.notificationlog.data.SelfCheckRepository
import com.example.notificationlog.llm.LlmAdvisor
import com.example.notificationlog.selfcheck.SelfCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** AIによる詳細分析の状態。 */
sealed interface AiState {
    data object Idle : AiState
    data object NeedModel : AiState
    data object Generating : AiState
    data class Result(val text: String) : AiState
    data class Error(val message: String) : AiState
}

class SelfCheckViewModel(
    private val repo: SelfCheckRepository,
    private val llmAdvisor: LlmAdvisor,
    private val conversationKey: String
) : ViewModel() {

    private val packageName: String =
        if (conversationKey.contains("|")) conversationKey.substringBefore("|") else ""

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _result = MutableStateFlow<SelfCheckResult?>(null)
    val result: StateFlow<SelfCheckResult?> = _result.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    fun onDraftChange(text: String) {
        _draft.value = text
        // 文が変わったらAI結果はリセット
        if (_aiState.value !is AiState.Generating) _aiState.value = AiState.Idle
    }

    /** オンデバイスLLMで詳しく分析。モデル未DLなら NeedModel を返す。 */
    fun runAiAnalysis() {
        val text = _draft.value.trim()
        if (text.isEmpty() || _aiState.value is AiState.Generating) return
        if (!llmAdvisor.isReady()) {
            _aiState.value = AiState.NeedModel
            return
        }
        _aiState.value = AiState.Generating
        viewModelScope.launch {
            val context = repo.recentContext(conversationKey)
            val result = runCatching { llmAdvisor.advise(text, context) }
            _aiState.value = result.fold(
                onSuccess = { AiState.Result(it) },
                onFailure = { AiState.Error(it.message ?: "分析に失敗しました。") }
            )
        }
    }

    fun check() {
        val text = _draft.value.trim()
        if (text.isEmpty() || _loading.value) return
        _loading.value = true
        viewModelScope.launch {
            val context = repo.recentContext(conversationKey)
            val res = repo.analyze(text, context)
            repo.record(conversationKey, packageName, res)
            _result.value = res
            _loading.value = false
        }
    }

    class Factory(
        private val app: App,
        private val conversationKey: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SelfCheckViewModel(app.selfCheckRepository, app.llmAdvisor, conversationKey) as T
    }
}
