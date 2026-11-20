/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.station

/** Tries a stream address, so a person knows before saving whether a station answers there. */
interface StreamTester {
    /** Opens [address] for a moment and says what answered. It never fails with an exception for a bad address. */
    suspend fun test(address: String): StreamTest
}

/** What a try of a stream address gave. */
sealed interface StreamTest {
    /**
     * A stream answered.
     *
     * @property contentType the kind of stream the server names, such as `audio/mpeg`
     * @property stationName the name the server gives the station, when it gives one
     * @property bitrateKbps the bit rate the server names, when it names one
     */
    data class Found(val contentType: String, val stationName: String? = null, val bitrateKbps: Int? = null) :
        StreamTest

    /** Nothing that can be played answered. */
    data class Failed(val fault: StreamTestFault) : StreamTest
}

/** Why a try of a stream address found no stream. */
enum class StreamTestFault {
    /** The address is not one the app opens. */
    BAD_ADDRESS,

    /** No server answered in time. */
    NO_ANSWER,

    /** The server answered with an error, such as "not found". */
    BAD_ANSWER,

    /** The playlist file at the address names no stream, or is too large or too deep. */
    BAD_PLAYLIST,

    /** The server answered, but not with a stream the app can play. */
    NOT_A_STREAM,
}
