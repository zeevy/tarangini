/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import android.content.ContentUris
import android.provider.MediaStore

/**
 * The address of a local song for the player, made from the id MediaStore
 * gave its file: `content://media/external/audio/media/<id>`. The player
 * takes only addresses of this form.
 *
 * @throws NumberFormatException when [sourceId] is not a number, which a local song never has
 */
internal fun localMediaUri(sourceId: String): String =
    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, sourceId.toLong()).toString()

/** The id MediaStore gave the file behind [mediaUri], or null when the address is not one [localMediaUri] makes. */
internal fun localSourceIdOf(mediaUri: String): String? {
    val number = mediaUri.substringAfterLast('/')
    val isNumber = number.isNotEmpty() && number.all { it in '0'..'9' }
    return number.takeIf { isNumber && mediaUri == "${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}/$number" }
}
