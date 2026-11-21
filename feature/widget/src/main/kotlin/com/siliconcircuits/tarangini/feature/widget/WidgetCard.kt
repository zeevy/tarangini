/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.siliconcircuits.tarangini.core.domain.playback.NowPlayingCard

/**
 * Writes [card] into the saved state of one widget. The widget keeps it while the app is closed, so
 * it still shows the last song after the app or the phone was restarted.
 *
 * @param processId the process that plays. Playback runs in this process, so a card of a process
 *   that has ended is no longer playing
 */
internal fun MutablePreferences.write(card: NowPlayingCard?, processId: Int) {
    clear()
    if (card == null) return
    this[LOADED] = true
    card.title?.let { this[TITLE] = it }
    card.artist?.let { this[ARTIST] = it }
    card.songUri?.let { this[SONG] = it }
    this[PAUSE] = card.showsPause
    this[PROCESS] = processId
}

/**
 * The card the widget shows, or null when nothing was loaded.
 *
 * @param processId the process that reads. A card that a process wrote that has ended shows a play
 *   button: the playback ended with that process, also when it had no time to say so
 */
internal fun Preferences.card(processId: Int): NowPlayingCard? = if (this[LOADED] == true) {
    NowPlayingCard(
        title = this[TITLE],
        artist = this[ARTIST],
        songUri = this[SONG],
        showsPause = this[PAUSE] == true && this[PROCESS] == processId,
    )
} else {
    null
}

/** True when the process [processId] wrote the card, so the playback service of this process is running. */
internal fun Preferences.isFromProcess(processId: Int): Boolean = this[PROCESS] == processId

private val LOADED = booleanPreferencesKey("loaded")
private val TITLE = stringPreferencesKey("title")
private val ARTIST = stringPreferencesKey("artist")
private val SONG = stringPreferencesKey("song")
private val PAUSE = booleanPreferencesKey("pause")
private val PROCESS = intPreferencesKey("process")
