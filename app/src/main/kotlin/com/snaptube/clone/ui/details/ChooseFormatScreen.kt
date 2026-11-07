package com.snaptube.clone.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.snaptube.clone.AppContainer
import com.snaptube.clone.core.common.formatBytes
import com.snaptube.clone.core.common.formatDuration
import com.snaptube.clone.core.model.DownloadFormat
import com.snaptube.clone.core.model.MediaType
import com.snaptube.clone.ui.player.PlayerActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseFormatScreen(
    container: AppContainer,
    url: String,
    onBack: () -> Unit,
    viewModel: ChooseFormatViewModel = viewModel(factory = ChooseFormatViewModelFactory(container))
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(url) { viewModel.load(url) }
    LaunchedEffect(state.downloadedTaskId) {
        if (state.downloadedTaskId != null) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose format") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> ErrorState(state.error!!, onBack)
                state.mediaInfo?.isRestricted == true -> RestrictedState(state.mediaInfo!!.restrictionReason)
                else -> Content(state, viewModel)
            }
        }
    }
}

@Composable
private fun Content(state: FormatUiState, viewModel: ChooseFormatViewModel) {
    val info = state.mediaInfo ?: return
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Video", "Audio")

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            AsyncImage(
                model = info.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(96.dp, 72.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(info.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(info.uploader, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (info.durationMs > 0) {
                    Text(formatDuration(info.durationMs), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        val formats = if (selectedTab == 0) state.videoFormats else state.audioFormats
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            items(formats) { format ->
                FormatRow(
                    format = format,
                    selected = state.selection?.id == format.id,
                    onClick = { viewModel.select(format) }
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val preview = state.selection ?: state.videoFormats.maxByOrNull { it.height }
            OutlinedButton(
                onClick = { preview?.let { context.startActivity(PlayerActivity.intentFor(context, it.url)) } },
                enabled = preview != null && preview.mediaType == MediaType.VIDEO,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Preview")
            }
            Button(onClick = { viewModel.download() }, enabled = state.selection != null, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Download")
            }
        }
    }
}

@Composable
private fun FormatRow(format: DownloadFormat, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(format.qualityLabel.ifEmpty { format.label }, style = MaterialTheme.typography.titleSmall)
                val details = buildList {
                    add(format.container.name)
                    if (format.codec.isNotBlank()) add(format.codec)
                    if (format.audioCodec.isNotBlank()) add(format.audioCodec)
                    if (format.contentLength > 0) add(formatBytes(format.contentLength))
                    if (!format.hasAudio && format.mediaType == MediaType.VIDEO) add("video only")
                }.joinToString(" · ")
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Filled.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Could not extract this link", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text("Go back") }
    }
}

@Composable
private fun RestrictedState(reason: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(8.dp))
        Text("Content unavailable", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(reason, style = MaterialTheme.typography.bodyMedium)
    }
}
