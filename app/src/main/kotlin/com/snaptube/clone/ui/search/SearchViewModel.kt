package com.snaptube.clone.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptube.clone.AppContainer
import com.snaptube.clone.core.common.AppResult
import com.snaptube.clone.core.extractor.SearchableExtractor
import com.snaptube.clone.core.model.MediaInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchSource(val label: String) {
    YOUTUBE("YouTube"),
    ALL("Web")
}

data class SearchUiState(
    val query: String = "",
    val source: SearchSource = SearchSource.YOUTUBE,
    val isLoading: Boolean = false,
    val results: List<MediaInfo> = emptyList(),
    val error: String? = null
)

class SearchViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val history = container.database.searchHistoryDao().observeAll()

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun onSourceChange(source: SearchSource) = _state.update { it.copy(source = source) }

    fun search() {
        val query = _state.value.query.trim()
        if (query.isEmpty()) return
        viewModelScope.launch {
            container.database.searchHistoryDao().insert(
                com.snaptube.clone.core.database.entity.SearchHistoryEntity(query, System.currentTimeMillis())
            )
        }
        _state.update { it.copy(isLoading = true, error = null, results = emptyList()) }
        viewModelScope.launch {
            val extractor = searchableFor(_state.value.source)
            if (extractor == null) {
                _state.update { it.copy(isLoading = false, error = "No search backend for this source") }
                return@launch
            }
            when (val result = extractor.search(query)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, results = result.data.items)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { container.database.searchHistoryDao().clear() }
    }

    private fun searchableFor(source: SearchSource): SearchableExtractor? = when (source) {
        SearchSource.YOUTUBE -> container.extractorRegistry.searchable().firstOrNull { it is SearchableExtractor && extractorName(it) == "youtube" }
        SearchSource.ALL -> container.extractorRegistry.searchable().firstOrNull()
    }

    private fun extractorName(extractor: Any): String =
        (extractor as? com.snaptube.clone.core.extractor.MediaExtractor)?.name ?: ""
}
