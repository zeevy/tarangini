/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanFilterTest {
    @Test
    fun `only mp3, aac and m4a files are accepted, in any case`() {
        val names = listOf("a.mp3", "b.AAC", "c.m4a", "d.opus", "e.flac", "f.mp3.txt", "mp3")

        val accepted = names.filter { ScanFilter().accepts(audioFile("1", fileName = it)) }

        assertEquals(listOf("a.mp3", "b.AAC", "c.m4a"), accepted)
    }

    @Test
    fun `a file shorter than the minimum is left out and a file with no known length stays`() {
        val filter = ScanFilter(minTrackMs = 30_000)

        assertFalse(filter.accepts(audioFile("1", durationMs = 29_999)))
        assertTrue(filter.accepts(audioFile("2", durationMs = 30_000)))
        assertTrue(filter.accepts(audioFile("3", durationMs = null)))
    }

    @Test
    fun `an excluded folder leaves out its files and the files of folders inside it`() {
        val filter = ScanFilter(excludedFolders = setOf("external_primary/WhatsApp"))

        assertFalse(filter.accepts(audioFile("1", folderPath = "external_primary/WhatsApp")))
        assertFalse(filter.accepts(audioFile("2", folderPath = "external_primary/WhatsApp/Media/Audio")))
        assertTrue(filter.accepts(audioFile("3", folderPath = "external_primary/WhatsApp Music")))
        assertTrue(filter.accepts(audioFile("4", folderPath = "external_primary/Music")))
    }

    @Test
    fun `included folders keep only their files, and an excluded folder inside still wins`() {
        val filter = ScanFilter(
            includedFolders = setOf("external_primary/Music/"),
            excludedFolders = setOf("external_primary/Music/Ringtones"),
        )

        assertTrue(filter.accepts(audioFile("1", folderPath = "external_primary/Music/Rock")))
        assertFalse(filter.accepts(audioFile("2", folderPath = "external_primary/Download")))
        assertFalse(filter.accepts(audioFile("3", folderPath = "external_primary/Music/Ringtones")))
    }

    @Test
    fun `a saved folder that names nothing is ignored`() {
        val filter = ScanFilter(includedFolders = setOf("", " / "), excludedFolders = setOf("/"))

        assertTrue(filter.accepts(audioFile("1")))
    }

    @Test
    fun `only a playable file that the settings leave out counts as hidden`() {
        val filter = ScanFilter(minTrackMs = 30_000, excludedFolders = setOf("external_primary/Recordings"))

        assertTrue(filter.hides(audioFile("1", durationMs = 4_000)))
        assertTrue(filter.hides(audioFile("2", folderPath = "external_primary/Recordings")))
        assertFalse(filter.hides(audioFile("3")))
        assertFalse(filter.hides(audioFile("4", fileName = "voice.opus", durationMs = 4_000)))
    }
}
