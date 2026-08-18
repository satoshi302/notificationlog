package com.example.notificationlog.ui.selfcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notificationlog.App
import com.example.notificationlog.data.SelfCheckRepository
import com.example.notificationlog.selfcheck.SelfCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SelfCheckViewModel(
    private val repo: SelfCheckRepository,
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

    fun onDraftChange(text: String) {
        _draft.value = text
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
            SelfCheckViewModel(app.selfCheckRepository, conversationKey) as T
    }
}
