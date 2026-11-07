package com.snaptube.clone.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snaptube.clone.AppContainer
import com.snaptube.clone.core.model.QualityPreference
import com.snaptube.clone.data.SettingsRepository
import com.snaptube.clone.ui.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    viewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(container))
) {
    val state by viewModel.state.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { SectionTitle("Downloads") }
            item { DownloadFolderRow(state.downloadPath, viewModel) }
            item {
                SwitchRow(
                    title = "Always ask for quality",
                    subtitle = "Show the format chooser before every download",
                    checked = state.askQuality,
                    onCheckedChange = viewModel::setAskQuality
                )
            }
            item {
                SwitchRow(
                    title = "Download over Wi-Fi only",
                    subtitle = "Queue downloads until an unmetered network is available",
                    checked = state.wifiOnly,
                    onCheckedChange = viewModel::setWifiOnly
                )
            }
            item { MaxConcurrentRow(state.maxConcurrent, viewModel::setMaxConcurrent) }
            item { DefaultQualityRow(state.defaultQuality, viewModel::setDefaultQuality) }

            item { Spacer(Modifier.size(12.dp)) }
            item { SectionTitle("Appearance") }
            item { ThemeRow(state.themeMode, viewModel::setThemeMode) }
            item { LanguageRow(state.language, viewModel::setLanguage) }

            item { Spacer(Modifier.size(12.dp)) }
            item { SectionTitle("Notifications") }
            item {
                SwitchRow(
                    title = "Download notifications",
                    subtitle = "Show progress and completion notifications",
                    checked = state.notifications,
                    onCheckedChange = viewModel::setNotifications
                )
            }

            item { Spacer(Modifier.size(12.dp)) }
            item { SectionTitle("Storage") }
            item { ActionRow("Clear download history") { viewModel.clearHistory() } }
            item { ActionRow("Empty recycle bin") { viewModel.emptyTrash() } }

            item { Spacer(Modifier.size(12.dp)) }
            item { SectionTitle("About") }
            item {
                Text(
                    "SnapTube Clone — an open, clean-room media downloader. " +
                        "Respects site terms of service and never bypasses DRM.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text("Run") }
    }
}

@Composable
private fun DownloadFolderRow(path: String, viewModel: SettingsViewModel) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.setDownloadPath(uri.toString())
    }
    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Folder, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Download folder", style = MaterialTheme.typography.bodyLarge)
                Text(
                    path.ifBlank { viewModel.defaultDownloadPath },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { picker.launch(null) }) { Text("Change") }
        }
    }
}

@Composable
private fun MaxConcurrentRow(value: Int, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Max concurrent downloads", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("$value", style = MaterialTheme.typography.titleMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt().coerceIn(1, 8)) },
            valueRange = 1f..8f,
            steps = 6
        )
    }
}

@Composable
private fun DefaultQualityRow(selected: QualityPreference, onSelect: (QualityPreference) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Default quality", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            QualityPreference.entries.forEach { pref ->
                FilterChip(
                    selected = pref == selected,
                    onClick = { onSelect(pref) },
                    label = {
                        Text(
                            when (pref) {
                                QualityPreference.ALWAYS_ASK -> "Ask"
                                QualityPreference.BEST -> "Best"
                                QualityPreference.MEDIUM -> "Medium"
                                QualityPreference.LOW -> "Low"
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeRow(current: Int, onSelect: (Int) -> Unit) {
    val options = listOf(
        SettingsRepository.THEME_SYSTEM to "System",
        SettingsRepository.THEME_LIGHT to "Light",
        SettingsRepository.THEME_DARK to "Dark"
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Theme", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            options.forEach { (mode, label) ->
                FilterChip(
                    selected = current == mode,
                    onClick = { onSelect(mode) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(current: String, onSelect: (String) -> Unit) {
    val languages = listOf(
        "en" to "English",
        "es" to "Español",
        "pt" to "Português",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "ru" to "Русский",
        "ar" to "العربية",
        "hi" to "हिन्दी",
        "id" to "Bahasa Indonesia",
        "ja" to "日本語",
        "ko" to "한국어",
        "zh" to "中文"
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Language", style = MaterialTheme.typography.bodyLarge)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            items(languages) { (code, label) ->
                FilterChip(
                    selected = current == code,
                    onClick = { onSelect(code) },
                    label = { Text(label) }
                )
            }
        }
    }
}
