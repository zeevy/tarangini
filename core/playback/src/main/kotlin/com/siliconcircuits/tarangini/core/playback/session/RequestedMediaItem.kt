/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.playback.session

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem

private const val AUDIO_ITEM_SEGMENTS = 4

/**
 * Turns an item a caller asked to play into an item the player can open.
 * Only an audio item of MediaStore is accepted. A file path, a web address,
 * a picture or video, or the content of another app is refused.
 *
 * The result is a new item with only the id and the checked address. Nothing
 * else the caller sent reaches the player: no subtitle, artwork, advert or
 * licence address.
 *
 * @throws IllegalArgumentException when the item names nothing this app may play
 */
internal fun MediaItem.toPlayable(): MediaItem {
    val uri = requireNotNull(localConfiguration?.uri ?: requestMetadata.mediaUri) { "The item names no file" }
    require(uri.isMediaStoreAudioItem()) { "Only audio files from MediaStore can be played" }
    return MediaItem.Builder().setMediaId(mediaId).setUri(uri).build()
}

/** True for `content://media/<volume>/audio/media/<number>` and nothing else. */
private fun Uri.isMediaStoreAudioItem(): Boolean {
    val segments = pathSegments
    val number = segments.lastOrNull().orEmpty()
    return scheme == ContentResolver.SCHEME_CONTENT &&
        authority == MediaStore.AUTHORITY &&
        segments.size == AUDIO_ITEM_SEGMENTS &&
        segments.subList(1, AUDIO_ITEM_SEGMENTS - 1) == listOf("audio", "media") &&
        number.isNotEmpty() &&
        number.all { it in '0'..'9' }
}
