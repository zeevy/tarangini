/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
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
 * until the stated end. Nothing fails, so no error tells about it, and on some files the position
 * even moves on at the speed of the clock.
 *
 * So what is watched is the sound itself: [LastSound] says when the decoder last gave any. When a
 * file that plays has given none for a few seconds, its sound has ended: the next item is played,
 * or playback stops at the start of the last one.
 *
 * A place inside that silence can also be asked for, by a seek or by the place that was saved when
 * the app closed. The file has nothing there, so the player fails. That is treated the same way.
 *
 * A stream is left alone. It has no stated end, and its pauses are handled as buffering.
 */
internal class SkipSilentEnd(
    private val player: Player,
    private val sound: LastSound,
    private val scope: CoroutineScope,
) : Player.Listener {
    private var watch: Job? = null

    override fun onIsPlayingChanged(isPlaying: Boolean) = watchAgain()

    // After a seek or at a new item the decoder starts afresh, so the count starts afresh too.
    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) = watchAgain()

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
        // The last sound may be from before a long pause.
        sound.heard()
        watch = if (player.isPlaying && isFile()) scope.launch { watchSound() } else null
    }

    private suspend fun watchSound() {
        while (true) {
            delay(CHECK_MS)
            // The player can be released while this waits. Then nothing is asked of it any more.
            if (!player.isPlaying) return
            val left = if (player.duration == C.TIME_UNSET) 0 else player.duration - player.currentPosition
            // Close to the stated end the player ends the item by itself.
            if (sound.silentForMs() >= SILENCE_MS && left > SILENCE_MS) return moveOn()
        }
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

/**
 * A file that plays and gave no sound for this long has none left. The sound that was already in
 * the output buffer still plays for part of that time.
 */
internal const val SILENCE_MS = 3_000L

/** How often the time since the last sound is looked at. */
internal const val CHECK_MS = 1_000L
