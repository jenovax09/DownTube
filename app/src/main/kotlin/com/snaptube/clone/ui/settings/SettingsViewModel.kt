package com.snaptube.clone.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptube.clone.AppContainer
import com.snaptube.clone.data.SettingsRepository
import com.snaptube.clone.core.model.QualityPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Immutable snapshot of every user preference shown on the settings screen. */
data class SettingsUiState(
    val downloadPath: String = "",
    val askQuality: Boolean = true,
    val wifiOnly: Boolean = false,
    val maxConcurrent: Int = 3,
    val themeMode: Int = SettingsRepository.THEME_SYSTEM,
    val language: String = "en",
    val notifications: Boolean = true,
    val defaultQuality: QualityPreference = QualityPreference.ALWAYS_ASK
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val settings: SettingsRepository = container.settingsRepository

    private val _message = MutableStateFlow<String?>(null)

    /** One-shot user feedback (e.g. "cleared"). */
    val message: StateFlow<String?> = _message

    val state: StateFlow<SettingsUiState> = combine(
        settings.downloadPath,
        settings.askQuality,
        settings.wifiOnly,
        settings.maxConcurrent,
        settings.themeMode
    ) { path, ask, wifi, max, theme ->
        SettingsUiState(
            downloadPath = path,
            askQuality = ask,
            wifiOnly = wifi,
            maxConcurrent = max,
            themeMode = theme
        )
    }
        .combine(settings.language) { s, lang -> s.copy(language = lang) }
        .combine(settings.notifications) { s, notif -> s.copy(notifications = notif) }
        .combine(settings.defaultQuality) { s, quality -> s.copy(defaultQuality = quality) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsUiState()
        )

    /** Folder the app writes finished downloads to (defaults to the app storage root). */
    val defaultDownloadPath: String get() = container.storageManager.resolveFolder().root.absolutePath

    fun setDownloadPath(path: String) = viewModelScope.launch { settings.setDownloadPath(path) }
    fun resetDownloadPath() = viewModelScope.launch { settings.setDownloadPath(defaultDownloadPath) }
    fun setAskQuality(value: Boolean) = viewModelScope.launch { settings.setAskQuality(value) }
    fun setWifiOnly(value: Boolean) = viewModelScope.launch { settings.setWifiOnly(value) }
    fun setMaxConcurrent(value: Int) = viewModelScope.launch { settings.setMaxConcurrent(value) }
    fun setThemeMode(mode: Int) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setLanguage(code: String) = viewModelScope.launch { settings.setLanguage(code) }
    fun setNotifications(value: Boolean) = viewModelScope.launch { settings.setNotifications(value) }
    fun setDefaultQuality(pref: QualityPreference) = viewModelScope.launch { settings.setDefaultQuality(pref) }

    fun clearHistory() = viewModelScope.launch {
        container.database.historyDao().clear()
        _message.value = "History cleared"
    }

    fun emptyTrash() = viewModelScope.launch {
        container.database.trashDao().clear()
        _message.value = "Recycle bin emptied"
    }

    fun consumeMessage() {
        _message.value = null
    }
}
