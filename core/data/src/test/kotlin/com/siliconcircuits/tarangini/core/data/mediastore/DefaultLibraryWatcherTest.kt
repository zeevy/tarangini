/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import com.siliconcircuits.tarangini.core.data.work.RescanSchedule
import com.siliconcircuits.tarangini.core.domain.library.LibraryScanner
import com.siliconcircuits.tarangini.core.domain.library.ScanOutcome
import com.siliconcircuits.tarangini.core.domain.library.ScanState
import com.siliconcircuits.tarangini.core.domain.library.ScanSummary
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLibraryWatcherTest {
    /** A scanner that counts. A test can hold a scan open with [gate] or make it fail with [failure]. */
    private class CountingScanner : LibraryScanner {
        override val state = MutableStateFlow<ScanState>(ScanState.Idle)
        var scanCount = 0
        var gate: CompletableDeferred<Unit>? = null
        var failure: Exception? = null

        override suspend fun scan(): ScanOutcome {
            scanCount++
            gate?.await()
            failure?.let { throw it }
            return ScanOutcome.Success(ScanSummary(added = 0, updated = 0, removed = 0, unchanged = 0))
        }
    }

    private val scanner = CountingScanner()
    private val source = FakeAudioFileSource()
    private val schedule = object : RescanSchedule {
        var asked = 0

        override fun waitForChange() {
            asked++
        }

        override fun waitForNextChange() = error("Only the job asks for the next one")
    }

    private fun TestScope.started() = DefaultLibraryWatcher(scanner, source, schedule, backgroundScope).also {
        it.start()
        runCurrent()
    }

    @Test
    fun `the start scans once and asks for the job that scans while the app is closed`() = runTest {
        started()

        assertEquals(1, scanner.scanCount)
        assertEquals(1, schedule.asked)
    }

    @Test
    fun `a change of the files gives a scan after the phone was quiet for a moment, and so does the next one`() =
        runTest {
            started()

            source.changed.emit(Unit)
            advanceTimeBy(1.seconds)
            assertEquals(1, scanner.scanCount)
            advanceTimeBy(2.seconds)
            assertEquals(2, scanner.scanCount)

            source.changed.emit(Unit)
            advanceTimeBy(3.seconds)
            assertEquals(3, scanner.scanCount)
        }

    @Test
    fun `ten quick changes give one scan, not ten`() = runTest {
        started()

        repeat(10) {
            source.changed.emit(Unit)
            advanceTimeBy(0.5.seconds)
        }
        advanceTimeBy(10.seconds)

        assertEquals(2, scanner.scanCount)
    }

    @Test
    fun `changes that never stop still give a scan`() = runTest {
        started()

        repeat(40) {
            source.changed.emit(Unit)
            advanceTimeBy(1.seconds)
        }

        assertEquals(2, scanner.scanCount)
    }

    @Test
    fun `a change that comes while a scan runs gives one more scan after it`() = runTest {
        started()
        val gate = CompletableDeferred<Unit>()
        scanner.gate = gate
        source.changed.emit(Unit)
        advanceTimeBy(3.seconds)
        assertEquals(2, scanner.scanCount)

        repeat(3) { source.changed.emit(Unit) }
        runCurrent()
        scanner.gate = null
        gate.complete(Unit)
        advanceTimeBy(3.seconds)

        assertEquals(3, scanner.scanCount)
    }

    @Test
    fun `a scan that fails does not stop the watching`() = runTest {
        scanner.failure = IllegalStateException("disk full")
        started()

        scanner.failure = null
        source.changed.emit(Unit)
        advanceTimeBy(3.seconds)

        assertEquals(2, scanner.scanCount)
    }
}
