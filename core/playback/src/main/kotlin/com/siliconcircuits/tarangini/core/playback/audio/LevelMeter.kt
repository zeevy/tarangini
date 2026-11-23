/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.siliconcircuits.tarangini.core.domain.audio.AudioLevels
import com.siliconcircuits.tarangini.core.domain.audio.LevelReading
import com.siliconcircuits.tarangini.core.domain.audio.SILENCE_DB
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * The level meter of the app. The playback service puts [processor] into the sound path of the
 * player; the screens read [levels]. The service runs in the process of the app, so both sides
 * share this one object.
 */
@Singleton
internal class LevelMeter @Inject constructor() : AudioLevels {
    private val watchers = AtomicInteger()
    private val lock = Any()
    private var heldLeft = 0f
    private var heldRight = 0f

    /** True while a screen collects [levels]. Only then is the sound measured. */
    val isWatched: Boolean
        get() = watchers.get() > 0

    /** A new processor for a new player. Each player needs its own, as a processor keeps the format of its player. */
    // A processor of the audio sink is API of Media3 that may still change.
    @OptIn(UnstableApi::class)
    fun processor(): AudioProcessor = LevelMeterProcessor(this)

    /** The peaks of one buffer, from 0 to 1. Called on the playback thread. */
    fun report(left: Float, right: Float) {
        synchronized(lock) {
            heldLeft = maxOf(heldLeft, left)
            heldRight = maxOf(heldRight, right)
        }
    }

    override fun levels(): Flow<LevelReading> = flow {
        watchers.incrementAndGet()
        try {
            take()
            var shown = LevelReading.SILENCE
            emit(shown)
            while (true) {
                delay(TICK_MS)
                val (left, right) = take()
                val next = LevelReading(fall(shown.leftDb, decibels(left)), fall(shown.rightDb, decibels(right)))
                if (next != shown) {
                    shown = next
                    emit(next)
                }
            }
        } finally {
            watchers.decrementAndGet()
        }
    }

    // The highest peaks since the last tick, and a new start for the next one.
    private fun take(): Pair<Float, Float> = synchronized(lock) {
        val peaks = heldLeft to heldRight
        heldLeft = 0f
        heldRight = 0f
        peaks
    }

    private companion object {
        /** About 25 readings a second: smooth to the eye, and little work. */
        const val TICK_MS = 40L

        /** A level falls 20 dB a second, so a short peak stays long enough to be seen and the meter does not jump. */
        const val FALL_PER_TICK_DB = 20f * TICK_MS / 1000f

        fun decibels(peak: Float): Float =
            if (peak <= 0f) SILENCE_DB else (DB_PER_DECADE * log10(peak)).coerceIn(SILENCE_DB, 0f)

        fun fall(before: Float, now: Float): Float = maxOf(now, before - FALL_PER_TICK_DB).coerceAtLeast(SILENCE_DB)

        const val DB_PER_DECADE = 20f
    }
}
