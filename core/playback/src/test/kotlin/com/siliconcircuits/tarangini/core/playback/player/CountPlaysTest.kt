/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountPlaysTest {
    private val counted = mutableListOf<String>()

    private fun TestScope.counter() = CountPlays(backgroundScope) { counted += it }

    private fun TestScope.hear(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun `a song counts once it was heard for ten seconds, and only once`() = runTest {
        val counter = counter()
        counter.itemStarted("one")
        counter.playing(true)

        hear(9_000)
        val early = counted.toList()
        hear(60_000)

        assertEquals(emptyList<String>(), early)
        assertEquals(listOf("one"), counted)
    }

    @Test
    fun `a song skipped in its first seconds does not count`() = runTest {
        val counter = counter()
        counter.itemStarted("one")
        counter.playing(true)
        hear(5_000)

        counter.itemStarted("two")
        hear(6_000)

        assertEquals(emptyList<String>(), counted)
        hear(4_000)
        assertEquals(listOf("two"), counted)
    }

    @Test
    fun `time in pause does not count, and what was heard before the pause does`() = runTest {
        val counter = counter()
        counter.itemStarted("one")
        counter.playing(true)
        hear(6_000)

        counter.playing(false)
        hear(60_000)
        val paused = counted.toList()
        counter.playing(true)
        hear(4_000)

        assertEquals(emptyList<String>(), paused)
        assertEquals(listOf("one"), counted)
    }

    @Test
    fun `a song that starts again counts again, and nothing counts while nothing is loaded`() = runTest {
        val counter = counter()
        counter.itemStarted("one")
        counter.playing(true)
        hear(10_000)
        counter.itemStarted("one")
        hear(10_000)

        counter.itemStarted(null)
        hear(60_000)

        assertEquals(listOf("one", "one"), counted)
    }

    @Test
    fun `a pause in the moment a song counts does not lose the play`() = runTest {
        lateinit var counter: CountPlays
        counter = CountPlays(backgroundScope) {
            counter.playing(false)
            delay(50)
            counted += it
        }
        counter.itemStarted("one")
        counter.playing(true)

        hear(11_000)

        assertEquals(listOf("one"), counted)
    }
}
