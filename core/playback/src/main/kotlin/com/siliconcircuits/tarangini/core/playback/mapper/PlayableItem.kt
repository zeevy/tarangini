/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.mapper

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.siliconcircuits.tarangini.core.domain.model.artworkUriOf

/**
 * The item the player gets for one address: the id, the address and where
 * the cover picture is asked for. The title and the other texts are read
 * from the file by the player. Every item of the queue is made here, so the
 * notification and the lock screen show the same picture as the app.
 */
internal fun playableItem(mediaId: String, uri: Uri = Uri.parse(mediaId)): MediaItem = MediaItem.Builder()
    .setMediaId(mediaId)
    .setUri(uri)
    .setMediaMetadata(MediaMetadata.Builder().setArtworkUri(artworkUriOf(uri.toString())?.let(Uri::parse)).build())
    .build()
