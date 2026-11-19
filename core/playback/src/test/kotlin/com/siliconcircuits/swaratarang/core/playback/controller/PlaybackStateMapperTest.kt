/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.controller

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.siliconcircuits.swaratarang.core.domain.model.PlaybackError
import com.siliconcircuits.swaratarang.core.domain.model.PlaybackItem
import com.siliconcircuits.swaratarang.core.domain.model.PlaybackState
import com.siliconcircuits.swaratarang.core.domain.model.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The fake player is built on API that Media3 may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaybackStateMapperTest {
    private val player = FakePlayer()

    private fun twoSongs() = listOf(FakePlayer.queued("first"), FakePlayer.queued("second"))

    @Test
    fun `a player with nothing loaded gives the empty state`() {
        assertEquals(PlaybackState(), player.toPlaybackState())
    }

    @Test
    fun `the queue and the current place come from the player`() {
        player.report { setPlaylist(twoSongs()).setCurrentMediaItemIndex(1) }

        val state = player.toPlaybackState()

        assertEquals(listOf("first", "second"), state.queue.map { it.id })
        assertEquals(1, state.currentIndex)
        assertEquals("second", state.currentItem?.id)
    }

    @Test
    fun `the current item shows the tags the player read from the file`() {
        val tags = MediaMetadata.Builder()
            .setTitle("Title")
            .setArtist("Artist")
            .setAlbumTitle("Album")
            .setArtworkUri(Uri.parse("content://media/external/audio/albumart/3"))
            .build()
        val song = MediaItem.Builder().setMediaId("first").build()
        player.report { setPlaylist(listOf(FakePlayer.queued(song, tags), FakePlayer.queued("second"))) }

        val state = player.toPlaybackState()

        assertEquals(
            PlaybackItem("first", "Title", "Artist", "Album", "content://media/external/audio/albumart/3"),
            state.currentItem,
        )
        assertEquals(PlaybackItem("second"), state.queue[1])
    }

    @Test
    fun `an item keeps the texts it was loaded with when the player has read no tags`() {
        val loadedWith = MediaMetadata.Builder().setTitle("Loaded title").setArtist("Loaded artist").build()
        val song = MediaItem.Builder().setMediaId("first").setMediaMetadata(loadedWith).build()
        player.report { setPlaylist(listOf(FakePlayer.queued("zero"), FakePlayer.queued(song))) }

        assertEquals(PlaybackItem("first", "Loaded title", "Loaded artist"), player.toPlaybackState().queue[1])
    }

    @Test
    fun `playing, paused and buffering are told apart`() {
        player.report { setPlaylist(twoSongs()).setPlaybackState(Player.STATE_READY) }
        assertFalse(player.toPlaybackState().isPlaying)

        player.report { setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
        assertTrue(player.toPlaybackState().isPlaying)
        assertFalse(player.toPlaybackState().isBuffering)

        player.report { setPlaybackState(Player.STATE_BUFFERING) }
        assertFalse(player.toPlaybackState().isPlaying)
        assertTrue(player.toPlaybackState().isBuffering)
        assertTrue(player.toPlaybackState().playWhenReady)
    }

    @Test
    fun `position and length come from the current item`() {
        val song = MediaItem.Builder().setMediaId("first").build()
        player.report {
            setPlaylist(listOf(FakePlayer.queued(song, durationMs = 180_000)))
                .setPlaybackState(Player.STATE_READY)
        }
        player.positionMs = 42_000

        val state = player.toPlaybackState()

        assertEquals(42_000L, state.positionMs)
        assertEquals(180_000L, state.durationMs)
    }

    @Test
    fun `an item with no known length has no duration`() {
        player.report { setPlaylist(twoSongs()).setPlaybackState(Player.STATE_READY) }

        assertNull(player.toPlaybackState().durationMs)
    }

    @Test
    fun `shuffle and each repeat mode are shown`() {
        player.report { setShuffleModeEnabled(true).setRepeatMode(Player.REPEAT_MODE_ONE) }
        assertTrue(player.toPlaybackState().shuffle)
        assertEquals(RepeatMode.ONE, player.toPlaybackState().repeat)

        player.report { setShuffleModeEnabled(false).setRepeatMode(Player.REPEAT_MODE_ALL) }
        assertFalse(player.toPlaybackState().shuffle)
        assertEquals(RepeatMode.ALL, player.toPlaybackState().repeat)

        player.report { setRepeatMode(Player.REPEAT_MODE_OFF) }
        assertEquals(RepeatMode.OFF, player.toPlaybackState().repeat)
    }

    @Test
    fun `each kind of player error becomes an error a screen can explain`() {
        val expected = mapOf(
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND to PlaybackError.FILE_NOT_AVAILABLE,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION to PlaybackError.FILE_NOT_AVAILABLE,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED to PlaybackError.NETWORK,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT to PlaybackError.NETWORK,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS to PlaybackError.NETWORK,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE to PlaybackError.CANNOT_PLAY,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED to PlaybackError.CANNOT_PLAY,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED to PlaybackError.CANNOT_PLAY,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED to PlaybackError.CANNOT_PLAY,
            PlaybackException.ERROR_CODE_UNSPECIFIED to PlaybackError.UNKNOWN,
        )

        expected.forEach { (code, error) ->
            player.report { setPlaylist(twoSongs()).setPlayerError(PlaybackException("failed", null, code)) }

            assertEquals("error code $code", error, player.toPlaybackState().error)
        }
    }

    @Test
    fun `a player that did not fail has no error`() {
        player.report { setPlaylist(twoSongs()).setPlaybackState(Player.STATE_READY) }

        assertNull(player.toPlaybackState().error)
    }
}
