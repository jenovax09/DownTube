package com.snaptube.clone.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snaptube.clone.AppContainer
import com.snaptube.clone.core.common.formatBytes
import com.snaptube.clone.core.common.formatDuration
import com.snaptube.clone.core.common.formatSpeed
import com.snaptube.clone.core.model.DownloadStatus
import com.snaptube.clone.core.model.DownloadTask
import com.snaptube.clone.core.model.LocalMedia
import com.snaptube.clone.ui.ViewModelFactory
import com.snaptube.clone.ui.player.PlayerActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    container: AppContainer,
    viewModel: LibraryViewModel = viewModel(factory = ViewModelFactory(container))
) {
    val state by viewModel.state.collectAsState()
    val active by viewModel.activeTasks.collectAsState()
    val completed by viewModel.completedTasks.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val history by viewModel.history.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("Library") }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LibraryTab.entries.toList()) { tab ->
                    FilterChip(
                        selected = state.tab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        label = { Text(tab.label) }
                    )
                }
            }

            when (state.tab) {
                LibraryTab.DOWNLOADS -> DownloadsList(
                    active = active,
                    completed = completed,
                    progress = progress,
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onCancel = viewModel::cancel,
                    onRetry = viewModel::retry,
                    onDelete = viewModel::delete,
                    onPlay = { task ->
                        if (task.outputPath.isNotBlank()) {
                            context.startActivity(PlayerActivity.intentForFile(context, task.outputPath))
                        }
                    }
                )
                LibraryTab.HISTORY -> HistoryList(history)
                LibraryTab.VIDEOS -> LocalList(state.localVideos) { media ->
                    context.startActivity(PlayerActivity.intentForFile(context, media.path))
                }
                LibraryTab.AUDIO -> LocalList(state.localAudio) { media ->
                    context.startActivity(PlayerActivity.intentForFile(context, media.path))
                }
            }
        }
    }
}

@Composable
private fun DownloadsList(
    active: List<DownloadTask>,
    completed: List<DownloadTask>,
    progress: Map<Long, com.snaptube.clone.core.download.DownloadProgress>,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onPlay: (DownloadTask) -> Unit
) {
    if (active.isEmpty() && completed.isEmpty()) {
        EmptyState("No downloads yet")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (active.isNotEmpty()) item { Text("Downloading", style = MaterialTheme.typography.titleSmall) }
        items(active) { task ->
            val p = progress[task.id]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(task.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    val downloaded = p?.downloadedBytes ?: task.downloadedBytes
                    val total = p?.totalBytes ?: task.totalBytes
                    Text(
                        "${formatBytes(downloaded)} / ${formatBytes(total)} · ${p?.status ?: task.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(8.dp))
                    LinearProgressIndicator(
                        progress = { if (total > 0) (downloaded.toFloat() / total) else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (p != null && p.bytesPerSecond > 0) {
                        Text(formatSpeed(p.bytesPerSecond), style = MaterialTheme.typography.labelSmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.QUEUED) {
                            ActionButton("Pause") { onPause(task.id) }
                        } else if (task.status == DownloadStatus.PAUSED) {
                            ActionButton("Resume") { onResume(task.id) }
                        }
                        if (task.status == DownloadStatus.FAILED) {
                            ActionButton("Retry") { onRetry(task.id) }
                        }
                        ActionButton("Cancel") { onCancel(task.id) }
                    }
                }
            }
        }

        if (completed.isNotEmpty()) item { Text("Completed", style = MaterialTheme.typography.titleSmall) }
        items(completed) { task ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(
                            "${formatBytes(task.totalBytes)} · ${task.formatId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (task.outputPath.isNotBlank()) {
                        IconButton(onClick = { onPlay(task) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Open")
                        }
                    }
                    IconButton(onClick = { onDelete(task.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(history: List<com.snaptube.clone.core.database.entity.HistoryEntity>) {
    if (history.isEmpty()) {
        EmptyState("No history yet")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(history) { row ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (row.mediaType == 1) Icons.Filled.MusicNote else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(
                            "${formatBytes(row.fileSize)} · ${row.format}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalList(items: List<LocalMedia>, onPlay: (LocalMedia) -> Unit) {
    if (items.isEmpty()) {
        EmptyState("Nothing here yet")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { media ->
            Card(Modifier.fillMaxWidth(), onClick = { onPlay(media) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(media.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(
                            "${formatDuration(media.durationMs)} · ${formatBytes(media.sizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(label) }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
