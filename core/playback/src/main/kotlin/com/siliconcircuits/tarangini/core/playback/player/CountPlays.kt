/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Counts an item as played once it was heard for [NEEDED_MS]. An item that is skipped in its first
 * seconds is not counted. Time in pause does not count, and an item is counted once each time it
 * starts, so a song on repeat counts again.
 *
 * @param played is told the queue id of the item that was heard
 */
internal class CountPlays(private val scope: CoroutineScope, private val played: suspend (mediaId: String) -> Unit) {
    private var item: String? = null
    private var isPlaying = false
    private var heardMs = 0L
    private var counted = false
    private var watch: Job? = null

    /** A new item starts, or the same one starts again. Null when nothing is loaded. */
    fun itemStarted(mediaId: String?) {
        item = mediaId
        heardMs = 0
        counted = false
        watchAgain()
    }

    /** Sound starts or stops. */
    fun playing(now: Boolean) {
        isPlaying = now
        watchAgain()
    }

    private fun watchAgain() {
        watch?.cancel()
        val heard = item
        if (!isPlaying || counted || heard == null) return
        watch = scope.launch {
            while (heardMs < NEEDED_MS) {
                delay(STEP_MS)
                heardMs += STEP_MS
            }
            counted = true
            // The write is its own job: a pause or a skip right now ends the watch, but not the write.
            // A play that cannot be saved is lost. The music goes on.
            scope.launch { runCatching { played(heard) } }
        }
    }

    companion object {
        /** How long an item must be heard to count. Shorter is a skip. */
        const val NEEDED_MS = 10_000L
        private const val STEP_MS = 1_000L
    }
}
