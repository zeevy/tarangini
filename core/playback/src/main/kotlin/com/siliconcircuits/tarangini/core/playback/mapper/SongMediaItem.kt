/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.mapper

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.playback.artwork.ArtworkAddress

/**
 * The song as the player and the car take it: a playable item whose id is
 * the address of the file, with the texts a list or the now playing screen
 * shows. A name the file does not give is left out, so the client shows its
 * own text for it.
 */
internal fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(mediaUri)
    .setUri(mediaUri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist.ifEmpty { null })
            .setAlbumTitle(album.ifEmpty { null })
            .setTrackNumber(trackNumber)
            .setDiscNumber(discNumber)
            .setArtworkUri(ArtworkAddress.ofSong(mediaUri))
            .setDurationMs(durationMs.takeIf { it > 0 })
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build(),
    )
    .build()
