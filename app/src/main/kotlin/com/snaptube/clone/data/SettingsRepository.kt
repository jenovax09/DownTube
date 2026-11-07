package com.snaptube.clone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.snaptube.clone.core.model.QualityPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snaptube_settings")

/** Persisted user preferences, backed by DataStore. */
class SettingsRepository(private val context: Context) {

    val downloadPath: Flow<String> = context.dataStore.data.map { it[KEY_PATH] ?: "" }
    val askQuality: Flow<Boolean> = context.dataStore.data.map { it[KEY_ASK_QUALITY] ?: true }
    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { it[KEY_WIFI_ONLY] ?: false }
    val maxConcurrent: Flow<Int> = context.dataStore.data.map { it[KEY_MAX_CONCURRENT] ?: 3 }
    val themeMode: Flow<Int> = context.dataStore.data.map { it[KEY_THEME] ?: THEME_SYSTEM }
    val language: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "en" }
    val notifications: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATIONS] ?: true }
    val defaultQuality: Flow<QualityPreference> = context.dataStore.data.map {
        QualityPreference.entries.getOrElse(it[KEY_DEFAULT_QUALITY] ?: 0) { QualityPreference.ALWAYS_ASK }
    }

    suspend fun setDownloadPath(path: String) = context.dataStore.edit { it[KEY_PATH] = path }
    suspend fun setAskQuality(value: Boolean) = context.dataStore.edit { it[KEY_ASK_QUALITY] = value }
    suspend fun setWifiOnly(value: Boolean) = context.dataStore.edit { it[KEY_WIFI_ONLY] = value }
    suspend fun setMaxConcurrent(value: Int) = context.dataStore.edit { it[KEY_MAX_CONCURRENT] = value.coerceIn(1, 8) }
    suspend fun setThemeMode(mode: Int) = context.dataStore.edit { it[KEY_THEME] = mode }
    suspend fun setLanguage(code: String) = context.dataStore.edit { it[KEY_LANGUAGE] = code }
    suspend fun setNotifications(value: Boolean) = context.dataStore.edit { it[KEY_NOTIFICATIONS] = value }
    suspend fun setDefaultQuality(pref: QualityPreference) =
        context.dataStore.edit { it[KEY_DEFAULT_QUALITY] = pref.ordinal }

    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        private val KEY_PATH = stringPreferencesKey("download_path")
        private val KEY_ASK_QUALITY = booleanPreferencesKey("ask_quality")
        private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        private val KEY_MAX_CONCURRENT = intPreferencesKey("max_concurrent")
        private val KEY_THEME = intPreferencesKey("theme_mode")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        private val KEY_DEFAULT_QUALITY = intPreferencesKey("default_quality")
    }
}
