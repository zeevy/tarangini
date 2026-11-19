/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Reads the test albums in `src/test/resources/gapless` the way the player does and checks what it
 * learns about the silence an encoder adds. The player can cut that silence only when the file says
 * how long it is. Each album is three tracks cut from one tone, 88,200 samples a track. How the
 * files were made and what was measured with them is in `docs/adr/0007-gapless-playback.md`.
 */
// The file readers of Media3 are API that may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class GaplessInfoTest {
    @Test
    fun `an MP3 file with a LAME header gets the decoder delay added by Media3`() {
        // If this fails after a Media3 change, Media3 no longer moves the silence by the decoder
        // delay, and forDecoder in Mp3DecoderDelay.kt must go.
        TRACKS.forEach { track ->
            val format = readFormat("with-delay-header-$track.mp3", Mp3Extractor())

            assertEquals(LAME_DELAY + MP3_DECODER_DELAY, format.encoderDelay)
            assertEquals(LAME_PADDING - MP3_DECODER_DELAY, format.encoderPadding)
        }
    }

    @Test
    fun `an MP3 file with a LAME header keeps only its real samples on the decoder of Android`() {
        TRACKS.forEach { track ->
            val format = readFormat("with-delay-header-$track.mp3", Mp3Extractor()).forDecoder("c2.android.mp3.decoder")

            assertEquals(LAME_DELAY, format.encoderDelay)
            assertEquals(LAME_PADDING, format.encoderPadding)
        }
    }

    @Test
    fun `an MP3 file with no delay header tells nothing, so its silence stays`() {
        val format = readFormat("no-delay-header-1.mp3", Mp3Extractor()).forDecoder("c2.android.mp3.decoder")

        assertEquals(0, format.encoderDelay)
        assertEquals(0, format.encoderPadding)
    }

    @Test
    fun `an M4A file with an edit list keeps only its real samples`() {
        TRACKS.forEach { track ->
            val format = readFormat("with-edit-list-skip-$track.m4a", Mp4Extractor(DefaultSubtitleParserFactory()))

            assertEquals(AAC_DELAY, format.encoderDelay)
            assertEquals(REAL_SAMPLES, AAC_DECODED_SAMPLES - format.encoderDelay - format.encoderPadding)
            assertEquals(format, format.forDecoder("c2.android.aac.decoder"))
        }
    }

    @Test
    fun `an M4A file whose edit list skips nothing tells nothing, so its silence stays`() {
        val format = readFormat("no-edit-list-skip-1.m4a", Mp4Extractor(DefaultSubtitleParserFactory()))

        assertEquals(0, format.encoderDelay)
        assertEquals(0, format.encoderPadding)
    }

    /** Runs [extractor] over the test file until it has told the format of the track. */
    private fun readFormat(file: String, extractor: Extractor): Format {
        val bytes = requireNotNull(javaClass.classLoader?.getResourceAsStream("gapless/$file")).use { it.readBytes() }
        var position = 0
        val reader = DataReader { target, offset, length ->
            val count = minOf(length, bytes.size - position)
            if (count <= 0) {
                C.RESULT_END_OF_INPUT
            } else {
                bytes.copyInto(target, offset, position, position + count)
                position += count
                count
            }
        }
        val output = CapturingOutput()
        extractor.init(output)
        val seekTo = PositionHolder()
        var input = DefaultExtractorInput(reader, 0, bytes.size.toLong())
        var result = Extractor.RESULT_CONTINUE
        while (result != Extractor.RESULT_END_OF_INPUT && output.format == null) {
            result = extractor.read(input, seekTo)
            if (result == Extractor.RESULT_SEEK) {
                position = seekTo.position.toInt()
                input = DefaultExtractorInput(reader, seekTo.position, bytes.size.toLong())
            }
        }
        return requireNotNull(output.format)
    }

    private class CapturingOutput :
        ExtractorOutput,
        TrackOutput {
        var format: Format? = null

        override fun track(id: Int, type: Int): TrackOutput = this

        override fun endTracks() = Unit

        override fun seekMap(seekMap: SeekMap) = Unit

        override fun format(format: Format) {
            this.format = format
        }

        override fun sampleData(input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int): Int {
            val skipped = input.read(ByteArray(length), 0, length)
            return if (skipped == C.RESULT_END_OF_INPUT) C.RESULT_END_OF_INPUT else skipped
        }

        override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) = data.skipBytes(length)

        override fun sampleMetadata(
            timeUs: Long,
            flags: Int,
            size: Int,
            offset: Int,
            cryptoData: TrackOutput.CryptoData?,
        ) = Unit
    }

    private companion object {
        val TRACKS = 1..3

        /** The samples of real sound in every test track: 2 seconds at 44.1 kHz. */
        const val REAL_SAMPLES = 88_200

        /** What the LAME header of the test files says, as the encoder wrote it. */
        const val LAME_DELAY = 576
        const val LAME_PADDING = 1_080

        const val AAC_DELAY = 1_024

        /** 88 AAC frames of 1,024 samples. */
        const val AAC_DECODED_SAMPLES = 90_112
    }
}
