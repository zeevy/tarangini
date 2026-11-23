/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini

import android.app.SearchManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.home.HomeSections
import com.siliconcircuits.tarangini.feature.player.miniplayer.MiniPlayer
import com.siliconcircuits.tarangini.feature.player.navigation.NowPlayingPane
import com.siliconcircuits.tarangini.feature.settings.SettingsSections
import com.siliconcircuits.tarangini.navigation.TaranginiShell
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var playback: PlaybackController

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is dark only, so the icons of the system bars are always light.
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        // A screen that is built again, after a turn of the phone, must not start the request a second time.
        if (savedInstanceState == null) playSpokenRequest(intent)
        setContent {
            TaranginiTheme {
                TaranginiShell(
                    miniPlayer = { onOpen, modifier -> MiniPlayer(onOpen = onOpen, modifier = modifier) },
                    nowPlayingPane = { controller, modifier -> NowPlayingPane(controller, modifier) },
                    home = { targets -> HomeSections(targets.onOpenLibrary, targets.onOpenPlaylist) },
                    settings = { onOpenLicences -> SettingsSections(onOpenLicences) },
                )
            }
        }
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
