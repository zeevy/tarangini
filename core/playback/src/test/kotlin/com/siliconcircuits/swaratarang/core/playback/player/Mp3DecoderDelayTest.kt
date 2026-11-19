/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.player

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The silence fields of a format are API that may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class Mp3DecoderDelayTest {
    private fun format(mimeType: String, delay: Int, padding: Int): Format =
        Format.Builder().setSampleMimeType(mimeType).setEncoderDelay(delay).setEncoderPadding(padding).build()

    private val mp3 = format(MimeTypes.AUDIO_MPEG, delay = 1_105, padding = 551)

    @Test
    fun `the decoder delay goes from the start silence to the end silence for the decoders of Android`() {
        listOf("c2.android.mp3.decoder", "OMX.google.mp3.decoder").forEach { decoder ->
            val fitted = mp3.forDecoder(decoder)

            assertEquals(576, fitted.encoderDelay)
            assertEquals(1_080, fitted.encoderPadding)
        }
    }

    @Test
    fun `nothing else of the format changes`() {
        val fitted = mp3.forDecoder("c2.android.mp3.decoder")

        assertEquals(mp3, fitted.buildUpon().setEncoderDelay(1_105).setEncoderPadding(551).build())
    }

    @Test
    fun `a decoder that is not known to cut its delay keeps what Media3 says`() {
        assertSame(mp3, mp3.forDecoder("c2.vendor.mp3.decoder"))
        assertSame(mp3, mp3.forDecoder(null))
    }

    @Test
    fun `a file that is not MP3 keeps its silence`() {
        val aac = format(MimeTypes.AUDIO_AAC, delay = 1_024, padding = 888)

        assertSame(aac, aac.forDecoder("c2.android.mp3.decoder"))
    }

    @Test
    fun `an MP3 file with less start silence than the decoder delay is left alone`() {
        val noHeader = format(MimeTypes.AUDIO_MPEG, delay = 0, padding = 0)

        assertSame(noHeader, noHeader.forDecoder("c2.android.mp3.decoder"))
    }
}
