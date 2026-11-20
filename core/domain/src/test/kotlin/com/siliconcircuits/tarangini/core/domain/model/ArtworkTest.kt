/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtworkTest {
    @Test
    fun `the cover picture of a local song is asked for at its own address`() {
        assertEquals(
            "content://media/external/audio/media/42/albumart",
            artworkUriOf("content://media/external/audio/media/42"),
        )
    }

    @Test
    fun `a web address and a file path have no cover picture address`() {
        assertNull(artworkUriOf("https://example.org/stream"))
        assertNull(artworkUriOf("/sdcard/Music/song.mp3"))
        assertNull(artworkUriOf(""))
    }

    @Test
    fun `only the exact address of one audio file gets a cover picture address`() {
        listOf(
            "content://media/external/audio/media/42?x=y",
            "content://media/external/audio/media/42#part",
            "content://media/external/audio/media/42/",
            "content://media/external/audio/media/42/albumart",
            "content://media/external/images/media/42",
            "content://media/external/audio/albums/42",
            "content://media//external/audio/media/42",
            "content://other/external/audio/media/42",
        ).forEach { address -> assertNull(address, artworkUriOf(address)) }
    }
}
