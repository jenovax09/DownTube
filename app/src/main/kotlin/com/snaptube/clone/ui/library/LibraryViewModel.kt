package com.snaptube.clone.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptube.clone.AppContainer
import com.snaptube.clone.core.database.entity.DeleteRecordEntity
import com.snaptube.clone.core.database.entity.HistoryEntity
import com.snaptube.clone.core.download.DownloadProgress
import com.snaptube.clone.core.model.DownloadTask
import com.snaptube.clone.core.model.LocalMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryTab(val label: String) { DOWNLOADS, HISTORY, VIDEOS, AUDIO }

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.DOWNLOADS,
    val localVideos: List<LocalMedia> = emptyList(),
    val localAudio: List<LocalMedia> = emptyList(),
    val isLoadingLocal: Boolean = false
)

class LibraryViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    val activeTasks: StateFlow<List<DownloadTask>> =
        container.downloadRepository.activeTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedTasks: StateFlow<List<DownloadTask>> =
        container.downloadRepository.completedTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progress: StateFlow<Map<Long, DownloadProgress>> = container.downloadRepository.progress

    val history: StateFlow<List<HistoryEntity>> =
        container.database.historyDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trash: StateFlow<List<DeleteRecordEntity>> =
        container.database.trashDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectTab(tab: LibraryTab) {
        _state.value = _state.value.copy(tab = tab)
        if (tab == LibraryTab.VIDEOS || tab == LibraryTab.AUDIO) refreshLocal(tab)
    }

    fun refreshLocal(tab: LibraryTab = _state.value.tab) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingLocal = true)
            val (videos, audio) = withContext(Dispatchers.IO) {
                container.mediaLibraryRepository.videos() to container.mediaLibraryRepository.audio()
            }
            _state.value = _state.value.copy(localVideos = videos, localAudio = audio, isLoadingLocal = false)
        }
    }

    fun pause(id: Long) = viewModelScope.launch { container.downloadRepository.pause(id) }
    fun resume(id: Long) = viewModelScope.launch { container.downloadRepository.resume(id) }
    fun cancel(id: Long) = viewModelScope.launch { container.downloadRepository.cancel(id) }
    fun retry(id: Long) = viewModelScope.launch { container.downloadRepository.retry(id) }
    fun delete(id: Long) = viewModelScope.launch { container.downloadRepository.delete(id) }

    fun clearHistory() = viewModelScope.launch { container.database.historyDao().clear() }
    fun emptyTrash() = viewModelScope.launch { container.database.trashDao().clear() }
}
