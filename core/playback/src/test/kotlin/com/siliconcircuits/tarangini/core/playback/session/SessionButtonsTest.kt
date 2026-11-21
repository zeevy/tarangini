/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SessionButtonsTest {
    private val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
    private val shown = mutableListOf<List<CommandButton>>()
    private val buttons = SessionButtons(player, { it.name }, shown::add).also(player::addListener)
    private val heart = CommandButton.Builder(CommandButton.ICON_HEART_UNFILLED)
        .setDisplayName("Heart")
        .setSessionCommand(FavouriteButton.COMMAND)
        .build()

    private fun names() = shown.last().map { it.displayName.toString() }

    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    private fun load() {
        player.setMediaItem(
            MediaItem.Builder().setMediaId("one").setUri("content://media/external/audio/media/1").build(),
        )
        settle()
    }

    @After
    fun release() = player.release()

    @Test
    fun `there is no button while nothing is loaded`() {
        buttons.heart(listOf(heart))

        assertEquals(emptyList<String>(), names())
    }

    @Test
    fun `a loaded item shows the heart, shuffle and repeat, each named by what a press does`() {
        load()
        buttons.heart(listOf(heart))

        assertEquals(listOf("Heart", "SHUFFLE_ON", "REPEAT_ALL"), names())
        assertEquals(
            listOf(SessionButtons.SHUFFLE, SessionButtons.REPEAT),
            shown.last().drop(1).map {
                it.sessionCommand
            },
        )
    }

    @Test
    fun `a press of shuffle switches it, and the button shows the new state`() {
        load()

        assertTrue(buttons.press(SessionButtons.SHUFFLE.customAction))
        settle()

        assertTrue(player.shuffleModeEnabled)
        assertEquals(listOf("SHUFFLE_OFF", "REPEAT_ALL"), names())
        assertEquals(CommandButton.ICON_SHUFFLE_ON, shown.last().first().icon)
    }

    @Test
    fun `a press of repeat goes from off to all, to one, and back to off`() {
        load()
        val modes = (1..3).map {
            buttons.press(SessionButtons.REPEAT.customAction)
            settle()
            player.repeatMode to names().last()
        }

        assertEquals(
            listOf(
                Player.REPEAT_MODE_ALL to "REPEAT_ONE",
                Player.REPEAT_MODE_ONE to "REPEAT_OFF",
                Player.REPEAT_MODE_OFF to "REPEAT_ALL",
            ),
            modes,
        )
    }

    @Test
    fun `a change made on the phone shows on the buttons`() {
        load()
        player.shuffleModeEnabled = true
        player.repeatMode = Player.REPEAT_MODE_ONE
        settle()

        assertEquals(listOf("SHUFFLE_OFF", "REPEAT_OFF"), names())
    }

    @Test
    fun `another command is not taken`() {
        assertFalse(buttons.press("com.example.OTHER"))
        assertFalse(buttons.press(FavouriteButton.COMMAND.customAction))
    }
}
