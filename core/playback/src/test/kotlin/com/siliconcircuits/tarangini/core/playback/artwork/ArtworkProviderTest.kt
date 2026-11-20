/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.artwork

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Process
import java.io.FileNotFoundException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowBinder

@RunWith(RobolectricTestRunner::class)
class ArtworkProviderTest {
    private val asked = mutableListOf<Uri>()
    private val provider = Robolectric.buildContentProvider(ArtworkProvider::class.java)
        .create(ArtworkAddress.AUTHORITY)
        .get()
        .apply {
            callers = { packageName, uid -> packageName == CAR && uid == CAR_UID }
            thumbnail = { source ->
                asked += source
                Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            }
        }

    private fun address(path: String) = Uri.parse("content://${ArtworkAddress.AUTHORITY}$path")

    private fun refused(path: String, mode: String = "r") {
        assertThrows(FileNotFoundException::class.java) { provider.openFile(address(path), mode) }
    }

    @After
    fun forgetTheCaller() = ShadowBinder.reset()

    @Test
    fun `the cover of a song and of an album are read from the audio files of the phone`() {
        val song = provider.openFile(
            requireNotNull(ArtworkAddress.ofSong("content://media/external/audio/media/42")),
            "r",
        )
        val album = provider.openFile(ArtworkAddress.ofAlbum(7), "r")

        song.close()
        album.close()

        assertEquals(
            listOf("content://media/external/audio/media/42", "content://media/external/audio/albums/7"),
            asked.map(Uri::toString),
        )
        assertEquals("image/jpeg", provider.getType(ArtworkAddress.ofAlbum(7)))
    }

    @Test
    fun `an address that is not a song or an album is refused`() {
        listOf(
            "/",
            "/song",
            "/song/",
            "/file/1",
            "/song/abc",
            "/song/-1",
            "/song/1/albumart",
            "/song/../../databases/library",
            "/song/%2E%2E%2Fshared_prefs",
            "/album/1234567890123456789",
            "/data/data/com.siliconcircuits.tarangini/files/settings",
        ).forEach(::refused)

        assertEquals(emptyList<Uri>(), asked)
        assertNull(provider.getType(address("/file/1")))
    }

    @Test
    fun `nothing can be written`() {
        listOf("w", "rw", "rwt", "wa").forEach { mode -> refused("/song/1", mode) }

        assertNull(provider.insert(address("/song/1"), ContentValues()))
        assertEquals(0, provider.update(address("/song/1"), ContentValues(), null, null))
        assertEquals(0, provider.delete(address("/song/1"), null, null))
        assertNull(provider.query(address("/song/1"), null, null, null, null))
    }

    @Test
    fun `an app that is not this app or the car gets no picture`() {
        ShadowBinder.setCallingUid(Process.myUid() + 1)

        refused("/song/1")
        assertEquals(emptyList<Uri>(), asked)
    }

    @Test
    fun `the media panel of the system gets a picture`() {
        val systemUi = Process.myUid() + 1
        ShadowBinder.setCallingUid(systemUi)
        // The test environment keeps granted permissions per process, and the caller is in this process.
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions("android.permission.MEDIA_CONTENT_CONTROL")

        provider.openFile(ArtworkAddress.ofAlbum(7), "r").close()

        assertEquals(1, asked.size)
    }

    @Test
    fun `a song with no picture gives no file`() {
        provider.thumbnail = { throw FileNotFoundException("No album art") }

        refused("/song/1")
    }

    private companion object {
        const val CAR = "com.google.android.projection.gearhead"
        const val CAR_UID = 12_345
    }
}
