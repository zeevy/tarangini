/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import com.siliconcircuits.tarangini.core.data.local.dao.SongKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanPlanTest {
    private fun stored(id: Long, file: AudioFile, missingSince: Long? = null) = SongKey(
        id = id,
        sourceId = file.id,
        dateModified = file.dateModified,
        missingSince = missingSince,
        fileName = file.fileName,
        sizeBytes = file.sizeBytes,
        folderPath = file.folderPath,
    )

    private fun plan(
        stored: List<SongKey>,
        found: List<AudioFile>,
        expiredBefore: Long = 0,
        hidden: List<AudioFile> = emptyList(),
    ) = planScan(stored, found, hidden, expiredBefore)

    @Test
    fun `files that are stored and did not change give an empty plan`() {
        val files = listOf(audioFile("1"), audioFile("2"))

        val plan = plan(files.mapIndexed { index, file -> stored(index + 1L, file) }, files, expiredBefore = 0)

        assertTrue(plan.isEmpty)
        assertEquals(2, plan.unchanged)
    }

    @Test
    fun `a file that is not stored is added`() {
        val plan = plan(emptyList(), listOf(audioFile("1")), expiredBefore = 0)

        assertEquals(listOf("1"), plan.added.map { it.id })
        assertFalse(plan.isEmpty)
    }

    @Test
    fun `a file with a new change time or a new size is updated`() {
        val first = audioFile("1")
        val second = audioFile("2")
        val found = listOf(first.copy(dateModified = 2_000), second.copy(sizeBytes = 9))

        val plan = plan(listOf(stored(1, first), stored(2, second)), found, expiredBefore = 0)

        assertEquals(listOf("1", "2"), plan.updated.map { it.id })
        assertEquals(0, plan.unchanged)
    }

    @Test
    fun `a stored song whose file is not found is gone, once`() {
        val first = audioFile("1")
        val second = audioFile("2")

        val rows = listOf(stored(1, first), stored(2, second, missingSince = 50))

        val plan = plan(rows, emptyList(), expiredBefore = 0)

        assertEquals(listOf(1L), plan.gone)
        assertTrue(plan.expired.isEmpty())
    }

    @Test
    fun `a song that has been gone for longer than the keep time is expired`() {
        val old = audioFile("1")
        val recent = audioFile("2")
        val rows = listOf(stored(1, old, missingSince = 99), stored(2, recent, missingSince = 100))

        val plan = plan(rows, emptyList(), expiredBefore = 100)

        assertEquals(listOf(1L), plan.expired)
    }

    @Test
    fun `a file that came back is updated, so it shows again`() {
        val file = audioFile("1")

        val plan = plan(listOf(stored(1, file, missingSince = 50)), listOf(file), expiredBefore = 0)

        assertEquals(listOf("1"), plan.updated.map { it.id })
    }

    @Test
    fun `a file with a new id and the folder, name and size of one lost song is that song`() {
        val before = audioFile("1", fileName = "raga.mp3", sizeBytes = 4_000)
        val after = before.copy(id = "77")

        val plan = plan(listOf(stored(5, before, missingSince = 50)), listOf(after), expiredBefore = 1_000)

        assertEquals(mapOf("77" to 5L), plan.moved)
        assertEquals(listOf("77"), plan.updated.map { it.id })
        assertTrue(plan.added.isEmpty() && plan.gone.isEmpty() && plan.expired.isEmpty())
    }

    @Test
    fun `two lost songs with the same name and size are not guessed at`() {
        val one = audioFile("1", fileName = "track.mp3", sizeBytes = 4_000)
        val two = audioFile("2", fileName = "track.mp3", sizeBytes = 4_000)
        val found = listOf(one.copy(id = "8"), two.copy(id = "9"))

        val plan = plan(listOf(stored(1, one), stored(2, two)), found, expiredBefore = 0)

        assertTrue(plan.moved.isEmpty())
        assertEquals(listOf("8", "9"), plan.added.map { it.id })
        assertEquals(listOf(1L, 2L), plan.gone)
    }

    @Test
    fun `a file with the name and size of a lost song in another folder is a new song`() {
        val before = audioFile("1", fileName = "raga.mp3", sizeBytes = 4_000, folderPath = "external_primary/Music/A")
        val other = before.copy(id = "77", folderPath = "external_primary/Music/B")

        val plan = plan(listOf(stored(5, before)), listOf(other))

        assertTrue(plan.moved.isEmpty())
        assertEquals(listOf("77"), plan.added.map { it.id })
        assertEquals(listOf(5L), plan.gone)
    }

    @Test
    fun `two songs with the same name and size in two folders are both found again under new ids`() {
        val one = audioFile("1", fileName = "track.mp3", sizeBytes = 4_000, folderPath = "external_primary/Music/A")
        val two = audioFile("2", fileName = "track.mp3", sizeBytes = 4_000, folderPath = "external_primary/Music/B")
        val found = listOf(one.copy(id = "8"), two.copy(id = "9"))

        val plan = plan(listOf(stored(1, one), stored(2, two)), found)

        assertEquals(mapOf("8" to 1L, "9" to 2L), plan.moved)
        assertTrue(plan.added.isEmpty() && plan.gone.isEmpty())
    }

    @Test
    fun `a song the settings hide gets the hidden mark and is never gone, expired or matched to another file`() {
        val file = audioFile("1", fileName = "note.mp3", sizeBytes = 4_000)
        val sameLooking = file.copy(id = "77")

        val first = plan(listOf(stored(1, file)), listOf(sameLooking), hidden = listOf(file))
        val later = plan(
            listOf(stored(1, file, missingSince = LibraryDao.HIDDEN_SINCE)),
            emptyList(),
            expiredBefore = Long.MAX_VALUE,
            hidden = listOf(file),
        )

        assertEquals(listOf(1L), first.hidden)
        assertTrue(first.gone.isEmpty() && first.moved.isEmpty())
        assertEquals(listOf("77"), first.added.map { it.id })
        assertTrue(later.isEmpty)
    }

    @Test
    fun `a hidden song stays hidden when the phone gives its file a new id`() {
        val file = audioFile("1", fileName = "note.mp3", sizeBytes = 4_000)
        val rows = listOf(stored(1, file, missingSince = LibraryDao.HIDDEN_SINCE))

        val plan = plan(rows, emptyList(), expiredBefore = Long.MAX_VALUE, hidden = listOf(file.copy(id = "77")))

        assertTrue(plan.isEmpty)
    }

    @Test
    fun `a hidden song whose file is now gone starts its keep time and is not removed in that scan`() {
        val file = audioFile("1")
        val rows = listOf(stored(1, file, missingSince = LibraryDao.HIDDEN_SINCE))

        val plan = plan(rows, emptyList(), expiredBefore = Long.MAX_VALUE)

        assertEquals(listOf(1L), plan.lostWhileHidden)
        assertTrue(plan.expired.isEmpty() && plan.gone.isEmpty())
    }
}
