/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.lyrics

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import com.siliconcircuits.tarangini.core.domain.repository.SongLyrics
import com.siliconcircuits.tarangini.core.playback.di.StreamIoDispatcher
import com.siliconcircuits.tarangini.core.playback.session.isMediaStoreAudioItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Lyrics from the tags of the music files on the phone.
 *
 * Only a MediaStore audio item is opened, the same rule the player has, so an address from
 * somewhere else cannot make the app read another file. A radio stream has no lyrics here.
 */
internal class DefaultSongLyrics @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:StreamIoDispatcher private val io: CoroutineDispatcher,
) : SongLyrics {
    override suspend fun lyricsOf(mediaUri: String): String? = withContext(io) {
        val uri = Uri.parse(mediaUri)
        if (!uri.isMediaStoreAudioItem()) return@withContext null
        // A file that is gone or faulty has no lyrics to show. That is no error for the person.
        runCatching {
            val files = context.contentResolver
            val length = runCatching { files.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull()
            TagReader(length ?: C.LENGTH_UNSET.toLong()) { files.openInputStream(uri) }
                .metadata()
                ?.let(LyricsFrames::textOf)
        }.onFailure { if (it is CancellationException) throw it }.getOrNull()
    }
}
