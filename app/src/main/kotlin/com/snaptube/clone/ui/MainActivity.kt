package com.snaptube.clone.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.snaptube.clone.SnapTubeApp
import com.snaptube.clone.data.SettingsRepository
import com.snaptube.clone.ui.navigation.SnapTubeNavHost
import com.snaptube.clone.ui.theme.SnapTubeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Link shared into / opened with the app, consumed by the Home screen. */
    private val incomingLink = MutableStateFlow<String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        val container = (application as SnapTubeApp).container

        setContent {
            val themeMode by container.settingsRepository.themeMode
                .collectAsState(initial = SettingsRepository.THEME_SYSTEM)

            SnapTubeTheme(themeMode = themeMode) {
                SnapTubeNavHost(
                    container = container,
                    incomingLink = incomingLink,
                    onConsumeLink = { incomingLink.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val link = extractLink(intent)
        if (link != null) incomingLink.value = link
    }

    private fun extractLink(intent: Intent): String? {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                return intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            Intent.ACTION_VIEW -> {
                return intent.dataString
            }
        }
        return null
    }
}
