/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

// The file readers of Media3 are API that may still change.
@file:OptIn(UnstableApi::class)

package com.siliconcircuits.tarangini.core.playback.player

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

/** Runs [extractor] over the test file at [path] under the test resources until it has told the format of the track. */
internal fun readTestFormat(path: String, extractor: Extractor): Format {
    val bytes = requireNotNull(CapturingOutput::class.java.classLoader?.getResourceAsStream(path)).use {
        it.readBytes()
    }
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
