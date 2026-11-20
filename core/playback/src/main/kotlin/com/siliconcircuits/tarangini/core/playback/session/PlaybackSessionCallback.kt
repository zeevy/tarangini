/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.siliconcircuits.tarangini.core.playback.browse.BrowseTree
import com.siliconcircuits.tarangini.core.playback.browse.MediaId
import com.siliconcircuits.tarangini.core.playback.mapper.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Answers the apps that talk to the media session. It checks the caller and every item a caller asks to play.
 *
 * @param scope runs the reads of the library and of the saved queue, on the main thread
 * @param tree the list the car shows
 * @param texts gives the items of the queue their title, artist and album
 * @param resumption gives what to play when a caller asks to go on and the player is empty, or null for nothing
 */
// Media3 marks parts of the session API as API that may still change: the "trusted by the system" flag,
// the default command sets and everything about playback resumption.
@OptIn(UnstableApi::class)
internal class PlaybackSessionCallback(
    private val callers: CallerPolicy,
    private val scope: CoroutineScope,
    private val tree: BrowseTree,
    private val texts: suspend (List<MediaItem>) -> List<MediaItem>,
    private val resumption: suspend () -> MediaItemsWithStartPosition?,
) : MediaLibrarySession.Callback {
    /** How many tabs the car that asked last shows. */
    private var tabLimit = MAX_TABS

    /** The lists a caller has read. These are told when the library changes. */
    val opened = OpenedLists()

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult =
        when (callers.accessFor(controller.packageName, controller.uid, controller.isTrusted)) {
            // The car needs the library commands to browse and the player commands to play what it picked.
            // What it may play is still checked item by item in onAddMediaItems.
            CallerAccess.FULL, CallerAccess.CAR -> MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
            )

            CallerAccess.TRANSPORT_ONLY -> MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .removeAll(Player.COMMAND_SET_MEDIA_ITEM, Player.COMMAND_CHANGE_MEDIA_ITEMS)
                    .build(),
            )

            CallerAccess.REFUSED -> MediaSession.ConnectionResult.reject()
        }

    /**
     * The top of the list the car shows. Without an answer here Android Auto
     * cannot connect at all. Only a caller with the library commands gets
     * this far: this app and the car. The car says with this call how many
     * tabs it shows.
     */
    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val asked = params?.extras?.getInt(MediaConstants.EXTRAS_KEY_ROOT_CHILDREN_LIMIT, MAX_TABS) ?: MAX_TABS
        tabLimit = asked.coerceIn(1, MAX_TABS)
        return Futures.immediateFuture(LibraryResult.ofItem(tree.root, params))
    }

    /**
     * What is under a node of the list. The car has no paging: it asks for
     * everything at once and gets the whole list. A caller that does ask
     * for a page gets that page, because Media3 refuses an answer with more
     * rows than were asked for.
     */
    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = fromLibrary {
        MediaId.parse(parentId)?.let { parent -> tree.children(parent, tabLimit) }?.let { children ->
            opened.add(parentId)
            // The numbers come from the caller. Long keeps a large page number from wrapping around.
            val skipped = (
                page.coerceAtLeast(
                    0,
                ).toLong() * pageSize.coerceAtLeast(0)
                ).coerceAtMost(children.size.toLong())
            LibraryResult.ofItemList(children.drop(skipped.toInt()).take(pageSize.coerceAtLeast(0)), params)
        }
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> = fromLibrary {
        MediaId.parse(mediaId)?.let { id -> tree.item(id) }?.let { item -> LibraryResult.ofItem(item, null) }
    }

    /**
     * A tap on a song in the car sends only the id of the song. The id
     * names the list the song was shown in, and that list is played from
     * the song on. Every other request goes the usual way through
     * [onAddMediaItems], where an item must name an address: the car never
     * adds a song to the queue by its id.
     */
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val picked = mediaItems.singleOrNull()?.pickedSong()
            ?: return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
        val answer = SettableFuture.create<MediaItemsWithStartPosition>()
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            // The caller always gets an answer, also when the read or the making of the items fails.
            val queue = runCatching {
                tree.queue(picked)?.run {
                    MediaItemsWithStartPosition(songs.map { it.toMediaItem() }, index, C.TIME_UNSET)
                }
            }.getOrNull()
            if (queue == null) {
                answer.setException(IllegalArgumentException("The library has no such song"))
            } else {
                answer.set(queue)
            }
        }
        return answer
    }

    /**
     * Reads from the library for a caller. The caller always gets an answer:
     * a "bad value" error for an id this app did not make or that names
     * nothing, and an error when the read fails. Nothing here can crash
     * the service.
     */
    private fun <T : Any> fromLibrary(read: suspend () -> LibraryResult<T>?): ListenableFuture<LibraryResult<T>> {
        val answer = SettableFuture.create<LibraryResult<T>>()
        // The read starts in this call, so an answer that needs no wait is there when the call returns.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result = runCatching { read() ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE) }
            answer.set(result.getOrElse { LibraryResult.ofError(SessionError.ERROR_UNKNOWN) })
        }
        return answer
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<List<MediaItem>> {
        val playable = try {
            mediaItems.map { it.toPlayable() }
        } catch (refused: IllegalArgumentException) {
            return Futures.immediateFailedFuture(refused)
        }
        val answer = SettableFuture.create<List<MediaItem>>()
        // The read starts in this call, so an answer that needs no wait is there when the call returns.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                answer.set(texts(playable))
            } finally {
                // The caller always gets an answer, also when the read is given up. A second set does nothing.
                answer.set(playable)
            }
        }
        return answer
    }

    /**
     * Called when a play request comes and the player is empty, for example
     * the play button of a Bluetooth device after the app was killed. The
     * items come from the database of this app and never from the caller.
     */
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean,
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val answer = SettableFuture.create<MediaItemsWithStartPosition>()
        scope.launch {
            // The caller always gets an answer, also when the read fails.
            val saved = runCatching { resumption() }.getOrNull()
            if (saved == null) {
                answer.setException(UnsupportedOperationException("Nothing was playing before"))
            } else {
                answer.set(saved)
            }
        }
        return answer
    }
}

/** The most tabs a car shows. A car can ask for fewer. */
private const val MAX_TABS = 4

/**
 * The song a caller picked from the list of the car, or null for any
 * other item. Such an item has only an id: an item that names an address
 * is checked as an address, whatever its id says.
 */
private fun MediaItem.pickedSong(): MediaId.SongIn? = if (localConfiguration == null &&
    requestMetadata.mediaUri == null
) {
    MediaId.parse(mediaId) as? MediaId.SongIn
} else {
    null
}
