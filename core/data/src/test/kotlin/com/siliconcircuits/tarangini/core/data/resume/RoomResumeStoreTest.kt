/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.resume

import com.siliconcircuits.tarangini.core.data.local.addSong
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.playback.ResumePlace
import com.siliconcircuits.tarangini.core.domain.playback.SavedPlayback
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoomResumeStoreTest {
    private val database = testDatabase()

    /** A new store is what the app has after it was killed and started again. */
    private fun TestScope.store() = RoomResumeStore(database.queueDao(), UnconfinedTestDispatcher(testScheduler))

    private fun song(number: Int) = "content://media/external/audio/media/$number"

    private fun place(index: Int, positionMs: Long = 0) =
        ResumePlace(index, positionMs, shuffle = false, repeat = RepeatMode.OFF)

    private suspend fun addSongs(vararg numbers: Int) = numbers.map { database.addSong("song $it", sourceId = "$it") }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `a saved queue comes back after a restart with its order, place, shuffle and repeat`() = runTest {
        addSongs(1, 2, 3)
        val playback = SavedPlayback(
            mediaIds = listOf(song(3), song(1), song(3), song(2)),
            place = ResumePlace(index = 2, positionMs = 61_000, shuffle = true, repeat = RepeatMode.ALL),
        )

        store().saveQueue(playback)

        assertEquals(playback, store().load())
    }

    @Test
    fun `nothing saved gives nothing, and an emptied queue gives nothing`() = runTest {
        assertNull(store().load())

        addSongs(1)
        store().saveQueue(SavedPlayback(listOf(song(1)), place(0)))
        store().saveQueue(SavedPlayback(emptyList(), place(0)))

        assertNull(store().load())
    }

    @Test
    fun `a new place is saved for the stored queue, in the same run and after a restart`() = runTest {
        addSongs(1, 2)
        val store = store()
        store.saveQueue(SavedPlayback(listOf(song(1), song(2)), place(0)))
        store.savePlace(place(1, positionMs = 5_000))
        assertEquals(place(1, 5_000), store().load()?.place)

        val afterRestart = store()
        afterRestart.load()
        afterRestart.savePlace(place(0, positionMs = 7_000))

        assertEquals(place(0, 7_000), store().load()?.place)
    }

    @Test
    fun `a place is not saved before the store knows which queue it belongs to`() = runTest {
        addSongs(1, 2)
        store().saveQueue(SavedPlayback(listOf(song(1), song(2)), place(1, positionMs = 5_000)))

        store().savePlace(place(0))

        assertEquals(place(1, 5_000), store().load()?.place)
    }

    @Test
    fun `an item that is not a song of the library is left out, and the place moves to the next song`() = runTest {
        addSongs(1, 2)
        val ids = listOf(song(1), "https://example.com/stream", song(99), song(2))

        store().saveQueue(SavedPlayback(ids, place(index = 2, positionMs = 9_000)))
        val loaded = requireNotNull(store().load())

        assertEquals(listOf(song(1), song(2)), loaded.mediaIds)
        assertEquals(place(1, 9_000), loaded.place)
    }

    @Test
    fun `when the file of the current song is gone, the next song is current, from its start`() = runTest {
        val (_, second, _) = addSongs(1, 2, 3)
        store().saveQueue(SavedPlayback(listOf(song(1), song(2), song(3)), place(index = 1, positionMs = 9_000)))

        database.libraryDao().markMissing(listOf(second), at = 1)
        val loaded = requireNotNull(store().load())

        assertEquals(listOf(song(1), song(3)), loaded.mediaIds)
        assertEquals(place(1, 0), loaded.place)
    }

    @Test
    fun `when the last song was current and its file is gone, the queue starts at the top`() = runTest {
        val (_, second) = addSongs(1, 2)
        store().saveQueue(SavedPlayback(listOf(song(1), song(2)), place(index = 1, positionMs = 9_000)))

        database.libraryDao().markMissing(listOf(second), at = 1)

        assertEquals(place(0, 0), store().load()?.place)
    }

    @Test
    fun `a place saved after a song went missing still names the right song when the song is back`() = runTest {
        val (_, second, _) = addSongs(1, 2, 3)
        store().saveQueue(SavedPlayback(listOf(song(1), song(2), song(3)), place(0)))
        database.libraryDao().markMissing(listOf(second), at = 1)

        val store = store()
        store.load()
        // The player holds songs 1 and 3 now, so its place 1 is song 3.
        store.savePlace(place(1, positionMs = 4_000))

        val loaded = requireNotNull(store().load())
        assertEquals(song(3), loaded.mediaIds[loaded.place.index])
        assertEquals(4_000L, loaded.place.positionMs)
    }
}
