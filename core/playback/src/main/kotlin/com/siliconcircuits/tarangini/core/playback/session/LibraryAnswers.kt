/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Reads from the library for a caller. The caller always gets an answer:
 * a "bad value" error when [read] gives null (an id this app did not make,
 * or one that names nothing), and an error when the read fails. Nothing
 * here can crash the service.
 */
internal fun <T : Any> CoroutineScope.fromLibrary(
    read: suspend () -> LibraryResult<T>?,
): ListenableFuture<LibraryResult<T>> {
    val answer = SettableFuture.create<LibraryResult<T>>()
    // The read starts in this call, so an answer that needs no wait is there when the call returns.
    launch(start = CoroutineStart.UNDISPATCHED) {
        val result = runCatching { read() ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE) }
        answer.set(result.getOrElse { LibraryResult.ofError(SessionError.ERROR_UNKNOWN) })
    }
    return answer
}

/**
 * One page of a list as an answer. The car has no paging: it asks for
 * everything at once and gets the whole list. A caller that does ask for a
 * page gets that page, because Media3 refuses an answer with more rows
 * than were asked for.
 */
internal fun List<MediaItem>.asPage(
    page: Int,
    pageSize: Int,
    params: LibraryParams?,
): LibraryResult<ImmutableList<MediaItem>> {
    // The numbers come from the caller. Long keeps a large page number from wrapping around.
    val size = pageSize.coerceAtLeast(0)
    val skipped = (page.coerceAtLeast(0).toLong() * size).coerceAtMost(this.size.toLong()).toInt()
    return LibraryResult.ofItemList(drop(skipped).take(size), params)
}

/** The error as the answer to a read of the library. */
// An answer made from a session error is API of Media3 that may still change.
@OptIn(UnstableApi::class)
internal fun <T : Any> SessionError.asResult(params: LibraryParams?): LibraryResult<T> =
    params?.let { LibraryResult.ofError(this, it) } ?: LibraryResult.ofError(this)
