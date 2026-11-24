/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitingSoundTest {
    private val waiting = WaitingSound()
    private val written = mutableListOf<Long>()

    @Test
    fun `sound waits in order until the sink has room, and a full sink keeps it`() {
        waiting.add(FadeMixer.Taken(ShortArray(4), timeUs = 1))
        waiting.add(FadeMixer.Taken(ShortArray(2), timeUs = 2))

        val full = waiting.offer { _, _ -> false }
        val done = waiting.offer(::takeAll)

        assertFalse(full)
        assertTrue(done)
        assertEquals(listOf(1L, 2L), written)
        assertTrue(waiting.isEmpty())
    }

    @Test
    fun `nothing to play adds nothing`() {
        waiting.add(FadeMixer.Taken(ShortArray(0), timeUs = 1))

        assertTrue(waiting.isEmpty())
    }

    private fun takeAll(buffer: ByteBuffer, timeUs: Long): Boolean {
        written += timeUs
        buffer.position(buffer.limit())
        return true
    }
}
