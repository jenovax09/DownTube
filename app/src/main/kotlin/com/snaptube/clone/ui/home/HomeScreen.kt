package com.snaptube.clone.ui.home

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snaptube.clone.AppContainer
import com.snaptube.clone.core.extractor.SupportedSites
import com.snaptube.clone.ui.ViewModelFactory
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    incomingLink: StateFlow<String?>,
    onConsumeLink: () -> Unit,
    onResultUrl: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = ViewModelFactory(container))
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val history by viewModel.recentHistory.collectAsState()
    val sharedLink by incomingLink.collectAsState()
    var input by remember { mutableStateOf("") }

    // A link shared/opened from another app takes priority.
    LaunchedEffect(sharedLink) {
        sharedLink?.let {
            input = it
            viewModel.onLinkDetected(it)
            onConsumeLink()
        }
    }

    // Silently peek at the clipboard to offer a one-tap paste.
    LaunchedEffect(Unit) {
        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clip?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
        if (text != null) viewModel.onLinkDetected(text)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SnapTube Clone", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; viewModel.onLinkDetected(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search or paste a link") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        Button(
                            onClick = {
                                val url = com.snaptube.clone.core.extractor.UrlMatcher.normalize(input)
                                if (url != null) onResultUrl(url)
                                else viewModel.recordSearch(input)
                            }
                        ) { Text("Go") }
                    },
                    singleLine = true
                )
            }

            if (state.clipboardLink != null && state.detectedSite != null &&
                input != state.clipboardLink
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Link detected", style = MaterialTheme.typography.labelMedium)
                                Text(state.clipboardLink!!, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                Text("Source: ${state.detectedSite}", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = {
                                input = state.clipboardLink!!
                                viewModel.consumeLink()
                            }) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = null)
                                Spacer(Modifier.padding(4.dp))
                                Text("Paste")
                            }
                        }
                    }
                }
            }

            item {
                Text("Supported sites", style = MaterialTheme.typography.titleMedium)
            }

            items(SupportedSites.all) { site ->
                Card(Modifier.fillMaxWidth(), onClick = {
                    input = "https://${site.host}/"
                }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(site.title, style = MaterialTheme.typography.titleSmall)
                            Text(site.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AssistChip(onClick = {}, label = { Text(site.category.name.lowercase()) })
                    }
                }
            }

            if (history.isNotEmpty()) {
                item { Text("Recent downloads", style = MaterialTheme.typography.titleMedium) }
                items(history.take(5)) { row ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(row.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                            Text(
                                com.snaptube.clone.core.common.formatBytes(row.fileSize) + " · " + row.format,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
