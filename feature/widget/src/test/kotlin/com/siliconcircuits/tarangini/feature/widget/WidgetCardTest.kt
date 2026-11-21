/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.widget

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.siliconcircuits.tarangini.core.domain.playback.NowPlayingCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCardTest {
    private val song = NowPlayingCard("Bhairavi", "Ravi", "content://media/external/audio/media/1", showsPause = true)

    @Test
    fun `a card comes back as it was written`() {
        val saved = mutablePreferencesOf().apply { write(song, processId = 7) }

        assertEquals(song, saved.card(processId = 7))
    }

    @Test
    fun `a card of a process that has ended shows a play button`() {
        val saved = mutablePreferencesOf().apply { write(song, processId = 7) }

        assertEquals(song.copy(showsPause = false), saved.card(processId = 8))
    }

    @Test
    fun `only the process that wrote the card runs its playback service`() {
        val saved = mutablePreferencesOf().apply { write(song, processId = 7) }

        assertTrue(saved.isFromProcess(7))
        assertFalse(saved.isFromProcess(8))
        assertFalse(mutablePreferencesOf().isFromProcess(7))
    }

    @Test
    fun `a card with no texts and no cover keeps them empty`() {
        val stream = NowPlayingCard(title = null, artist = null, songUri = null, showsPause = false)
        val saved = mutablePreferencesOf().apply { write(stream, processId = 7) }

        assertEquals(stream, saved.card(processId = 7))
    }

    @Test
    fun `nothing loaded clears the last song`() {
        val saved = mutablePreferencesOf().apply {
            write(song, processId = 7)
            write(null, processId = 7)
        }

        assertNull(saved.card(processId = 7))
        assertNull(mutablePreferencesOf().card(processId = 7))
    }
}
