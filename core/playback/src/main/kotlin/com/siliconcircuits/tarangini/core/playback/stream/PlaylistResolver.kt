/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody

/** A playlist file could not be turned into the address of a stream. */
internal class PlaylistException(message: String) : IOException(message)

/**
 * Finds the stream behind a playlist file (`.pls`, `.m3u`).
 *
 * The file comes from a server nobody here knows, so every step has a limit: the size of a file,
 * the time one read may take, and the number of files that point to the next one. Only `http` and
 * `https` addresses are followed. A file that leads back to itself is an error, not a loop.
 * Redirects are followed by the HTTP client, which follows only `http` and `https` and gives up
 * after 20 of them.
 *
 * It makes network calls and must not run on the main thread.
 *
 * @param httpClient gives the shared client. It is asked for at the first use
 */
internal class PlaylistResolver(httpClient: () -> OkHttpClient) {
    // The read timeout alone lets a server send one byte now and then for days. A few lines of text need seconds.
    private val client by lazy { httpClient().newBuilder().callTimeout(CALL_SECONDS, TimeUnit.SECONDS).build() }

    /**
     * Gives the address of the stream for [address]. An address that is not a playlist file comes
     * back as it is, with no network call.
     *
     * @throws PlaylistException when a limit is passed or the file names no stream
     * @throws IOException when the server cannot be reached or does not answer in time
     */
    @Throws(IOException::class)
    fun resolve(address: HttpUrl): HttpUrl {
        var current = address
        val seen = HashSet<HttpUrl>()
        while (current.isPlaylistFile()) {
            if (!seen.add(current)) refuse("The playlist file leads back to itself")
            if (seen.size > MAX_FILES) refuse("More than $MAX_FILES playlist files point to one another")
            current = firstStreamIn(current)
        }
        return current
    }

    private fun firstStreamIn(file: HttpUrl): HttpUrl =
        client.newCall(Request.Builder().url(file).build()).execute().use { response ->
            // A redirect to something that is not http or https is not followed and ends here too.
            if (!response.isSuccessful) refuse("The server answered ${response.code} for the playlist file")
            // An entry may be relative to the place the file was read from in the end. An entry with
            // another scheme, such as a file path, gives null.
            val readFrom = response.request.url
            playlistEntries(response.body.limitedText()).firstNotNullOfOrNull { readFrom.resolve(it) }
                ?: refuse("The playlist file names no http or https stream")
        }

    /** The text of the body. Not more than the limit is ever read from the network. */
    private fun ResponseBody.limitedText(): String {
        val source = source()
        if (source.request(MAX_BYTES + 1)) refuse("The playlist file is larger than $MAX_BYTES bytes")
        return source.readUtf8()
    }

    private fun refuse(why: String): Nothing = throw PlaylistException(why)

    private companion object {
        /** Playlist files that may point to the next one. Real stations need one, a few need two. */
        const val MAX_FILES = 3

        /** A playlist file is a few lines of text. 64 KiB is far more than any real one. */
        const val MAX_BYTES = 64L * 1024
        const val CALL_SECONDS = 15L
    }
}
