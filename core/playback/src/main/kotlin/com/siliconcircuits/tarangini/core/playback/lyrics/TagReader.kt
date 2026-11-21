/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.lyrics

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.Metadata
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
import java.io.IOException
import java.io.InputStream

/**
 * Reads the tags of an MP3 or M4A file with the readers Media3 plays the file with, so a tag is
 * understood here the same way as in the player. Only the start of the file is read: the reading
 * ends when the reader has told the format of the track, which carries the tags.
 *
 * @param length the length of the file in bytes when it is known. With it a reader refuses a tag
 * that states to be longer than the file, before it makes room for it
 * @param open opens the file at its start, or gives null when it cannot be opened. An M4A file
 * can keep its tags at the end, so the file is opened again to jump there
 */
// The extractors of Media3 are marked unstable; there is no stable way to read the tags of a file.
@OptIn(UnstableApi::class)
internal class TagReader(private val length: Long = C.LENGTH_UNSET.toLong(), private val open: () -> InputStream?) {
    /** The tags of the file, or null when it has none or is no MP3 or M4A file. */
    // The M4A reader is first: it knows after a few bytes that a file is not its kind.
    fun metadata(): Metadata? =
        sequenceOf(Mp4Extractor(DefaultSubtitleParserFactory()), Mp3Extractor()).firstNotNullOfOrNull(::read)

    private fun read(extractor: Extractor): Metadata? {
        val file = OpenedFile()
        return try {
            val output = FirstFormat()
            val seekTo = PositionHolder()
            var input = file.at(0)
            // A file of another kind ends the try here: its first bytes are not what the reader knows.
            var result = if (extractor.sniff(input)) Extractor.RESULT_CONTINUE else Extractor.RESULT_END_OF_INPUT
            input.resetPeekPosition()
            extractor.init(output)
            var steps = 0
            while (result != Extractor.RESULT_END_OF_INPUT && output.format == null && steps++ < MAX_STEPS) {
                result = extractor.read(input, seekTo)
                if (result == Extractor.RESULT_SEEK) input = file.at(seekTo.position)
            }
            output.format?.metadata
        } catch (_: IOException) {
            // A reader ends this way on a file of another kind, on a short file and on a faulty one.
            // A file that cannot be opened ends here too.
            null
        } finally {
            file.close()
            extractor.release()
        }
    }

    /** The file as the readers want it: opened again for every jump. */
    private inner class OpenedFile {
        private var stream: InputStream? = null

        fun at(position: Long): DefaultExtractorInput {
            close()
            val opened = open() ?: throw IOException("The file cannot be opened.")
            stream = opened
            var left = position
            while (left > 0) {
                val step = opened.skip(left)
                if (step <= 0) throw IOException("The file ends before the place asked for.")
                left -= step
            }
            val reader = DataReader { target, offset, length ->
                val count = opened.read(target, offset, length)
                if (count < 0) C.RESULT_END_OF_INPUT else count
            }
            return DefaultExtractorInput(reader, position, length)
        }

        fun close() {
            stream?.close()
            stream = null
        }
    }

    /** Keeps the first format a reader tells and throws the sound away. */
    private class FirstFormat :
        ExtractorOutput,
        TrackOutput {
        var format: Format? = null

        override fun track(id: Int, type: Int): TrackOutput = this

        override fun endTracks() = Unit

        override fun seekMap(seekMap: SeekMap) = Unit

        override fun format(format: Format) {
            if (this.format == null) this.format = format
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
        /** A reader tells the format after a few steps. The limit ends the reading of a faulty file. */
        const val MAX_STEPS = 1_000
    }
}
