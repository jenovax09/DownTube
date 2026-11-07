package com.snaptube.clone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptube.clone.AppContainer
import com.snaptube.clone.core.extractor.UrlMatcher
import com.snaptube.clone.core.model.DownloadTask
import com.snaptube.clone.core.model.SearchHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val clipboardLink: String? = null,
    val detectedSite: String? = null,
    val activeDownloads: Int = 0
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val recentHistory: StateFlow<List<com.snaptube.clone.core.database.entity.HistoryEntity>> =
        container.database.historyDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeTasks: StateFlow<List<DownloadTask>> =
        container.downloadRepository.activeTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onLinkDetected(raw: String?) {
        val normalized = raw?.let { UrlMatcher.normalize(it) }
        val site = normalized?.let { UrlMatcher.snapshot(it).name.lowercase().replace('_', ' ') }
        _state.value = _state.value.copy(clipboardLink = normalized, detectedSite = site)
    }

    fun consumeLink() {
        _state.value = _state.value.copy(clipboardLink = null, detectedSite = null)
    }

    fun recordSearch(query: String) {
        viewModelScope.launch {
            container.database.searchHistoryDao().insert(
                com.snaptube.clone.core.database.entity.SearchHistoryEntity(query.trim(), System.currentTimeMillis())
            )
        }
    }
}
