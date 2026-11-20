/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.siliconcircuits.tarangini.core.playback.session.isMediaStoreAudioItem
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

/**
 * Where the player reads its data from: the audio files of the phone as before, and `http` and
 * `https` streams through the shared HTTP client. A playlist file (`.pls`, `.m3u`) is exchanged for
 * the stream it names before the stream is opened, on the loading thread of the player.
 *
 * Every address is checked again here, just before it is opened. The check on the item alone is
 * not enough: an HLS playlist comes from the server and names the addresses of its parts and keys
 * itself. Without this check it could name a file of this app or the content of another app.
 *
 * @param httpClient gives the shared client. It is asked for when the first stream is opened, so a
 *   person who plays only files never builds it
 */
// The data source classes of Media3 are API that may still change.
@OptIn(UnstableApi::class)
internal fun streamDataSourceFactory(context: Context, httpClient: () -> OkHttpClient): DataSource.Factory {
    val playlists = PlaylistResolver(httpClient)
    val http = OkHttpDataSource.Factory(Call.Factory { request -> httpClient().newCall(request) })
    val streams = ResolvingDataSource.Factory(http) { spec ->
        val address = spec.uri.toString().toHttpUrlOrNull()
        if (address == null || !address.isPlaylistFile()) {
            spec
        } else {
            spec.withUri(Uri.parse(playlists.resolve(address).toString()))
        }
    }
    return ResolvingDataSource.Factory(DefaultDataSource.Factory(context, streams)) { spec ->
        if (spec.uri.isStreamAddress() || spec.uri.isMediaStoreAudioItem()) spec else throw RefusedAddressException()
    }
}
