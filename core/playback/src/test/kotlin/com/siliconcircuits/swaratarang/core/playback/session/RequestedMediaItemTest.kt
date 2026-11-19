/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.session

import android.net.Uri
import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RequestedMediaItemTest {
    private val song = "content://media/external/audio/media/42"

    private fun requested(uri: String): MediaItem = MediaItem.Builder()
        .setMediaId("id")
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(uri)).build())
        .build()

    @Test
    fun `an audio file from MediaStore is accepted, asked for either way`() {
        assertEquals(Uri.parse(song), requested(song).toPlayable().localConfiguration?.uri)
        assertEquals(Uri.parse(song), MediaItem.fromUri(song).toPlayable().localConfiguration?.uri)
        assertEquals("id", requested(song).toPlayable().mediaId)
    }

    @Test
    fun `a file path, a web address and the content of another app are refused`() {
        val refused = listOf(
            "file:///data/data/com.siliconcircuits.swaratarang/databases/swara-tarang.db",
            "/sdcard/Music/a.mp3",
            "https://example.org/a.mp3",
            "content://com.other.app.provider/secret",
            "content://media.evil/external/audio/media/42",
            "android.resource://com.siliconcircuits.swaratarang/raw/a",
        )

        refused.forEach { uri ->
            assertThrows(uri, IllegalArgumentException::class.java) { requested(uri).toPlayable() }
        }
    }

    @Test
    fun `an item that names no file is refused`() {
        assertThrows(IllegalArgumentException::class.java) { MediaItem.Builder().setMediaId("id").build().toPlayable() }
    }
}
