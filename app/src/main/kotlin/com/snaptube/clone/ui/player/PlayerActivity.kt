package com.snaptube.clone.ui.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.snaptube.clone.core.player.PlaybackController
import com.snaptube.clone.core.player.PlsPlaylistParser
import com.snaptube.clone.ui.theme.SnapTubeTheme
import java.io.File

/**
 * Standalone playback screen.
 *
 * Handles a single local file, a content URI, or a `.pls` playlist, and offers
 * the playback-speed presets and a fullscreen/landscape toggle — mirroring the
 * built-in player of the analysed app.
 */
class PlayerActivity : ComponentActivity() {

    private lateinit var controller: PlaybackController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controller = PlaybackController(this)
        prepareFromIntent(intent)

        setContent {
            SnapTubeTheme(themeMode = com.snaptube.clone.data.SettingsRepository.THEME_DARK) {
                Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                    PlayerScreen(
                        controller = controller,
                        onFinish = { finish() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        prepareFromIntent(intent)
    }

    override fun onDestroy() {
        controller.release()
        super.onDestroy()
    }

    private fun prepareFromIntent(intent: Intent?) {
        intent ?: return
        when (val action = intent.action) {
            ACTION_PLAY_PLAYLIST -> {
                val entries = intent.getStringArrayListExtra(EXTRA_PLAYLIST_ENTRIES).orEmpty()
                if (entries.isNotEmpty()) {
                    controller.playPlaylist(entries)
                    return
                }
                val plsPath = intent.getStringExtra(EXTRA_PLS_PATH)
                if (plsPath != null) {
                    val parsed = runCatching { PlsPlaylistParser.parse(File(plsPath).readText()) }.getOrNull()
                    val paths = parsed?.map { it.path }.orEmpty()
                    if (paths.isNotEmpty()) {
                        controller.playPlaylist(paths)
                        return
                    }
                }
            }
            else -> Unit
        }

        val rawUri = intent.getStringExtra(EXTRA_MEDIA_URI) ?: intent.dataString ?: return
        val file = runCatching { File(Uri.parse(rawUri).path ?: rawUri) }.getOrNull()
        if (file != null && file.exists()) {
            controller.playFile(file.absolutePath)
        } else {
            controller.playUri(rawUri)
        }
    }

    companion object {
        private const val ACTION_PLAY_PLAYLIST = "com.snaptube.clone.action.PLAY_PLAYLIST"

        private const val EXTRA_MEDIA_URI = "extra_media_uri"
        private const val EXTRA_PLS_PATH = "extra_pls_path"
        private const val EXTRA_PLAYLIST_ENTRIES = "extra_playlist_entries"

        /** Open a single local file or content URI. */
        fun intentFor(context: Context, uri: String): Intent =
            Intent(context, PlayerActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(EXTRA_MEDIA_URI, uri)

        fun intentForFile(context: Context, path: String): Intent =
            intentFor(context, File(path).toURI().toString())

        /** Open every entry of an exported `.pls` playlist. */
        fun intentForPls(context: Context, plsPath: String): Intent =
            Intent(context, PlayerActivity::class.java)
                .setAction(ACTION_PLAY_PLAYLIST)
                .putExtra(EXTRA_PLS_PATH, plsPath)

        fun intentForPlaylist(context: Context, paths: List<String>): Intent =
            Intent(context, PlayerActivity::class.java)
                .setAction(ACTION_PLAY_PLAYLIST)
                .putStringArrayListExtra(EXTRA_PLAYLIST_ENTRIES, ArrayList(paths))
    }
}

@Composable
private fun PlayerScreen(
    controller: PlaybackController,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var speed by remember { mutableFloatStateOf(1.0f) }
    var speedMenu by remember { mutableStateOf(false) }
    var landscape by remember {
        mutableStateOf(
            (context as? ComponentActivity)?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        )
    }

    BackHandler { onFinish() }

    DisposableEffect(Unit) {
        (context as? ComponentActivity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            (context as? ComponentActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    player = controller.player
                }
            }
        )

        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Box {
                IconButton(onClick = { speedMenu = true }) {
                    Icon(Icons.Filled.Speed, contentDescription = "Speed", tint = Color.White)
                }
                DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                    PlaybackController.SPEED_PRESETS.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text("${preset}x") },
                            onClick = {
                                speed = preset
                                controller.setSpeed(preset)
                                speedMenu = false
                            }
                        )
                    }
                }
            }
            IconButton(onClick = {
                val activity = context as? ComponentActivity ?: return@IconButton
                landscape = !landscape
                activity.requestedOrientation = if (landscape) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }) {
                Icon(Icons.Filled.ScreenRotation, contentDescription = "Fullscreen", tint = Color.White)
            }
        }

        if (speed != 1.0f) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    "Speed ${speed}x",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
