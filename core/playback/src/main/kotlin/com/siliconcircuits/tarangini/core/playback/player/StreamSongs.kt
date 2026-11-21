/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

/**
 * Tells which song a stream of the audio renderer belongs to. The renderer reads the next song
 * before the current one has ended; [read] keeps the song of each stream by its offset, and
 * [started] gives the song whose sound starts at that offset.
 *
 * The first stream after a start, a skip or a new queue plays at once, and Media3 tells of it
 * while the stream is handed over, or not at all when its offset is the same as before. For that
 * stream [playsAtOnce] gives its song.
 */
internal class StreamSongs<T : Any> {
    private val songs = LinkedHashMap<Long, T>()

    /** The renderer read the stream with [offsetUs] of [song]. */
    fun read(offsetUs: Long, song: T) {
        songs[offsetUs] = song
        // A few streams are read ahead at most; the oldest ones are done.
        while (songs.size > KEPT) songs.remove(songs.keys.first())
    }

    /** The song whose sound starts now, at the stream with [offsetUs], or null when it is not known. */
    fun started(offsetUs: Long): T? = songs[offsetUs]

    /**
     * The song of the stream with [offsetUs] that was just handed over, when it is the one that
     * plays now ([outputOffsetUs] is the offset of the sound that goes out), else null: then it
     * waits for the end of the song before it.
     */
    fun playsAtOnce(offsetUs: Long, outputOffsetUs: Long): T? = if (offsetUs ==
        outputOffsetUs
    ) {
        songs[offsetUs]
    } else {
        null
    }

    private companion object {
        const val KEPT = 8
    }
}
