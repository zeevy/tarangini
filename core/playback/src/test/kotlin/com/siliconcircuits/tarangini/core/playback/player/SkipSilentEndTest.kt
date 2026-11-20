/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** A player whose position, length and play state are set by the test. */
// A player that wraps another one is API of Media3 that may still change.
@OptIn(UnstableApi::class)
private class ClockedPlayer(player: ExoPlayer) : ForwardingPlayer(player) {
    var position = 0L
    var length = 138_000L
    var playing = true
    var pauses = 0
    var seeksToStart = 0
    var prepares = 0

    override fun getCurrentPosition(): Long = position

    override fun getDuration(): Long = length

    override fun isPlaying(): Boolean = playing

    override fun prepare() {
        prepares++
    }

    override fun seekToDefaultPosition() {
        seeksToStart++
    }

    override fun pause() {
        pauses++
        playing = false
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SkipSilentEndTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val real: ExoPlayer = ExoPlayer.Builder(context).build()
    private val player = ClockedPlayer(real)
    private val scope = TestScope()
    private val sound = LastSound(now = { scope.testScheduler.currentTime })
    private val skip = SkipSilentEnd(player, sound, scope)

    @After
    fun release() = real.release()

    private fun queue(vararg addresses: String) = real.setMediaItems(addresses.map(MediaItem::fromUri))

    private val twoSongs = arrayOf("content://media/external/audio/media/1", "content://media/external/audio/media/2")

    /** Lets [ms] pass, with sound from the decoder every 100 ms when [withSound] is true. */
    private fun pass(ms: Long, withSound: Boolean) {
        repeat((ms / STEP_MS).toInt()) {
            if (withSound) sound.heard()
            scope.advanceTimeBy(STEP_MS)
            scope.runCurrent()
        }
    }

    @Test
    fun `a file that gives no more sound before its stated end gives way to the next item`() {
        queue(*twoSongs)
        skip.onIsPlayingChanged(true)

        pass(60_000, withSound = true)
        pass(SILENCE_MS - CHECK_MS, withSound = false)
        assertEquals(0, real.currentMediaItemIndex)

        pass(2 * CHECK_MS, withSound = false)
        assertEquals(1, real.currentMediaItemIndex)
    }

    @Test
    fun `a short gap in the sound is not the end`() {
        queue(*twoSongs)
        skip.onIsPlayingChanged(true)

        repeat(5) {
            pass(SILENCE_MS - CHECK_MS, withSound = false)
            pass(CHECK_MS, withSound = true)
        }

        assertEquals(0, real.currentMediaItemIndex)
    }

    @Test
    fun `the last item stops at its start`() {
        queue("content://media/external/audio/media/1")
        skip.onIsPlayingChanged(true)

        pass(SILENCE_MS + CHECK_MS, withSound = false)

        assertEquals(1, player.pauses)
        assertEquals(1, player.seeksToStart)
    }

    @Test
    fun `sound from before a long pause does not count`() {
        queue(*twoSongs)
        player.playing = false
        skip.onIsPlayingChanged(false)
        pass(600_000, withSound = false)

        player.playing = true
        skip.onIsPlayingChanged(true)
        pass(SILENCE_MS - CHECK_MS, withSound = false)
        pass(60_000, withSound = true)

        assertEquals(0, real.currentMediaItemIndex)
    }

    @Test
    fun `the last seconds of a file are left to end by themselves`() {
        queue(*twoSongs)
        player.position = player.length - SILENCE_MS
        skip.onIsPlayingChanged(true)

        pass(2 * SILENCE_MS, withSound = false)

        assertEquals(0, real.currentMediaItemIndex)
    }

    @Test
    fun `a seek starts the count again`() {
        queue(*twoSongs)
        skip.onIsPlayingChanged(true)
        val place = Player.PositionInfo(null, 0, null, null, 0, 0, 0, 0, 0)

        pass(SILENCE_MS - CHECK_MS, withSound = false)
        skip.onPositionDiscontinuity(place, place, Player.DISCONTINUITY_REASON_SEEK)
        pass(SILENCE_MS - CHECK_MS, withSound = false)

        assertEquals(0, real.currentMediaItemIndex)
    }

    @Test
    fun `a player that was released is not touched`() {
        queue(*twoSongs)
        skip.onIsPlayingChanged(true)
        player.playing = false

        pass(2 * SILENCE_MS, withSound = false)

        assertEquals(0, real.currentMediaItemIndex)
        assertEquals(0, player.pauses)
    }

    @Test
    fun `a paused player and a stream are not watched`() {
        queue("https://radio.example/live", "content://media/external/audio/media/2")
        skip.onIsPlayingChanged(true)
        pass(2 * SILENCE_MS, withSound = false)
        assertEquals(0, real.currentMediaItemIndex)

        queue(*twoSongs)
        player.playing = false
        skip.onIsPlayingChanged(false)
        pass(2 * SILENCE_MS, withSound = false)
        assertEquals(0, real.currentMediaItemIndex)
    }

    @Test
    fun `a place after the last sound of a file gives way to the next item`() {
        queue(*twoSongs)
        player.position = 118_000

        skip.onPlayerError(
            PlaybackException("past the end", null, PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE),
        )

        assertEquals(1, real.currentMediaItemIndex)
        assertEquals(1, player.prepares)
    }

    @Test
    fun `a failure at the start of a file, another failure and a failed stream are left to others`() {
        queue(*twoSongs)
        skip.onPlayerError(PlaybackException("range", null, PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE))
        player.position = 118_000
        skip.onPlayerError(PlaybackException("gone", null, PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND))
        queue("https://radio.example/live", "content://media/external/audio/media/2")
        skip.onPlayerError(PlaybackException("range", null, PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE))

        assertEquals(0, real.currentMediaItemIndex)
        assertEquals(0, player.prepares)
    }
}

private const val STEP_MS = 100L
