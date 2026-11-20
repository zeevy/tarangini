/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Moving the test clock by hand is API of the coroutines test library that may still change.
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryChangeNoticesTest {
    private val library = FakeLibraryRepository()
    private val told = mutableListOf<String>()

    @Test
    fun `nothing is told at the start, and each open list is told once after the library changed`() = runTest {
        val notices = LibraryChangeNotices(library, backgroundScope, { listOf("library", "songs") }, told::add)
        notices.start()
        pass()
        assertTrue(told.isEmpty())

        library.version.value++
        pass()

        assertEquals(listOf("library", "songs"), told)
    }

    @Test
    fun `many changes in a short time are told once, after the last one`() = runTest {
        val notices = LibraryChangeNotices(library, backgroundScope, { listOf("songs") }, told::add)
        notices.start()
        runCurrent()

        repeat(5) {
            library.version.value++
            advanceTimeBy(HALF_A_SECOND)
        }
        assertTrue(told.isEmpty())
        pass()

        assertEquals(listOf("songs"), told)
    }

    @Test
    fun `nothing is told after the end`() = runTest {
        val notices = LibraryChangeNotices(library, backgroundScope, { listOf("songs") }, told::add)
        notices.start()
        runCurrent()

        notices.end()
        library.version.value++
        pass()

        assertTrue(told.isEmpty())
    }

    /** Lets more time pass than the wait after a change. The watch runs in the background, which "until idle" skips. */
    private fun TestScope.pass() {
        advanceTimeBy(LONGER_THAN_THE_WAIT)
        runCurrent()
    }

    private companion object {
        const val HALF_A_SECOND = 500L
        const val LONGER_THAN_THE_WAIT = 5_000L
    }
}
