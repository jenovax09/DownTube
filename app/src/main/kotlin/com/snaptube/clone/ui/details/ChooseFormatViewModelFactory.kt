package com.snaptube.clone.ui.details

import androidx.lifecycle.ViewModelProvider
import com.snaptube.clone.AppContainer

/** Convenience factory for the format chooser (see [com.snaptube.clone.ui.ViewModelFactory]). */
fun ChooseFormatViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    com.snaptube.clone.ui.ViewModelFactory(container)
