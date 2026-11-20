/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo
import java.io.IOException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// A load error policy is API of Media3 that may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class StreamRetryPolicyTest {
    private var clock = 1_000_000L
    private val policy = StreamRetryPolicy { clock }
    private val stream = DataSpec(Uri.parse("https://radio.example/stream"))
    private val file = DataSpec(Uri.parse("content://media/external/audio/media/1"))
    private val noNetwork: IOException = HttpDataSource.HttpDataSourceException(
        UnknownHostException("radio.example"),
        stream,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        HttpDataSource.HttpDataSourceException.TYPE_OPEN,
    )

    private fun delayFor(fault: IOException, errorCount: Int, spec: DataSpec = stream): Long =
        policy.getRetryDelayMsFor(
            LoadErrorInfo(LoadEventInfo(1, spec, clock), MediaLoadData(C.DATA_TYPE_MEDIA), fault, errorCount),
        )

    private fun answer(code: Int): IOException =
        HttpDataSource.InvalidResponseCodeException(code, null, null, emptyMap(), stream, ByteArray(0))

    @Test
    fun `the waits are 1, 2 and 4 seconds, then 5 seconds each time`() {
        assertEquals(
            listOf(1_000L, 2_000L, 4_000L, 5_000L, 5_000L, 5_000L),
            (1..6).map(::streamRetryDelayMs),
        )
        assertEquals(5_000L, streamRetryDelayMs(500))
        assertEquals(1_000L, streamRetryDelayMs(0))
    }

    /** The stream fails again and again, with [stepMs] between two failures. Gives the last answer. */
    private fun failFor(totalMs: Long, stepMs: Long = 5_000): Long {
        var answer = delayFor(noNetwork, 1)
        repeat((totalMs / stepMs).toInt()) {
            clock += stepMs
            answer = delayFor(noNetwork, 1)
        }
        return answer
    }

    @Test
    fun `a stream with no network is tried again for 2 minutes by the clock, then no more`() {
        assertEquals(1_000L, failFor(115_000))

        clock += 5_000
        assertEquals(C.TIME_UNSET, delayFor(noNetwork, 30))
    }

    @Test
    fun `the time starts again when sound is back, after an error and with another item`() {
        val startsAgain = listOf<() -> Unit>(
            { policy.onIsPlayingChanged(true) },
            { policy.onPlayerError(PlaybackException("gave up", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)) },
            { policy.onMediaItemTransition(null, 0) },
        )
        startsAgain.forEach { event ->
            failFor(110_000)
            event()

            assertEquals(1_000L, failFor(110_000))
        }
    }

    @Test
    fun `a drop that mended while the buffer played does not count against the next one`() {
        failFor(110_000)
        // Nothing fails for a while, and the player says nothing: sound never stopped.
        clock += 46_000

        assertEquals(1_000L, failFor(110_000))
    }

    @Test
    fun `slow tries, each as long as one can take, still end after 2 minutes`() {
        assertEquals(C.TIME_UNSET, failFor(135_000, stepMs = 45_000))
    }

    @Test
    fun `a pause does not start the time again`() {
        failFor(110_000)
        policy.onIsPlayingChanged(false)

        assertEquals(C.TIME_UNSET, failFor(10_000))
    }

    @Test
    fun `a server that is busy is tried again, an address it does not have is not`() {
        assertEquals(1_000L, delayFor(answer(503), 1))
        assertEquals(C.TIME_UNSET, delayFor(answer(404), 1))
        assertEquals(C.TIME_UNSET, delayFor(answer(403), 1))
        assertEquals(1_000L, delayFor(answer(408), 1))
        assertEquals(1_000L, delayFor(answer(429), 1))
    }

    @Test
    fun `a playlist file with a fault and a refused address are not tried again, also inside another error`() {
        assertEquals(C.TIME_UNSET, delayFor(PlaylistException("names no stream"), 1))
        assertEquals(C.TIME_UNSET, delayFor(IOException(RefusedAddressException()), 1))
    }

    @Test
    fun `a playlist file that cannot be reached is tried again`() {
        assertEquals(1_000L, delayFor(UnknownHostException("radio.example"), 1))
    }

    @Test
    fun `a file of the phone gets the few quick tries of Media3 and no more`() {
        val fault = IOException("read failed")

        assertEquals(0L, delayFor(fault, 1, file))
        assertEquals(2_000L, delayFor(fault, 3, file))
        assertEquals(C.TIME_UNSET, delayFor(fault, 4, file))
    }

    @Test
    fun `the count of tries alone never ends them`() {
        assertEquals(Int.MAX_VALUE, policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA))
    }
}
