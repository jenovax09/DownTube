package com.snaptube.clone.core.extractor.youtube

import com.snaptube.clone.core.model.ContainerType
import com.snaptube.clone.core.model.MediaType

/**
 * Static mapping of YouTube itag -> container/codec/quality metadata.
 *
 * Itags are public, documented values. Unknown itags are still handled by the
 * extractor using the stream metadata returned in the player response.
 */
object ItagTable {

    data class Info(
        val container: ContainerType,
        val mediaType: MediaType,
        val width: Int,
        val height: Int,
        val fps: Int,
        val bitrate: Int,
        val videoCodec: String,
        val audioCodec: String
    )

    private val table: Map<Int, Info> = mapOf(
        // Progressive (muxed video+audio)
        5 to Info(ContainerType.MP4, MediaType.VIDEO, 320, 240, 30, 0, "h263", "aac"),
        6 to Info(ContainerType.MP4, MediaType.VIDEO, 320, 240, 30, 0, "h263", "aac"),
        17 to Info(ContainerType.MP4, MediaType.VIDEO, 176, 144, 15, 24, "mp4v", "aac"),
        18 to Info(ContainerType.MP4, MediaType.VIDEO, 640, 360, 30, 96, "avc1", "aac"),
        22 to Info(ContainerType.MP4, MediaType.VIDEO, 1280, 720, 30, 192, "avc1", "aac"),
        34 to Info(ContainerType.MP4, MediaType.VIDEO, 640, 360, 30, 128, "avc1", "aac"),
        35 to Info(ContainerType.MP4, MediaType.VIDEO, 854, 480, 30, 0, "avc1", "aac"),
        36 to Info(ContainerType.MP4, MediaType.VIDEO, 320, 240, 30, 0, "avc1", "aac"),
        37 to Info(ContainerType.MP4, MediaType.VIDEO, 1920, 1080, 30, 0, "avc1", "aac"),
        38 to Info(ContainerType.MP4, MediaType.VIDEO, 4096, 3072, 30, 0, "avc1", "aac"),
        43 to Info(ContainerType.WEBM, MediaType.VIDEO, 640, 360, 30, 128, "vp8", "vorbis"),
        44 to Info(ContainerType.WEBM, MediaType.VIDEO, 854, 480, 30, 128, "vp8", "vorbis"),
        45 to Info(ContainerType.WEBM, MediaType.VIDEO, 1280, 720, 30, 128, "vp8", "vorbis"),
        46 to Info(ContainerType.WEBM, MediaType.VIDEO, 1920, 1080, 30, 128, "vp8", "vorbis"),
        59 to Info(ContainerType.MP4, MediaType.VIDEO, 854, 480, 30, 128, "avc1", "aac"),
        78 to Info(ContainerType.MP4, MediaType.VIDEO, 854, 480, 30, 128, "avc1", "aac"),
        // DASH video-only
        133 to Info(ContainerType.MP4, MediaType.VIDEO, 426, 240, 30, 0, "avc1", ""),
        134 to Info(ContainerType.MP4, MediaType.VIDEO, 640, 360, 30, 0, "avc1", ""),
        135 to Info(ContainerType.MP4, MediaType.VIDEO, 854, 480, 30, 0, "avc1", ""),
        136 to Info(ContainerType.MP4, MediaType.VIDEO, 1280, 720, 30, 0, "avc1", ""),
        137 to Info(ContainerType.MP4, MediaType.VIDEO, 1920, 1080, 30, 0, "avc1", ""),
        138 to Info(ContainerType.MP4, MediaType.VIDEO, 3840, 2160, 30, 0, "avc1", ""),
        160 to Info(ContainerType.MP4, MediaType.VIDEO, 256, 144, 30, 0, "avc1", ""),
        264 to Info(ContainerType.MP4, MediaType.VIDEO, 2560, 1440, 30, 0, "avc1", ""),
        266 to Info(ContainerType.MP4, MediaType.VIDEO, 3840, 2160, 30, 0, "avc1", ""),
        298 to Info(ContainerType.MP4, MediaType.VIDEO, 1280, 720, 60, 0, "avc1", ""),
        299 to Info(ContainerType.MP4, MediaType.VIDEO, 1920, 1080, 60, 0, "avc1", ""),
        278 to Info(ContainerType.WEBM, MediaType.VIDEO, 256, 144, 30, 0, "vp9", ""),
        242 to Info(ContainerType.WEBM, MediaType.VIDEO, 426, 240, 30, 0, "vp9", ""),
        243 to Info(ContainerType.WEBM, MediaType.VIDEO, 640, 360, 30, 0, "vp9", ""),
        244 to Info(ContainerType.WEBM, MediaType.VIDEO, 854, 480, 30, 0, "vp9", ""),
        247 to Info(ContainerType.WEBM, MediaType.VIDEO, 1280, 720, 30, 0, "vp9", ""),
        248 to Info(ContainerType.WEBM, MediaType.VIDEO, 1920, 1080, 30, 0, "vp9", ""),
        271 to Info(ContainerType.WEBM, MediaType.VIDEO, 2560, 1440, 30, 0, "vp9", ""),
        313 to Info(ContainerType.WEBM, MediaType.VIDEO, 3840, 2160, 30, 0, "vp9", ""),
        302 to Info(ContainerType.WEBM, MediaType.VIDEO, 1280, 720, 60, 0, "vp9", ""),
        303 to Info(ContainerType.WEBM, MediaType.VIDEO, 1920, 1080, 60, 0, "vp9", ""),
        308 to Info(ContainerType.WEBM, MediaType.VIDEO, 2560, 1440, 60, 0, "vp9", ""),
        315 to Info(ContainerType.WEBM, MediaType.VIDEO, 3840, 2160, 60, 0, "vp9", ""),
        // Audio-only
        139 to Info(ContainerType.M4A, MediaType.AUDIO, 0, 0, 0, 48, "", "aac"),
        140 to Info(ContainerType.M4A, MediaType.AUDIO, 0, 0, 0, 128, "", "aac"),
        141 to Info(ContainerType.M4A, MediaType.AUDIO, 0, 0, 0, 256, "", "aac"),
        256 to Info(ContainerType.M4A, MediaType.AUDIO, 0, 0, 0, 256, "", "aac"),
        258 to Info(ContainerType.M4A, MediaType.AUDIO, 0, 0, 0, 384, "", "aac"),
        171 to Info(ContainerType.WEBM, MediaType.AUDIO, 0, 0, 0, 128, "", "vorbis"),
        172 to Info(ContainerType.WEBM, MediaType.AUDIO, 0, 0, 0, 192, "", "vorbis"),
        249 to Info(ContainerType.WEBM, MediaType.AUDIO, 0, 0, 0, 50, "", "opus"),
        250 to Info(ContainerType.WEBM, MediaType.AUDIO, 0, 0, 0, 70, "", "opus"),
        251 to Info(ContainerType.WEBM, MediaType.AUDIO, 0, 0, 0, 160, "", "opus"),
        // AV1
        394 to Info(ContainerType.MP4, MediaType.VIDEO, 256, 144, 30, 0, "av01", ""),
        395 to Info(ContainerType.MP4, MediaType.VIDEO, 426, 240, 30, 0, "av01", ""),
        396 to Info(ContainerType.MP4, MediaType.VIDEO, 640, 360, 30, 0, "av01", ""),
        397 to Info(ContainerType.MP4, MediaType.VIDEO, 854, 480, 30, 0, "av01", ""),
        398 to Info(ContainerType.MP4, MediaType.VIDEO, 1280, 720, 30, 0, "av01", ""),
        399 to Info(ContainerType.MP4, MediaType.VIDEO, 1920, 1080, 30, 0, "av01", ""),
        400 to Info(ContainerType.MP4, MediaType.VIDEO, 2560, 1440, 30, 0, "av01", ""),
        401 to Info(ContainerType.MP4, MediaType.VIDEO, 3840, 2160, 30, 0, "av01", "")
    )

    fun info(itag: Int): Info? = table[itag]
}
