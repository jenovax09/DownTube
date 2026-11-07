package com.snaptube.clone.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptube.clone.AppContainer
import com.snaptube.clone.core.common.AppResult
import com.snaptube.clone.core.extractor.UrlMatcher
import com.snaptube.clone.core.model.ContainerType
import com.snaptube.clone.core.model.DownloadFormat
import com.snaptube.clone.core.model.DownloadTask
import com.snaptube.clone.core.model.MediaInfo
import com.snaptube.clone.core.model.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormatUiState(
    val isLoading: Boolean = false,
    val mediaInfo: MediaInfo? = null,
    val error: String? = null,
    val selection: DownloadFormat? = null,
    val downloadedTaskId: Long? = null
) {
    val videoFormats: List<DownloadFormat> get() = mediaInfo?.formats?.filter { it.mediaType == MediaType.VIDEO } ?: emptyList()
    val audioFormats: List<DownloadFormat> get() = mediaInfo?.formats?.filter { it.mediaType == MediaType.AUDIO } ?: emptyList()
}

class ChooseFormatViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(FormatUiState())
    val state: StateFlow<FormatUiState> = _state.asStateFlow()

    fun load(url: String) {
        if (_state.value.mediaInfo != null && _state.value.isLoading.not()) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = container.extractorRegistry.extract(url)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, mediaInfo = result.data, error = null)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun select(format: DownloadFormat) {
        _state.update { it.copy(selection = format) }
    }

    /**
     * Queue the selected format. Audio-only formats download directly; a
     * video-only stream plus the best matching audio stream is muxed later by
     * the download engine.
     */
    fun download() {
        val state = _state.value
        val info = state.mediaInfo ?: return
        val format = state.selection ?: bestDefault(state) ?: return

        viewModelScope.launch {
            val audioUrl = if (format.mediaType == MediaType.VIDEO && !format.hasAudio) {
                state.audioFormats.maxByOrNull { it.bitrate }?.url
            } else {
                null
            }
            val task = DownloadTask(
                mediaId = info.id.ifEmpty { info.webpageUrl },
                title = info.title.ifEmpty { "media" },
                formatId = format.id,
                mediaType = format.mediaType,
                source = info.source,
                videoUrl = format.url,
                audioUrl = audioUrl,
                headers = format.headers,
                referrer = info.webpageUrl,
                coverUrl = info.thumbnailUrl,
                totalBytes = format.contentLength,
                durationMs = info.durationMs,
                status = com.snaptube.clone.core.model.DownloadStatus.QUEUED
            )
            val id = container.downloadRepository.enqueue(task)
            _state.update { it.copy(downloadedTaskId = id) }
        }
    }

    private fun bestDefault(state: FormatUiState): DownloadFormat? =
        state.videoFormats.filter { it.height in 720..1080 }.minByOrNull { kotlin.math.abs(it.height - 720) }
            ?: state.videoFormats.maxByOrNull { it.height }
            ?: state.audioFormats.maxByOrNull { it.bitrate }
}
