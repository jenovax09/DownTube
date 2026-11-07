package com.snaptube.clone.core.model

import kotlinx.serialization.Serializable

/** Container/codec family for a downloadable format. */
enum class ContainerType { MP4, WEBM, MP3, M4A, THREE_GP, HLS, DASH, OTHER }

/**
 * A single selectable format/quality offered for a media item.
 *
 * The [id] mirrors the tag style used by the app that was analysed
 * (e.g. "MP4_720P", "MP3_160K", "MP4_1080P_VIDEO_399") so stored
 * history/format strings stay human-readable.
 */
@Serializable
data class DownloadFormat(
    val id: String,
    val label: String,
    val container: ContainerType,
    val mediaType: MediaType,
    val url: String,
    val ext: String,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Int = 0,
    val bitrate: Int = 0,
    val codec: String = "",
    val audioCodec: String = "",
    val contentLength: Long = 0L,
    val hasVideo: Boolean = true,
    val hasAudio: Boolean = true,
    val itag: Int = -1,
    val requiresMux: Boolean = false,
    /** Extra HTTP headers required to fetch the stream. */
    val headers: Map<String, String> = emptyMap()
) {
    val resolution: String get() = if (height <= 0) "" else "${height}p"

    val bitrateLabel: String
        get() = when {
            bitrate <= 0 -> ""
            bitrate % 1000 == 0 -> "${bitrate / 1000}K"
            else -> "${bitrate}k"
        }

    val qualityLabel: String
        get() = when (mediaType) {
            MediaType.AUDIO -> bitrateLabel.ifEmpty { label }
            else -> resolution.ifEmpty { label }.let { if (fps >= 50) "$it${fps}FPS" else it }
        }
}
