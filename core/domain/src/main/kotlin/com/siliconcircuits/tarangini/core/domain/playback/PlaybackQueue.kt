/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.playback

import com.siliconcircuits.tarangini.core.domain.model.RepeatMode

/**
 * Changes what plays next. The queue itself is read from the state of [PlaybackController].
 *
 * A place in the queue is the index of the item in that state. A place that does not exist is ignored.
 * Like every playback command, a change is ignored when no screen of the app is open.
 */
interface PlaybackQueue {
    /**
     * Puts the files right after the current item. With an empty queue they start to play.
     * With shuffle on they are still placed after the current item in the queue, but the player
     * picks its own moment to play them.
     *
     * @param mediaUris the addresses of the audio files, as MediaStore gives them
     */
    fun playNext(mediaUris: List<String>)

    /**
     * Puts the files at the end of the queue. With an empty queue they start to play.
     *
     * @param mediaUris the addresses of the audio files, as MediaStore gives them
     */
    fun addToEnd(mediaUris: List<String>)

    /** Moves the item at place [from] to place [to]. The current item keeps playing, also when it is the one moved. */
    fun move(from: Int, to: Int)

    /**
     * Takes the item at place [index] out. When it is the current item, the one after it plays,
     * or playback ends when it was the last one.
     */
    fun remove(index: Int)

    /** Empties the queue. Playback stops. */
    fun clear()

    /** With shuffle on, the player picks the items of the queue in a random order. The queue itself keeps its order. */
    fun setShuffle(on: Boolean)

    /** Sets what the player does when an item or the queue ends. */
    fun setRepeat(mode: RepeatMode)
}
