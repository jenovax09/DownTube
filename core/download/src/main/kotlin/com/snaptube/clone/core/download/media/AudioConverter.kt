package com.snaptube.clone.core.download.media

import java.io.File

/**
 * Audio format conversion.
 *
 * Media3 Transformer can remux AAC audio into M4A but cannot encode MP3, so the
 * MP3 path is delegated to a pluggable encoder. Two implementations are
 * provided:
 *
 *  - [NoOpAudioConverter]  : online-only builds keep the M4A/AAC audio instead
 *                            of transcoding (zero extra native dependencies).
 *  - [FfmpegKitAudioConverter] : wires the optional `ffmpeg-kit` artifact
 *                            (declared behind the `ffmpegKit` Gradle feature) to
 *                            produce true MP3 at a target bitrate.
 *
 * Keeping this behind an interface means the core module never hard-depends on a
 * large native library, exactly as the reference app ships separate `.so`
 * encoder libraries only in some builds.
 */
interface AudioConverter {
    /**
     * Convert [input] (any decodable audio/video) into MP3 at [bitrateKbps].
     * Returns the produced file, or the input unchanged when unsupported.
     */
    suspend fun toMp3(input: File, outputFile: File, bitrateKbps: Int): File

    val supportsMp3: Boolean
}

/** Default: leaves audio as-is; the downloader keeps the M4A/AAC stream. */
class NoOpAudioConverter : AudioConverter {
    override val supportsMp3: Boolean = false

    override suspend fun toMp3(input: File, outputFile: File, bitrateKbps: Int): File {
        if (input != outputFile) {
            input.copyTo(outputFile, overwrite = true)
        }
        return outputFile
    }
}
