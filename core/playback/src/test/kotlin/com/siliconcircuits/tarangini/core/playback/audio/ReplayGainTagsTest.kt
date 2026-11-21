/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.CommentFrame
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import com.siliconcircuits.tarangini.core.domain.audio.ReplayGain
import com.siliconcircuits.tarangini.core.playback.lyrics.TagReader
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The frame classes are API of the extractors that may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class ReplayGainTagsTest {
    private fun tagsOf(file: String): ReplayGain? {
        val bytes = requireNotNull(javaClass.classLoader?.getResourceAsStream(file)).use { it.readBytes() }
        // The tag reader runs the same extractors as the player, so the player sees the same frames.
        return replayGainOf(TagReader { ByteArrayInputStream(bytes) }.metadata())
    }

    @Test
    fun `the tags of an MP3 file are read from its TXXX frames, in any case`() {
        assertEquals(
            ReplayGain(trackGainDb = 3.2f, trackPeak = 0.5f, albumGainDb = -1.5f, albumPeak = 0.75f),
            tagsOf("replaygain/with-replaygain.mp3"),
        )
    }

    @Test
    fun `the tags of an M4A file are read from its iTunes atoms`() {
        assertEquals(
            ReplayGain(trackGainDb = -7.5f, trackPeak = 0.891251f, albumGainDb = -6f, albumPeak = 0.977f),
            tagsOf("replaygain/with-replaygain.m4a"),
        )
    }

    @Test
    fun `a file with no ReplayGain tags has none`() {
        assertNull(tagsOf("lyrics/with-lyrics.mp3"))
        assertNull(tagsOf("lyrics/with-lyrics.m4a"))
        assertNull(replayGainOf(null))
    }

    @Test
    fun `other frames, peaks alone and the second tag of a name are left out`() {
        val metadata = Metadata(
            CommentFrame("eng", "REPLAYGAIN_TRACK_GAIN", "-9 dB"),
            TextInformationFrame("TIT2", "REPLAYGAIN_TRACK_GAIN", listOf("-9 dB")),
            TextInformationFrame("TXXX", "REPLAYGAIN_TRACK_GAIN", listOf("-2 dB")),
            InternalFrame("com.apple.iTunes", "REPLAYGAIN_TRACK_GAIN", "-8 dB"),
        )

        assertEquals(ReplayGain(trackGainDb = -2f), replayGainOf(metadata))
        assertNull(replayGainOf(Metadata(TextInformationFrame("TXXX", "REPLAYGAIN_TRACK_PEAK", listOf("0.9")))))
    }
}
