/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.artwork

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.VisibleForTesting
import com.siliconcircuits.tarangini.core.playback.session.CertificateCallers
import com.siliconcircuits.tarangini.core.playback.session.KnownCallers
import java.io.FileNotFoundException

/**
 * Serves the cover pictures of songs and albums as small images, for the
 * car. The car shows only pictures it can load from a `content://` address
 * of the app, and it has no access to the audio files of the phone.
 *
 * It does one thing: a read of `/song/<id>` or `/album/<id>` gives a JPEG of
 * [SIDE] pixels at most. Every other path, every write and every other call
 * is refused. The id is a number and is never used as a path, so no address
 * can reach a file. Only this app, Android Auto and the Google app may read:
 * a cover says what music a person has, which is nothing for other apps.
 */
internal class ArtworkProvider : ContentProvider() {
    /** Who may read besides this app. A test puts its own in. */
    @VisibleForTesting
    var callers: KnownCallers? = null

    /** Reads the picture of a MediaStore address. A test puts its own in. */
    @VisibleForTesting
    var thumbnail: (Uri) -> Bitmap = { source ->
        requireNotNull(context).contentResolver.loadThumbnail(source, Size(SIDE, SIDE), null)
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = if (sourceOf(uri) != null) JPEG else null

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val source = sourceOf(uri)
        if (mode != "r" || source == null || !callerMayRead()) throw FileNotFoundException("No picture at this address")
        // A song with no picture, a file that is gone and a missing audio permission all end the same way.
        val picture = runCatching { thumbnail(source) }.getOrNull() ?: throw FileNotFoundException("No picture")
        return openPipeHelper(uri, JPEG, null, picture) { output, _, _, _, bitmap ->
            // The reader may close early. That ends the write and is no fault.
            runCatching {
                ParcelFileDescriptor.AutoCloseOutputStream(output).use { stream ->
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, QUALITY, stream)
                }
            }
        }
    }

    /** The MediaStore address the picture is read from, or null for an address this provider does not serve. */
    private fun sourceOf(uri: Uri): Uri? {
        val parts = uri.pathSegments
        val id = parts.getOrNull(1)?.takeIf { parts.size == 2 && it.length <= MOST_DIGITS && it.all(Char::isDigit) }
        val base = when (parts.firstOrNull()) {
            ArtworkAddress.SONG -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            ArtworkAddress.ALBUM -> MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
            else -> null
        }
        return if (id == null || base == null) null else ContentUris.withAppendedId(base, id.toLong())
    }

    private fun callerMayRead(): Boolean {
        val uid = Binder.getCallingUid()
        // The system refuses to name the package when the caller lies about it.
        val name = runCatching { callingPackage }.getOrNull()
        val known = callers ?: CertificateCallers(requireNotNull(context).packageManager)
        return uid == Process.myUid() || (name != null && known.isKnown(name, uid))
    }

    // Nothing but a read of a picture is offered.
    override fun query(
        uri: Uri,
        columns: Array<out String>?,
        where: String?,
        args: Array<out String>?,
        order: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(uri: Uri, values: ContentValues?, where: String?, args: Array<out String>?): Int = 0

    override fun delete(uri: Uri, where: String?, args: Array<out String>?): Int = 0

    private companion object {
        /** The longest side of a served picture, in pixels. A car list shows covers smaller than this. */
        const val SIDE = 320
        const val QUALITY = 85
        const val JPEG = "image/jpeg"
        const val MOST_DIGITS = 18
    }
}
