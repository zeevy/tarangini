/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.session

import android.content.ContentResolver
import android.provider.MediaStore
import androidx.media3.common.MediaItem

/**
 * Turns an item a caller asked to play into an item the player can open.
 * Only an audio file known to MediaStore is accepted. A file path, a web
 * address or the content of another app is refused.
 *
 * @throws IllegalArgumentException when the item names nothing this app may play
 */
internal fun MediaItem.toPlayable(): MediaItem {
    val uri = requireNotNull(localConfiguration?.uri ?: requestMetadata.mediaUri) { "The item names no file" }
    require(uri.scheme == ContentResolver.SCHEME_CONTENT && uri.authority == MediaStore.AUTHORITY) {
        "Only audio files from MediaStore can be played"
    }
    return buildUpon().setUri(uri).build()
}
