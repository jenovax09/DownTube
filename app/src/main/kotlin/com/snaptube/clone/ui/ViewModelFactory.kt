package com.snaptube.clone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.snaptube.clone.AppContainer
import com.snaptube.clone.ui.details.ChooseFormatViewModel
import com.snaptube.clone.ui.home.HomeViewModel
import com.snaptube.clone.ui.library.LibraryViewModel
import com.snaptube.clone.ui.search.SearchViewModel
import com.snaptube.clone.ui.settings.SettingsViewModel

/**
 * Single factory so every screen can build its ViewModel from the shared
 * [AppContainer] without a DI framework.
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(container) as T
        modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(container) as T
        modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(container) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container) as T
        modelClass.isAssignableFrom(ChooseFormatViewModel::class.java) -> ChooseFormatViewModel(container) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
