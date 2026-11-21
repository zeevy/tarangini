/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamSongsTest {
    private val songs = StreamSongs<String>()

    @Test
    fun `the song of a stream is known when its sound starts, also after the next one was read`() {
        songs.read(offsetUs = 1_000, song = "first")
        songs.read(offsetUs = 5_000, song = "second")

        assertEquals("first", songs.started(1_000))
        assertEquals("second", songs.started(5_000))
    }

    @Test
    fun `the first stream plays at once, a stream read ahead waits for the song before it`() {
        // A new queue: the stream is the one that goes out as soon as it is handed over.
        songs.read(offsetUs = 1_000, song = "first")
        assertEquals("first", songs.playsAtOnce(offsetUs = 1_000, outputOffsetUs = 1_000))

        // The next song is read while the first still plays.
        songs.read(offsetUs = 5_000, song = "second")
        assertNull(songs.playsAtOnce(offsetUs = 5_000, outputOffsetUs = 1_000))
    }

    @Test
    fun `a skip that gives the new song the same offset tells of the new song`() {
        songs.read(offsetUs = 1_000, song = "first")
        songs.read(offsetUs = 1_000, song = "after the skip")

        assertEquals("after the skip", songs.playsAtOnce(offsetUs = 1_000, outputOffsetUs = 1_000))
    }

    @Test
    fun `a stream that was never read is not known`() {
        assertNull(songs.started(42))
    }

    @Test
    fun `only the last few streams are kept`() {
        (1L..20L).forEach { songs.read(it, "song $it") }

        assertNull(songs.started(1))
        assertEquals("song 20", songs.started(20))
    }
}
