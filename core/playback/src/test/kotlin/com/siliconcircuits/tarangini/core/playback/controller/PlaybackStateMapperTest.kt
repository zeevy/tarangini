/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.model.QueueSourceKind
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.model.SongFormat
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
    fun `the format of the current item comes from its picked sound track`() {
        val format = Format.Builder().setSampleMimeType(MimeTypes.AUDIO_MPEG).setAverageBitrate(320_000)
            .setSampleRate(44_100).setChannelCount(2).build()
        val tracks = Tracks(
            listOf(Tracks.Group(TrackGroup(format), false, intArrayOf(C.FORMAT_HANDLED), booleanArrayOf(true))),
        )
        val song = FakePlayer.queued("first").buildUpon().setTracks(tracks).build()
        player.report { setPlaylist(listOf(song)).setCurrentMediaItemIndex(0) }

        assertEquals(SongFormat("MP3", 320, 44_100, 2), player.toPlaybackState().format)
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
    fun `each item keeps the length the library gave it`() {
        val known = MediaItem.Builder().setMediaId("first")
            .setMediaMetadata(MediaMetadata.Builder().setDurationMs(185_000).build()).build()
        player.report { setPlaylist(listOf(FakePlayer.queued(known), FakePlayer.queued("second"))) }

        assertEquals(listOf(185_000L, null), player.toPlaybackState().queue.map { it.durationMs })
    }

    @Test
    fun `with shuffle on the play order follows the timeline from the current item, without repeat`() {
        val shuffled = ReversedTimeline(windows = 4)

        assertEquals(listOf(2, 1, 0), shuffled.playOrderFrom(2))
        assertEquals(listOf(3, 2, 1, 0), shuffled.playOrderFrom(3))
    }

    @Test
    fun `with shuffle off there is no play order`() {
        player.report { setPlaylist(twoSongs()) }

        assertEquals(null, player.toPlaybackState().playOrder)
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
    fun `an item the player has not opened yet has the length it was loaded with`() {
        val loadedWith = MediaMetadata.Builder().setDurationMs(185_000).build()
        val song = MediaItem.Builder().setMediaId("first").setMediaMetadata(loadedWith).build()
        player.report { setPlaylist(listOf(FakePlayer.queued(song))) }

        assertEquals(185_000L, player.toPlaybackState().durationMs)
    }

    @Test
    fun `a live stream has no length, also when the player reports the length of the part it keeps`() {
        val station = MediaItem.Builder().setMediaId("radio").build()
        player.report { setPlaylist(listOf(FakePlayer.queued(station, durationMs = 30_000, live = true))) }

        assertNull(player.toPlaybackState().durationMs)
    }

    @Test
    fun `an item with an http or https address is a stream, a file of the phone is not`() {
        val items = listOf(
            "http://radio.example/stream",
            "HTTPS://radio.example/live.m3u8",
            "content://media/external/audio/media/1",
        )
            .map { MediaItem.Builder().setMediaId(it).setUri(it).build() }
        player.report { setPlaylist(items.map { FakePlayer.queued(it) }) }

        assertEquals(listOf(true, true, false), player.toPlaybackState().queue.map { it.isStream })
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
    fun `the source of the queue comes from the metadata this app wrote, and other metadata gives none`() {
        val practice = QueueSource(QueueSourceKind.PLAYLIST, "Practice")
        player.report { setPlaylist(twoSongs()).setPlaylistMetadata(practice.toPlaylistMetadata()) }
        assertEquals(practice, player.toPlaybackState().source)

        player.report { setPlaylistMetadata(MediaMetadata.Builder().setTitle("A list of another app").build()) }
        assertNull(player.toPlaybackState().source)
    }

    @Test
    fun `an empty player plays from no list, even with a name left in its metadata`() {
        val practice = QueueSource(QueueSourceKind.PLAYLIST, "Practice")
        player.report { setPlaylistMetadata(practice.toPlaylistMetadata()) }

        assertNull(player.toPlaybackState().source)
    }

    @Test
    fun `a player that did not fail has no error`() {
        player.report { setPlaylist(twoSongs()).setPlaybackState(Player.STATE_READY) }

        assertNull(player.toPlaybackState().error)
    }
}

/** A timeline of [windows] items whose shuffle order plays them from the last to the first. */
private class ReversedTimeline(private val windows: Int) : Timeline() {
    override fun getWindowCount() = windows

    override fun getNextWindowIndex(windowIndex: Int, repeatMode: Int, shuffleModeEnabled: Boolean) =
        if (shuffleModeEnabled) windowIndex - 1 else (windowIndex + 1).takeIf { it < windows } ?: C.INDEX_UNSET

    override fun getWindow(windowIndex: Int, window: Window, defaultPositionProjectionUs: Long) = window

    override fun getPeriodCount() = windows

    override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean) = period

    override fun getIndexOfPeriod(uid: Any) = C.INDEX_UNSET

    override fun getUidOfPeriod(periodIndex: Int): Any = periodIndex
}
