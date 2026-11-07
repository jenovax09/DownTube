package com.snaptube.clone.core.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Small wrapper that owns a single ExoPlayer instance for in-app playback
 * (full-screen video, the floating mini-bar and the music player all share it).
 *
 * Lifecycle is managed by the caller; [release] must be called when the owner
 * is destroyed.
 */
class PlaybackController(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true
        )
        repeatMode = Player.REPEAT_MODE_OFF
    }

    val isPlaying: Boolean get() = player.isPlaying

    fun playFile(path: String, startPositionMs: Long = 0L) {
        player.setMediaItem(MediaItem.fromUri(java.io.File(path).toURI().toString()))
        if (startPositionMs > 0) player.seekTo(startPositionMs)
        player.prepare()
        player.play()
    }

    fun playUri(uri: String) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    /** Queue a playlist built from .pls or a plain list of local files. */
    fun playPlaylist(paths: List<String>, startIndex: Int = 0) {
        val items = paths.map { MediaItem.fromUri(java.io.File(it).toURI().toString()) }
        player.setMediaItems(items, startIndex, 0L)
        player.prepare()
        player.play()
    }

    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun setVolume(volume: Float) {
        player.volume = volume
    }

    fun release() {
        player.release()
    }

    companion object {
        /** Preset speeds exposed by the playback speed dialog. */
        val SPEED_PRESETS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    }
}
