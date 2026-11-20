/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.siliconcircuits.tarangini.core.playback.stream.isStreamAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Moves on when a file has less sound than it states.
 *
 * A file that was cut short, such as a ringtone made from a song, can keep the header of the whole
 * song. The player believes the header, so after the last sound it stays "playing" in silence
 * until the stated end, with a position that creeps. Nothing fails, so no error tells about it.
 *
 * So the position is watched while a file plays. When it stays well behind the clock, the sound
 * has ended: the next item is played, or playback stops at the start of the last one.
 *
 * A place inside that silence can also be asked for, by a seek or by the place that was saved when
 * the app closed. The file has nothing there, so the player fails. That is treated the same way.
 *
 * A stream is left alone. It has no stated end, and its pauses are handled as buffering.
 */
internal class SkipSilentEnd(private val player: Player, private val scope: CoroutineScope) : Player.Listener {
    private var watch: Job? = null

    override fun onIsPlayingChanged(isPlaying: Boolean) = watchAgain()

    // A seek or a new item moves the position by itself, so the watch starts again from there.
    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) = watchAgain()

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = watchAgain()

    override fun onPlayerError(error: PlaybackException) {
        val pastTheEnd = error.errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE
        // Only a place inside the file can be after its last sound. A file that fails at its start
        // is left alone, so a queue of such files does not go round for ever.
        if (pastTheEnd && isFile() && player.currentPosition > 0) {
            moveOn()
            player.prepare()
        }
    }

    private fun isFile() = player.currentMediaItem?.localConfiguration?.uri?.isStreamAddress() == false

    private fun watchAgain() {
        watch?.cancel()
        watch = if (player.isPlaying && isFile()) scope.launch { watchPosition() } else null
    }

    private suspend fun watchPosition() {
        var slowWindows = 0
        while (slowWindows < SLOW_WINDOWS) {
            val before = player.currentPosition
            delay(WATCH_MS)
            // The player can be released while this waits. Then nothing is asked of it any more.
            if (!player.isPlaying) return
            val moved = player.currentPosition - before
            val left = if (player.duration == C.TIME_UNSET) 0 else player.duration - player.currentPosition
            val isSlow = moved < WATCH_MS * player.playbackParameters.speed * SLOWEST_SHARE && left > WATCH_MS
            // One slow window can be a new audio route that takes a second to start. Silence stays slow.
            slowWindows = if (isSlow) slowWindows + 1 else 0
        }
        moveOn()
    }

    private fun moveOn() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            player.pause()
            player.seekToDefaultPosition()
        }
    }
}

/** How long the position is watched before it is judged. */
internal const val WATCH_MS = 2_000L

/** So many slow windows in a row mean silence. Together they are the silence a person hears at most. */
internal const val SLOW_WINDOWS = 2

// Sound that plays moves the position with the clock. In the silence after the last sound it moved
// at less than half of that on a Pixel 6a, so three quarters tells the two apart safely.
private const val SLOWEST_SHARE = 0.75f
