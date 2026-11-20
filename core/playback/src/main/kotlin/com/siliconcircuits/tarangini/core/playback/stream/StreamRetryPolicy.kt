/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo

/**
 * Says how long the player goes on trying when a stream cannot be read.
 *
 * Radio in a moving car loses the network often. The player then tries again by itself, after 1,
 * 2 and 4 seconds and then every 5 seconds, and stays in the loading state all that time. After
 * [LIMIT_MS] of failures it gives up, and the error goes to the screens and the session.
 *
 * A fault that waiting cannot mend ends the tries at once, and a file of the phone keeps the few
 * quick tries of Media3.
 *
 * The same object listens to the player, to know when the time starts again.
 *
 * @param now the clock, in milliseconds. A test hands in its own
 */
// A load error policy is API of Media3 that may still change.
@OptIn(UnstableApi::class)
internal class StreamRetryPolicy(private val now: () -> Long = SystemClock::elapsedRealtime) :
    DefaultLoadErrorHandlingPolicy(),
    Player.Listener {
    // Loads fail on a thread of the player, and the player reports on the main thread.
    private val lock = Any()
    private var failingSince = NOT_FAILING
    private var lastFailure = NOT_FAILING

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorInfo): Long {
        val triesOfMedia3 = super.getRetryDelayMsFor(loadErrorInfo)
        val isStream = loadErrorInfo.loadEventInfo.uri.scheme?.lowercase() in STREAM_SCHEMES
        return when {
            triesOfMedia3 == C.TIME_UNSET || loadErrorInfo.exception.cannotGetBetter() -> C.TIME_UNSET
            isStream -> streamRetryDelayMs(loadErrorInfo.errorCount).takeIf { hasTimeLeft() } ?: C.TIME_UNSET
            loadErrorInfo.errorCount > FILE_TRIES -> C.TIME_UNSET
            else -> triesOfMedia3
        }
    }

    /** The count never ends the tries: [getRetryDelayMsFor] does, by the time and by the kind of fault. */
    override fun getMinimumLoadableRetryCount(dataType: Int): Int = Int.MAX_VALUE

    private fun hasTimeLeft(): Boolean = synchronized(lock) {
        val time = now()
        // A drop can mend while sound from the buffer still plays, and then nothing tells this
        // object. Failures come at least every [QUIET_MS] while a stream is down, so a longer
        // silence means that it was read again in between.
        val mended = lastFailure != NOT_FAILING && time - lastFailure > QUIET_MS
        if (failingSince == NOT_FAILING || mended) failingSince = time
        lastFailure = time
        time - failingSince < LIMIT_MS
    }

    private fun startAgain() = synchronized(lock) {
        failingSince = NOT_FAILING
        lastFailure = NOT_FAILING
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) startAgain()
    }

    /** After an error the next try is one the person asked for, or another address. It gets the whole time. */
    override fun onPlayerError(error: PlaybackException) = startAgain()

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = startAgain()

    private companion object {
        const val LIMIT_MS = 120_000L
        const val NOT_FAILING = -1L

        /** Longer than one try can take: 10 seconds to connect, 20 to read, 5 to wait. */
        const val QUIET_MS = 45_000L

        /** How often Media3 tries to read a file by default. */
        const val FILE_TRIES = 3
    }
}

/**
 * How long to wait before try number [errorCount] plus one: 1, 2 and 4 seconds, then 5 seconds
 * each time. Short waits first, because most drops are short. Never long ones, so that the sound
 * is back soon after the network is.
 */
internal fun streamRetryDelayMs(errorCount: Int): Long =
    (FIRST_WAIT_MS shl (errorCount - 1).coerceIn(0, DOUBLINGS)).coerceAtMost(LONGEST_WAIT_MS)

/** True for a fault that is the same on the next try. */
@OptIn(UnstableApi::class)
private fun Throwable.cannotGetBetter(): Boolean = generateSequence(this) { it.cause }.take(CAUSES_READ).any { fault ->
    fault is PlaylistException ||
        fault is RefusedAddressException ||
        fault is HttpDataSource.InvalidContentTypeException ||
        (fault is HttpDataSource.InvalidResponseCodeException && fault.responseCode.isClientFault())
}

private const val FIRST_WAIT_MS = 1_000L
private const val LONGEST_WAIT_MS = 5_000L
private const val DOUBLINGS = 3
private const val CAUSES_READ = 10

/**
 * True for the answers that say "the request is wrong", such as 404. An answer from 500 on can
 * pass, and so can "too slow" (408) and "too many requests" (429).
 */
private fun Int.isClientFault(): Boolean = this in CLIENT_FAULTS && this != HTTP_TIMEOUT && this != HTTP_TOO_MANY

private val CLIENT_FAULTS = 400..499
private const val HTTP_TIMEOUT = 408
private const val HTTP_TOO_MANY = 429
