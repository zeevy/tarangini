/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import android.os.Bundle
import androidx.media3.common.MediaMetadata
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.model.QueueSourceKind

// The kind travels in the extras by its name, which never changes. The media type is set too, so a
// controller of another app, such as the car, can tell an album from a playlist.
private const val KIND_KEY = "com.siliconcircuits.tarangini.queue_source_kind"

/** The source as the playlist metadata of the player; no source is empty metadata. */
internal fun QueueSource?.toPlaylistMetadata(): MediaMetadata = if (this == null) {
    MediaMetadata.EMPTY
} else {
    MediaMetadata.Builder()
        .setTitle(name.take(QueueSource.MAX_NAME))
        .setMediaType(kind.mediaType())
        .setExtras(Bundle().apply { putString(KIND_KEY, kind.name) })
        .build()
}

/** The source the playlist metadata names, or null for metadata this app did not write. */
internal fun MediaMetadata.toQueueSource(): QueueSource? {
    val kind = extras?.getString(KIND_KEY)?.let { name -> QueueSourceKind.entries.firstOrNull { it.name == name } }
    return kind?.let { QueueSource.of(it, title?.toString().orEmpty()) }
}

private fun QueueSourceKind.mediaType(): Int = when (this) {
    QueueSourceKind.ALBUM -> MediaMetadata.MEDIA_TYPE_ALBUM
    QueueSourceKind.ARTIST -> MediaMetadata.MEDIA_TYPE_ARTIST
    QueueSourceKind.PLAYLIST, QueueSourceKind.FAVOURITES -> MediaMetadata.MEDIA_TYPE_PLAYLIST
    QueueSourceKind.GENRE -> MediaMetadata.MEDIA_TYPE_GENRE
    QueueSourceKind.FOLDER -> MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
}
