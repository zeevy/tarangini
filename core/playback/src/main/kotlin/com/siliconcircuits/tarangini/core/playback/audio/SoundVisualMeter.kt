/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import com.siliconcircuits.tarangini.core.domain.audio.SoundVisual
import com.siliconcircuits.tarangini.core.domain.audio.VisualFrame
import com.siliconcircuits.tarangini.core.playback.di.PlaybackCoroutineScope
import com.siliconcircuits.tarangini.core.playback.di.VisualDispatcher
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn

/**
 * The visual of the player: the level meter processor writes the newest samples into [window]
 * while a screen collects [frames], and one loop in [scope] turns them into a spectrum and a wave.
 * The bars fall slowly, so a short sound stays long enough to be seen.
 */
@Singleton
internal class SoundVisualMeter @Inject constructor(
    @param:PlaybackCoroutineScope private val scope: CoroutineScope,
    @param:VisualDispatcher private val work: CoroutineDispatcher,
) : SoundVisual {
    private val watchers = AtomicInteger()

    /** The newest samples. Only the level meter processor writes to it. */
    val window = SampleWindow()

    /** True while a screen collects [frames]. Only then are samples copied. */
    val isWatched: Boolean
        get() = watchers.get() > 0

    // The sums run off the main thread. No replay: a screen that comes back must not see an old picture.
    private val shared: Flow<VisualFrame> = flow {
        watchers.incrementAndGet()
        try {
            // Samples from before anybody watched are not shown.
            window.clear()
            var shown = VisualFrame.SILENT
            emit(shown)
            while (true) {
                delay(TICK_MS)
                val samples = window.copy()
                // A frame every tick, also in silence: the peak caps of the screen fall one step per frame.
                shown = VisualFrame(fall(shown.bands, spectrumOf(samples, window.sampleRate)), waveOf(samples))
                emit(shown)
            }
        } finally {
            watchers.decrementAndGet()
        }
    }.flowOn(work).shareIn(scope, SharingStarted.WhileSubscribed())

    override fun frames(): Flow<VisualFrame> = shared

    private companion object {
        /** About 25 frames a second, the same as the level meter. */
        const val TICK_MS = 40L

        /** A bar falls the full height in about 0.7 s. */
        const val FALL_PER_TICK = 1.5f * TICK_MS / 1000f

        fun fall(before: List<Float>, now: List<Float>): List<Float> =
            List(now.size) { maxOf(now[it], before.getOrElse(it) { 0f } - FALL_PER_TICK).coerceAtLeast(0f) }
    }
}
