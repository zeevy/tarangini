/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.sleep

import androidx.media3.exoplayer.ExoPlayer
import com.siliconcircuits.tarangini.core.domain.model.SleepState
import com.siliconcircuits.tarangini.core.domain.playback.SleepTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultSleepTimerTest {
    private val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build().apply { playWhenReady = true }

    private fun TestScope.timer() = DefaultSleepTimer(backgroundScope).also { it.attach(player) }

    @After
    fun release() = player.release()

    @Test
    fun `playback pauses after the set time, and the time left counts down`() = runTest {
        val timer = timer()

        timer.stopAfter(minutes = 2)
        advanceTimeBy(60_000)
        runCurrent()
        val halfWay = timer.state.value
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(SleepState.Timed(60_000), halfWay)
        assertEquals(SleepState.Off, timer.state.value)
        assertFalse(player.playWhenReady)
    }

    @Test
    fun `the last seconds fade out, and the volume is back after the pause`() = runTest {
        val timer = timer()

        timer.stopAfter(minutes = 1)
        advanceTimeBy(57_000)
        runCurrent()
        val fading = player.volume
        advanceTimeBy(3_000)
        runCurrent()

        assertTrue(fading > 0f && fading < 1f)
        assertTrue(player.playWhenReady.not())
        assertEquals(1f, player.volume)
    }

    @Test
    fun `a timer can be changed and cancelled, and playback goes on`() = runTest {
        val timer = timer()

        timer.stopAfter(minutes = 1)
        advanceTimeBy(58_000)
        timer.stopAfter(minutes = 10)
        advanceTimeBy(120_000)
        runCurrent()
        val changed = timer.state.value
        timer.cancel()
        advanceTimeBy(600_000)

        assertEquals(SleepState.Timed(480_000), changed)
        assertEquals(SleepState.Off, timer.state.value)
        assertTrue(player.playWhenReady)
        assertEquals(1f, player.volume)
    }

    @Test
    fun `stop at the end of the track asks the player to pause there, and a cancel takes it back`() = runTest {
        val timer = timer()

        timer.stopAtEndOfTrack()
        val asked = player.pauseAtEndOfMediaItems to timer.state.value
        timer.cancel()

        assertEquals(true to SleepState.EndOfTrack, asked)
        assertFalse(player.pauseAtEndOfMediaItems)
        assertEquals(SleepState.Off, timer.state.value)
    }

    @Test
    fun `a time is held between one minute and twelve hours`() = runTest {
        val timer = timer()

        timer.stopAfter(minutes = 0)
        runCurrent()
        val shortest = timer.state.value
        timer.stopAfter(minutes = 100_000)
        runCurrent()

        assertEquals(SleepState.Timed(60_000), shortest)
        assertEquals(SleepState.Timed(SleepTimer.MAX_MINUTES * 60_000L), timer.state.value)
    }

    @Test
    fun `when the service ends the timer ends with it`() = runTest {
        val timer = timer()
        timer.stopAfter(minutes = 5)
        runCurrent()

        timer.detach()
        advanceTimeBy(600_000)

        assertEquals(SleepState.Off, timer.state.value)
        assertTrue(player.playWhenReady)
    }
}
