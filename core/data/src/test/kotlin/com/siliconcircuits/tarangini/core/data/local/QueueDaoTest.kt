/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.data.local

import com.siliconcircuits.tarangini.core.data.local.entity.ResumeStateEntity
import com.siliconcircuits.tarangini.core.data.local.entity.StoredRepeatMode
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QueueDaoTest {
    private val database = testDatabase()
    private val queue = database.queueDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `the saved queue keeps its order and is replaced as a whole`() = runTest {
        val a = database.addSong("a")
        val b = database.addSong("b")
        queue.replaceQueue(listOf(b, a, b))
        assertEquals(listOf(b, a, b), queue.queue().map { it.row.song.id })

        queue.replaceQueue(listOf(a))
        assertEquals(listOf(a), queue.queue().map { it.row.song.id })

        queue.clearQueue()
        assertTrue(queue.queue().isEmpty())
    }

    @Test
    fun `a queued song whose file is gone is left out and the others keep their saved positions`() = runTest {
        val a = database.addSong("a")
        val b = database.addSong("b")
        val c = database.addSong("c")
        queue.replaceQueue(listOf(a, b, c))

        database.libraryDao().markMissing(listOf(b), at = 1)

        assertEquals(listOf(0 to a, 2 to c), queue.queue().map { it.position to it.row.song.id })
    }

    @Test
    fun `there is one resume state and a save replaces it`() = runTest {
        assertNull(queue.resumeState())
        queue.saveResumeState(state(queuePosition = 1, positionMs = 10))

        queue.saveResumeState(state(queuePosition = 2, positionMs = 20).copy(shuffle = true))

        val stored = requireNotNull(queue.resumeState())
        assertEquals(2, stored.queuePosition)
        assertEquals(20L, stored.positionMs)
        assertTrue(stored.shuffle)
        assertEquals(StoredRepeatMode.ALL, stored.repeatMode)
    }

    @Test
    fun `the resume state forgets a station that was removed`() = runTest {
        val station = database.addStation("A")
        queue.saveResumeState(state(queuePosition = 0, positionMs = 0).copy(stationId = station))

        database.stationDao().deleteStation(station)

        assertNull(requireNotNull(queue.resumeState()).stationId)
    }

    private fun state(queuePosition: Int, positionMs: Long) = ResumeStateEntity(
        queuePosition = queuePosition,
        positionMs = positionMs,
        stationId = null,
        shuffle = false,
        repeatMode = StoredRepeatMode.ALL,
    )
}
