/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import android.net.Uri
import com.siliconcircuits.tarangini.core.domain.station.StreamTest
import com.siliconcircuits.tarangini.core.domain.station.StreamTestFault
import com.siliconcircuits.tarangini.core.domain.station.StreamTester
import com.siliconcircuits.tarangini.core.playback.di.StreamIoDispatcher
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runInterruptible
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * [StreamTester] that opens the address the way the player would: the same address check, the
 * same HTTP client, and a playlist file is exchanged for its stream first.
 *
 * Only the first answer of the server is read, never the sound. The whole try ends after
 * [LIMIT_SECONDS]. Every text of the server is cleaned before it is given on.
 */
internal class DefaultStreamTester @Inject constructor(
    // The client is built at the first use, which is on the worker thread here.
    private val httpClient: Provider<OkHttpClient>,
    @StreamIoDispatcher private val io: CoroutineDispatcher,
) : StreamTester {
    // A try that is not wanted any more is interrupted, so its call to the server ends at once.
    override suspend fun test(address: String): StreamTest = runInterruptible(io) {
        val url = address.trim().takeIf { Uri.parse(it).isStreamAddress() }?.toHttpUrlOrNull()
        try {
            if (url == null) StreamTest.Failed(StreamTestFault.BAD_ADDRESS) else open(url)
        } catch (_: PlaylistException) {
            StreamTest.Failed(StreamTestFault.BAD_PLAYLIST)
        } catch (_: IOException) {
            StreamTest.Failed(StreamTestFault.NO_ANSWER)
        } catch (_: IllegalArgumentException) {
            // An answer the HTTP client cannot make sense of, such as a redirect to an address that is none.
            StreamTest.Failed(StreamTestFault.NO_ANSWER)
        }
    }

    private fun open(url: HttpUrl): StreamTest {
        val client = httpClient.get().newBuilder().callTimeout(LIMIT_SECONDS, TimeUnit.SECONDS).build()
        val stream = PlaylistResolver { client }.resolve(url)
        // A station sends its name only to a caller that asks for the titles in the stream.
        val request = Request.Builder().url(stream).header("Icy-MetaData", "1").build()
        return client.newCall(request).execute().use { answer -> answer.toResult(stream) }
    }

    private fun Response.toResult(stream: HttpUrl): StreamTest {
        val type = header("Content-Type").orEmpty().substringBefore(';').trim().lowercase()
        return when {
            !isSuccessful -> StreamTest.Failed(StreamTestFault.BAD_ANSWER)

            !type.isPlayable(stream) -> StreamTest.Failed(StreamTestFault.NOT_A_STREAM)

            else -> StreamTest.Found(
                contentType =
                    type.takeIf { it.length in 1..LONGEST_TYPE && it.all(typeCharacters::contains) } ?: "audio",
                stationName = cleanStreamText(header("icy-name")),
                bitrateKbps = header("icy-br")?.substringBefore(',')?.trim()?.toIntOrNull()?.takeIf { it in bitrates },
            )
        }
    }

    /**
     * True for a kind of answer the player can play from [stream]. The player knows an HLS stream
     * and a playlist file only by the ending of the address, so the test does the same.
     */
    private fun String.isPlayable(stream: HttpUrl): Boolean = when (this) {
        in hlsTypes -> stream.encodedPath.endsWith(".m3u8", ignoreCase = true)

        in playlistTypes -> false

        // A server that names no kind, or only "bytes", is left to the player, which looks at the sound itself.
        else -> startsWith("audio/") || this in otherTypes
    }

    private companion object {
        const val LIMIT_SECONDS = 15L
        const val LONGEST_TYPE = 50
        val bitrates = 1..9_999
        val typeCharacters = ('a'..'z') + ('0'..'9') + listOf('/', '.', '+', '-')
        val hlsTypes =
            setOf("application/vnd.apple.mpegurl", "application/x-mpegurl", "audio/x-mpegurl", "audio/mpegurl")
        val otherTypes = setOf("application/ogg", "application/octet-stream", "")
        val playlistTypes = setOf("audio/x-scpls", "application/pls+xml")
    }
}
