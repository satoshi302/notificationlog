package com.example.notificationlog.ui.trend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notificationlog.App
import com.example.notificationlog.data.SelfCheckRepository
import com.example.notificationlog.data.TrendReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrendReportViewModel(
    private val repo: SelfCheckRepository
) : ViewModel() {

    private val _report = MutableStateFlow<TrendReport?>(null)
    val report: StateFlow<TrendReport?> = _report.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _report.value = repo.buildTrend()
        }
    }

    class Factory(private val app: App) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TrendReportViewModel(app.selfCheckRepository) as T
    }
}
