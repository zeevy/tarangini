/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.siliconcircuits.tarangini.core.playback.controller.FakePlayer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SkipMissingFileTest {
    private val player = FakePlayer().also { player ->
        player.addListener(SkipMissingFile(player))
        player.setMediaItems(listOf("a", "b", "c").map { MediaItem.Builder().setMediaId(it).build() })
        player.prepare()
        player.report()
    }

    private fun failWith(code: Int) = player.report {
        setPlayerError(PlaybackException("test", null, code))
        setPlaybackState(Player.STATE_IDLE)
    }

    @Test
    fun `a file that is gone is skipped and the next item is prepared`() {
        failWith(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals("prepare", player.commands.last())
    }

    @Test
    fun `another error does not skip`() {
        failWith(PlaybackException.ERROR_CODE_DECODING_FAILED)

        assertEquals(0, player.currentMediaItemIndex)
    }

    @Test
    fun `the last item is not skipped`() {
        player.seekTo(2, 0)
        player.report()

        failWith(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)

        assertEquals(2, player.currentMediaItemIndex)
    }

    @Test
    fun `a queue of files that are all gone stops after one round, also with repeat on`() {
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.report()

        repeat(10) { failWith(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND) }

        assertEquals(3, player.commands.count { it.startsWith("seek") })
    }
}
