/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.mapper

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/** The extra of a queue item that holds the genre keys of its song. Only this app reads it. */
private const val GENRE_KEYS = "com.siliconcircuits.tarangini.GENRE_KEYS"

/** The genre keys the playback service gave this item, or none. */
internal fun MediaItem.genreKeys(): List<String> = mediaMetadata.extras?.getStringArrayList(GENRE_KEYS).orEmpty()

/** The same texts, with [keys] as the genres of the song. The other extras stay. */
internal fun MediaMetadata.withGenreKeys(keys: List<String>): MediaMetadata {
    if (keys.isEmpty()) return this
    val extras = Bundle(extras ?: Bundle.EMPTY).apply { putStringArrayList(GENRE_KEYS, ArrayList(keys)) }
    return buildUpon().setExtras(extras).build()
}
