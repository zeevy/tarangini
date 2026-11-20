/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.siliconcircuits.tarangini.core.playback.browse.MediaId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Answers the apps that talk to the media session. It checks the caller and every item a caller asks to play.
 *
 * @param scope runs the reads of the library and of the saved queue, on the main thread
 * @param car what the car reads and plays: its list, its search, and its requests with no address
 * @param texts gives the items of the queue their title, artist and album
 * @param spokenDone is told how a spoken request of a caller ended: null when its queue was found
 * @param resumption gives what to play when a caller asks to go on and the player is empty, or null for nothing
 */
// Media3 marks parts of the session API as API that may still change: the "trusted by the system" flag,
// the default command sets and everything about playback resumption.
@OptIn(UnstableApi::class)
internal class PlaybackSessionCallback(
    private val callers: CallerPolicy,
    private val scope: CoroutineScope,
    private val car: CarLibrary,
    private val texts: suspend (List<MediaItem>) -> List<MediaItem>,
    private val spokenDone: (MediaSession, MediaSession.ControllerInfo, NothingToPlay?) -> Unit,
    private val resumption: suspend () -> MediaItemsWithStartPosition?,
) : MediaLibrarySession.Callback {
    /** How many tabs the car that asked last shows. */
    private var tabLimit = MAX_TABS

    /** The lists a caller has read. These are told when the library changes. */
    val opened = OpenedLists()

    /** The heart button of the session. The service sets it once the session exists, as the button needs it. */
    var favourite: FavouriteButton? = null

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult =
        when (callers.accessFor(controller.packageName, controller.uid, controller.isTrusted)) {
            // The car needs the library commands to browse and the player commands to play what it picked.
            // What it may play is still checked item by item in onAddMediaItems.
            CallerAccess.FULL, CallerAccess.CAR -> MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.withFavourite(),
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
            )

            // The media panel of the phone is such a caller. Its heart button marks only the song that plays.
            CallerAccess.TRANSPORT_ONLY -> MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.withFavourite(),
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
        return Futures.immediateFuture(LibraryResult.ofItem(car.tree.root, params))
    }

    /**
     * What is under a node of the list. While the audio permission is not given on the phone, every
     * list is an error with a text that says so; Media3 shows such an error in the car. An empty
     * library gives an empty Library tab and a note that says why.
     */
    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.fromLibrary {
        val parent = MediaId.parse(parentId)
        val refused = car.problems.noAccess()
        val empty = if (parent == MediaId.Library && refused == null) car.problems.noSongs() else null
        // A list that was asked for is told when the library changes, also when it could not be shown now.
        if (parent != null) opened.add(parentId)
        empty?.let { note -> session.sendError(browser, note) }
        when {
            parent == null -> null
            refused != null -> refused.asResult(params)
            empty != null -> emptyList<MediaItem>().asPage(page, pageSize, params)
            else -> car.tree.children(parent, tabLimit)?.asPage(page, pageSize, params)
        }
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> = scope.fromLibrary {
        MediaId.parse(mediaId)?.let { id -> car.tree.item(id) }?.let { item -> LibraryResult.ofItem(item, null) }
    }

    /**
     * A search typed or spoken in the car. The caller is told how many
     * results there are and then asks for them with [onGetSearchResult].
     * The text comes from another app and is used only as text to search
     * for. A search that fails counts as one with no result.
     */
    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> {
        // The count is sent after this call has returned: Media3 listens for it only from then on.
        scope.launch {
            val count = runCatching { car.search.results(query).size }.getOrDefault(0)
            session.notifySearchResultChanged(browser, query, count, params)
        }
        return Futures.immediateFuture(LibraryResult.ofVoid(params))
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.fromLibrary {
        // Without the audio permission nothing found could play, so the search says what to do, as a list does.
        car.problems.noAccess()?.let { refused -> refused.asResult(params) }
            ?: car.search.results(query).asPage(page, pageSize, params)
    }

    /**
     * Two requests name no address. A tap on a song in the car sends only
     * the id of the song; the id names the list the song was shown in, and
     * that list is played from the song on. A spoken "play ..." sends the
     * words; what they find in the library is played. Every other request
     * goes the usual way through [onAddMediaItems], where an item must name
     * an address.
     */
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaItemsWithStartPosition> = mediaItems.singleOrNull()
        ?.let { item -> car.play.queueFor(item) { missing -> spokenDone(mediaSession, controller, missing) } }
        ?: super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)

    /** The one command of our own: a press of the heart button. Only a caller that was let in gets this far. */
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> = if (customCommand.customAction == FavouriteButton.COMMAND.customAction) {
        favourite?.toggle()
        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    } else {
        super.onCustomCommand(session, controller, customCommand, args)
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

private fun SessionCommands.withFavourite(): SessionCommands = buildUpon().add(FavouriteButton.COMMAND).build()

/** The most tabs a car shows. A car can ask for fewer. */
private const val MAX_TABS = 4
