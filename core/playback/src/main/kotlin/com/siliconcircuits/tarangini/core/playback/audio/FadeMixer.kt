/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Holds the end of a song and mixes it into the start of the next one: the old song fades out
 * while the new one fades in, on equal power curves (cosine out, sine in), so the level stays even.
 * It works on 16 bit samples, one frame being one sample per channel.
 *
 * While it holds, the newest samples stay here and only the samples that no longer fit go on, so
 * the order of the sound never changes, also when the song runs longer than planned.
 */
internal class FadeMixer {
    private var sampleRate = DEFAULT_RATE
    private var channels = 2

    // The held end is a ring: [start] is its oldest sample, [held] the number of samples in it.
    private var tail = ShortArray(0)
    private var start = 0
    private var held = 0
    private var room = 0
    private var mixed = 0
    private var tailStartUs = 0L
    private var state = State.PASS

    /** True while the sound goes on as it comes. */
    val isPassing: Boolean get() = state == State.PASS

    /** True while some of a held end has not been mixed yet. */
    val hasTail: Boolean get() = held > mixed

    /** The sound that comes next has [sampleRate] frames a second of [channels] samples each. */
    fun configure(sampleRate: Int, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
    }

    /** [remainingUs] of the song are still to come: within the last [fadeUs] its end is held. */
    fun songEndsIn(remainingUs: Long, fadeUs: Long) {
        if (state != State.PASS || fadeUs <= 0 || remainingUs !in 1..fadeUs) return
        room = framesIn(fadeUs, sampleRate) * channels
        if (room <= 0) return
        if (tail.size < room) tail = ShortArray(room)
        start = 0
        held = 0
        mixed = 0
        state = State.HOLD
    }

    /** The first sound of the next song comes next: a held end is mixed into it. */
    fun nextSongStarts() {
        if (state != State.HOLD) return
        state = if (held > 0) State.MIX else State.PASS
    }

    /** Takes the [samples] that start at [timeUs] and gives the samples to play, with their start. */
    fun take(samples: ShortArray, timeUs: Long): Taken = when (state) {
        State.PASS -> Taken(samples, timeUs)
        State.HOLD -> hold(samples, timeUs)
        State.MIX -> Taken(mix(samples), timeUs)
    }

    /** Gives the held end that was not mixed, to play as it is, and stops holding. */
    fun releaseTail(): Taken {
        val rest = ShortArray(held - mixed)
        copyOut(mixed, rest.size, rest, 0)
        val taken = Taken(rest, tailStartUs + usOf((mixed / channels).toLong(), sampleRate))
        flush()
        return taken
    }

    /** Forgets everything held, as after a seek. */
    fun flush() {
        state = State.PASS
        held = 0
        mixed = 0
    }

    private fun hold(samples: ShortArray, timeUs: Long): Taken {
        if (held == 0) tailStartUs = timeUs
        val out = (held + samples.size - room).coerceAtLeast(0)
        val goes = ShortArray(out)
        // The oldest held samples go first, then the new samples that do not fit at all.
        val fromRing = minOf(out, held)
        copyOut(0, fromRing, goes, 0)
        start = (start + fromRing) % room
        held -= fromRing
        val skipped = out - fromRing
        samples.copyInto(goes, fromRing, 0, skipped)
        for (index in skipped until samples.size) {
            tail[(start + held) % room] = samples[index]
            held++
        }
        val goesAt = tailStartUs
        tailStartUs += usOf((out / channels).toLong(), sampleRate)
        return Taken(goes, goesAt)
    }

    private fun mix(samples: ShortArray): ShortArray {
        val out = samples.copyOf()
        val frames = (held / channels).coerceAtLeast(1)
        var fadeOut = 1.0
        var fadeIn = 0.0
        var index = 0
        while (index < out.size && mixed < held) {
            // Both samples of a frame get the same share.
            if (mixed % channels == 0) {
                val angle = (mixed / channels).toDouble() / frames * PI / 2
                fadeOut = cos(angle)
                fadeIn = sin(angle)
            }
            val value = tail[(start + mixed) % room] * fadeOut + samples[index] * fadeIn
            out[index] = value.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            index++
            mixed++
        }
        if (mixed >= held) flush()
        return out
    }

    /** Copies [count] held samples from the [from]th onwards into [into] at [at]. */
    private fun copyOut(from: Int, count: Int, into: ShortArray, at: Int) {
        for (index in 0 until count) into[at + index] = tail[(start + from + index) % room]
    }

    /** Samples to play and the time of the first one. */
    class Taken(val samples: ShortArray, val timeUs: Long)

    private enum class State { PASS, HOLD, MIX }

    private companion object {
        const val DEFAULT_RATE = 44_100
    }
}

private fun framesIn(us: Long, sampleRate: Int): Int = (sampleRate * us / MICROS_PER_SECOND).toInt()

private fun usOf(frames: Long, sampleRate: Int): Long = frames * MICROS_PER_SECOND / sampleRate

private const val MICROS_PER_SECOND = 1_000_000L
