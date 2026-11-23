/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini

import android.app.SearchManager
import android.app.UiModeManager
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.siliconcircuits.tarangini.core.domain.model.Look
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.repository.SettingsRepository
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.miniplayer.MiniPlayer
import com.siliconcircuits.tarangini.feature.player.navigation.PlayerTab
import com.siliconcircuits.tarangini.feature.settings.SettingsSections
import com.siliconcircuits.tarangini.navigation.TaranginiShell
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var playback: PlaybackController

    @Inject
    lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // The icons of the system bars follow the night mode of the app, which is the saved look.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A screen that is built again, after a turn of the phone, must not start the request a second time.
        if (savedInstanceState == null) playSpokenRequest(intent)
        lifecycleScope.launch { settings.look().collect(::applyLook) }
        setContent {
            TaranginiTheme {
                TaranginiShell(
                    miniPlayer = { onOpen, modifier -> MiniPlayer(onOpen = onOpen, modifier = modifier) },
                    player = { targets, modifier -> PlayerTab(targets, modifier) },
                    settings = { onOpenLicences -> SettingsSections(onOpenLicences) },
                )
            }
        }
    }

    /**
     * Hands the look to Android as the night mode of this app. Android keeps it between starts, so
     * the window before the first frame, the bar icons and the system dialogs already have the right
     * colours, and the theme follows it through the dark mode it reads. A change starts the screen
     * again with the new mode; the same mode again changes nothing.
     */
    private fun applyLook(look: Look) {
        val mode = when (look) {
            Look.FOLLOW_PHONE -> UiModeManager.MODE_NIGHT_AUTO
            Look.LIGHT -> UiModeManager.MODE_NIGHT_NO
            Look.DARK -> UiModeManager.MODE_NIGHT_YES
        }
        getSystemService(UiModeManager::class.java)?.setApplicationNightMode(mode)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        playSpokenRequest(intent)
    }

    /**
     * "Play ... on Tarangini", said to the voice assistant of the phone, opens the app with the words.
     * Any app can send this, so the words are only ever text to search the library for.
     */
    private fun playSpokenRequest(intent: Intent?) {
        if (intent?.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
            // Extras of another app can fail to read. Then the request counts as one with no words.
            val words = runCatching { intent.getStringExtra(SearchManager.QUERY) }.getOrNull().orEmpty()
            playback.playSpoken(words.take(MOST_WORDS))
        }
    }
}

// A spoken request is a few words. The rest of a longer text is dropped.
private const val MOST_WORDS = 200
