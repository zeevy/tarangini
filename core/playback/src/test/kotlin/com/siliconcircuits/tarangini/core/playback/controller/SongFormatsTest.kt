/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import com.siliconcircuits.tarangini.core.domain.model.SongFormat
import com.siliconcircuits.tarangini.core.playback.player.readTestFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The file readers and track groups of Media3 are API that may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class SongFormatsTest {
    @Test
    fun `an MP3 test file gives its type, bit rate, sample rate and channels`() {
        val format = readTestFormat("gapless/with-delay-header-1.mp3", Mp3Extractor()).toSongFormat()

        // The frames say 64 kbit/s. Media3 gives the bytes over the real length, 65,201 bit/s: this file
        // is 2 seconds long, so the encoder silence cut from the length counts. On a song of minutes it does not.
        assertEquals(SongFormat("MP3", 65, 44_100, 1), format)
    }

    @Test
    fun `an M4A test file gives AAC and its average bit rate, cut to whole kbit`() {
        val format = readTestFormat("gapless/with-edit-list-skip-1.m4a", Mp4Extractor(DefaultSubtitleParserFactory()))
            .toSongFormat()

        // The file states an average of 64,732 bit/s; Media3 works out 64,095 from the samples.
        assertEquals(SongFormat("AAC", 64, 44_100, 1), format)
    }

    @Test
    fun `a part the file does not tell is left out`() {
        val format = Format.Builder().setSampleMimeType(
            MimeTypes.AUDIO_MPEG,
        ).setSampleRate(48_000).build().toSongFormat()

        assertEquals(SongFormat("MP3", null, 48_000, null), format)
    }

    @Test
    fun `a bit rate under one kbit is left out, not shown as 0`() {
        assertNull(Format.Builder().setAverageBitrate(800).build().toSongFormat().bitrateKbps)
    }

    @Test
    fun `a sound format the player does not name has no type`() {
        assertNull(Format.Builder().setSampleMimeType(MimeTypes.AUDIO_AC3).build().toSongFormat().codec)
    }

    @Test
    fun `the format comes from the picked sound track only`() {
        val picked = Format.Builder().setSampleMimeType(MimeTypes.AUDIO_FLAC).setChannelCount(2).build()
        val other = Format.Builder().setSampleMimeType(MimeTypes.AUDIO_MPEG).build()
        val tracks = Tracks(
            listOf(
                Tracks.Group(TrackGroup(other), false, intArrayOf(C.FORMAT_HANDLED), booleanArrayOf(false)),
                Tracks.Group(TrackGroup(picked), false, intArrayOf(C.FORMAT_HANDLED), booleanArrayOf(true)),
            ),
        )

        assertEquals(SongFormat("FLAC", channels = 2), tracks.playingFormat())
        assertNull(Tracks.EMPTY.playingFormat())
    }
}
