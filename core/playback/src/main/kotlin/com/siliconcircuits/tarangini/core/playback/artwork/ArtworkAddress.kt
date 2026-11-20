/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.artwork

import android.net.Uri
import com.siliconcircuits.tarangini.core.domain.model.artworkUriOf

/**
 * The addresses of the cover pictures this app serves through
 * [ArtworkProvider]. The car and the lock screen load a picture from the
 * address in an item. They cannot read the audio files of the phone
 * themselves, so the address must be one of this app.
 */
internal object ArtworkAddress {
    /** The name of the provider. It is the application id plus ".artwork", as in the manifest. */
    const val AUTHORITY = "com.siliconcircuits.tarangini.artwork"

    const val SONG = "song"
    const val ALBUM = "album"

    /** The cover of the song at [mediaUri], or null when the address is not a song of the phone. */
    fun ofSong(mediaUri: String): Uri? =
        artworkUriOf(mediaUri)?.let { Uri.parse(mediaUri).lastPathSegment }?.let { id -> address(SONG, id) }

    /** The cover of an album. */
    fun ofAlbum(albumId: Long): Uri = address(ALBUM, albumId.toString())

    private fun address(kind: String, id: String): Uri =
        Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(kind).appendPath(id).build()
}
