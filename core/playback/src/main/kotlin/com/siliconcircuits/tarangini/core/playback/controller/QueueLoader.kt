/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.siliconcircuits.tarangini.core.playback.di.PlaybackCoroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Puts items into the player in parts, so that a very long list starts to play at once and the
 * main thread is never busy for long.
 *
 * A new queue starts with a few items from the asked place. The rest follows in larger parts
 * while the first item already plays. Loads run one after the other, in the order they were asked for.
 *
 * Everything here must be called on the main thread.
 */
@Singleton
internal class QueueLoader @Inject constructor(
    private val link: PlayerLink,
    @param:PlaybackCoroutineScope private val scope: CoroutineScope,
) {
    /** The loads that were asked for and have not ended, oldest first. */
    private val loads = mutableListOf<Job>()

    /** Replaces the queue with the files and plays from [startIndex]. A load that still runs is given up. */
    fun replace(mediaUris: List<String>, startIndex: Int) {
        if (mediaUris.isEmpty()) return
        val items = mediaUris.map(::toMediaItem)
        val start = startIndex.coerceIn(items.indices)
        cancel()
        start {
            if (items.size <= FIRST_PART) {
                link.send { player -> player.startWith(items, start) }
                return@start
            }
            val firstEnd = min(start + FIRST_PART, items.size)
            link.send { player -> player.startWith(items.subList(start, firstEnd), 0) }
            waitForSound()
            addInParts(items.subList(firstEnd, items.size)) { player, _ -> player.mediaItemCount }
            addInParts(items.subList(0, start)) { _, added -> added }
        }
    }

    /**
     * Adds the files to the queue, after the loads asked for before this one. With an empty queue they start to play.
     *
     * @param place gives the place of the first file in the queue, read from the player when the load starts
     */
    fun add(mediaUris: List<String>, place: (Player) -> Int) {
        if (mediaUris.isEmpty()) return
        val items = mediaUris.map(::toMediaItem)
        val before = loads.lastOrNull()
        start {
            before?.join()
            var wasEmpty = false
            var first: Int? = null
            val placeOfNext = { player: Player, added: Int ->
                val start = first ?: place(player).coerceIn(0, player.mediaItemCount).also { first = it }
                start + added
            }
            val head = min(FIRST_PART, items.size)
            link.send { player -> wasEmpty = player.mediaItemCount == 0 }
            addInParts(items.subList(0, head), placeOfNext)
            link.send { player ->
                if (wasEmpty) {
                    player.prepare()
                    player.play()
                }
            }
            addInParts(items.subList(head, items.size)) { player, added -> placeOfNext(player, head + added) }
        }
    }

    /** Gives up every load that still runs or waits. Items already in the player stay there. */
    fun cancel() {
        val running = loads.toList()
        loads.clear()
        running.forEach(Job::cancel)
    }

    private fun start(load: suspend () -> Unit) {
        val job = scope.launch(start = CoroutineStart.LAZY) { load() }
        loads += job
        job.invokeOnCompletion { loads -= job }
        job.start()
    }

    private fun Player.startWith(items: List<MediaItem>, startIndex: Int) {
        setMediaItems(items, startIndex, 0)
        prepare()
        play()
    }

    /**
     * @param place gives the place for the next part, from the player and the number of items added so far
     */
    private suspend fun addInParts(items: List<MediaItem>, place: (Player, Int) -> Int) {
        var added = 0
        while (added < items.size) {
            // Lets the main thread draw and take input between two parts.
            yield()
            val part = items.subList(added, min(added + PART, items.size))
            val addedBefore = added
            link.send { player -> player.addMediaItems(place(player, addedBefore), part) }
            added += part.size
        }
    }

    /**
     * Waits until the first item is heard, so that the rest of a long list does not hold it back.
     * It gives up after a short time: the player may be held back for another reason, such as a call.
     */
    private suspend fun waitForSound() {
        var waited = Duration.ZERO
        while (link.player?.isPlaying != true && waited < LONGEST_WAIT) {
            delay(WAIT_STEP)
            waited += WAIT_STEP
        }
    }

    private fun toMediaItem(uri: String): MediaItem = MediaItem.Builder().setMediaId(uri).setUri(uri).build()

    private companion object {
        /** Enough items to start with at once: the current one and what a tap on "next" needs. */
        const val FIRST_PART = 20

        /**
         * Each part makes the player send its whole queue to every listener,
         * so few large parts cost less than many small ones.
         */
        const val PART = 5000

        val WAIT_STEP = 50.milliseconds
        val LONGEST_WAIT = 3.seconds
    }
}
