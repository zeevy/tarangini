/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.lyrics

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import com.siliconcircuits.tarangini.core.domain.text.isDirectionMark

/**
 * Finds the lyrics in the tags Media3 read from a file.
 *
 * Media3 gives the lyrics of an M4A file as a text frame with the name `USLT`. For an MP3 file it
 * gives the `USLT` frame as its bytes, which are taken apart here. A tag is text from a file of
 * unknown origin, so it is cleaned and cut before it is shown.
 */
internal object LyricsFrames {
    /** Longer lyrics are cut. A long song text has a few thousand characters. */
    const val MAX_CHARS = 50_000

    /** The name of the frame in a tag of today, and in the old tags with names of three letters. */
    private val FRAMES = setOf("USLT", "ULT")

    /** The encoding byte and the three letters of the language come before the text of a frame. */
    private const val HEADER_BYTES = 4
    private const val LATIN = 0
    private const val UTF_16 = 1
    private const val UTF_16_BIG_ENDIAN = 2
    private const val UTF_8 = 3

    /** The lyrics in [metadata], or null when it has none. */
    // The tag frames of Media3 are marked unstable; there is no stable way to read a tag.
    @OptIn(UnstableApi::class)
    fun textOf(metadata: Metadata): String? = (0 until metadata.length()).firstNotNullOfOrNull { index ->
        when (val entry = metadata[index]) {
            is BinaryFrame -> if (entry.id in FRAMES) unsynced(entry.data)?.let(::clean) else null
            is TextInformationFrame -> if (entry.id in FRAMES) clean(entry.values.joinToString("\n")) else null
            else -> null
        }
    }

    /**
     * The text of an ID3 frame for lyrics: one byte for the encoding, three for the language, a
     * description that ends with a zero character, then the lyrics. Null when the bytes are no such frame.
     */
    fun unsynced(frame: ByteArray): String? {
        val charset = when (frame.firstOrNull()?.toInt()) {
            LATIN -> Charsets.ISO_8859_1
            UTF_16 -> Charsets.UTF_16
            UTF_16_BIG_ENDIAN -> Charsets.UTF_16BE
            UTF_8 -> Charsets.UTF_8
            else -> null
        }
        val wide = charset == Charsets.UTF_16 || charset == Charsets.UTF_16BE
        val start = if (frame.size < HEADER_BYTES) null else endOfDescription(frame, wide)
        return if (charset == null || start == null) null else String(frame, start, frame.size - start, charset)
    }

    /** The place after the zero character that ends the description, or null when there is none. */
    private fun endOfDescription(frame: ByteArray, wide: Boolean): Int? {
        val step = if (wide) 2 else 1
        val zero = (HEADER_BYTES..frame.size - step step step).firstOrNull { at ->
            frame[at].toInt() == 0 && frame[at + step - 1].toInt() == 0
        }
        return zero?.let { it + step }
    }

    /**
     * Lyrics as they may be shown: one kind of line break, no control character, no character that
     * changes the direction of the text, and no more than [MAX_CHARS]. Null when nothing is left.
     */
    fun clean(text: String): String? = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace('\t', ' ')
        .filterNot { it != '\n' && (it.isISOControl() || it.isDirectionMark()) }
        .trim()
        .take(MAX_CHARS)
        .ifEmpty { null }
}
