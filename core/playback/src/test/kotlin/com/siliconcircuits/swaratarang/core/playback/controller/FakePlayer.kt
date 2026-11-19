/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.controller

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.robolectric.Shadows.shadowOf

/**
 * A player for tests. A test changes what the player reports with [report], and reads [commands]
 * to see what it was told to do. It plays nothing.
 */
// The base class for a player written by hand is API that Media3 may still change.
@OptIn(UnstableApi::class)
internal class FakePlayer : SimpleBasePlayer(Looper.getMainLooper()) {
    private var now: State = State.Builder()
        .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
        .setContentPositionMs(PositionSupplier { positionMs })
        .build()

    /** Every command received, oldest first, as short text such as `pause` or `seek to item 1 at 0`. */
    val commands = mutableListOf<String>()

    /**
     * Where the current item has played to. Changing it sends no event, the same as a real
     * player while it plays. The next [report] tells the listeners.
     */
    var positionMs = 0L

    /** Changes what the player reports and tells the listeners, as a real player does after something happened. */
    fun report(change: State.Builder.() -> Unit = {}) {
        now = now.buildUpon().apply(change).build()
        invalidateState()
        shadowOf(Looper.getMainLooper()).idle()
    }

    override fun getState(): State = now

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        commands += if (playWhenReady) "play" else "pause"
        if (playWhenReady && heldBack) return Futures.immediateVoidFuture()
        now = now.buildUpon().setPlayWhenReady(playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST).build()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        commands += "prepare"
        val prepared = if (now.playlist.isEmpty()) STATE_ENDED else STATE_READY
        now = now.buildUpon().setPlayerError(null).setPlaybackState(prepared).build()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        commands += "load ${mediaItems.map { it.mediaId }} from $startIndex"
        now = now.buildUpon()
            .setPlaylist(mediaItems.map(::loaded))
            .setCurrentMediaItemIndex(startIndex)
            .build()
        positionMs = startPositionMs.coerceAtLeast(0)
        return Futures.immediateVoidFuture()
    }

    override fun handleAddMediaItems(index: Int, mediaItems: List<MediaItem>): ListenableFuture<*> {
        commands += "add ${mediaItems.map { it.mediaId }} at $index"
        changeQueue { addAll(index, mediaItems.map(::loaded)) }
        val hook = afterNextAdd
        afterNextAdd = null
        hook?.invoke()
        return Futures.immediateVoidFuture()
    }

    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> {
        commands += "move $fromIndex to $newIndex"
        changeQueue {
            val moved = subList(fromIndex, toIndex).toList()
            subList(fromIndex, toIndex).clear()
            addAll(newIndex, moved)
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> {
        commands += if (toIndex - fromIndex == now.playlist.size) "clear" else "remove $fromIndex until $toIndex"
        changeQueue { subList(fromIndex, toIndex).clear() }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        commands += "shuffle $shuffleModeEnabled"
        now = now.buildUpon().setShuffleModeEnabled(shuffleModeEnabled).build()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        commands += "repeat $repeatMode"
        now = now.buildUpon().setRepeatMode(repeatMode).build()
        return Futures.immediateVoidFuture()
    }

    /** True makes the player take a "play" command and stay silent, as a real player does during a call. */
    var heldBack = false

    /** Runs once, right after the next "add" command. A test uses it to do something in the middle of a long load. */
    var afterNextAdd: (() -> Unit)? = null

    /** Changes the queue the way a real player does: the current item stays the current one when it is still there. */
    private fun changeQueue(change: MutableList<MediaItemData>.() -> Unit) {
        val before = now.playlist
        val oldIndex = currentMediaItemIndex
        val currentUid = before.getOrNull(oldIndex)?.uid
        val after = before.toMutableList().apply(change)
        val newIndex = after.indexOfFirst { it.uid == currentUid }.takeIf { it >= 0 }
            ?: oldIndex.coerceIn(0, (after.size - 1).coerceAtLeast(0))
        now = now.buildUpon()
            .setPlaylist(after)
            .setCurrentMediaItemIndex(newIndex)
            .apply { if (after.isEmpty() && now.playbackState != STATE_IDLE) setPlaybackState(STATE_ENDED) }
            .build()
    }

    private var loadedCount = 0

    /** A queue entry with its own key, so that the same file can be in the queue twice. */
    private fun loaded(item: MediaItem): MediaItemData =
        MediaItemData.Builder("loaded ${loadedCount++}").setMediaItem(item).build()

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val position = positionMs.coerceAtLeast(0)
        commands += "seek to item $mediaItemIndex at $position"
        now = now.buildUpon().setCurrentMediaItemIndex(mediaItemIndex).build()
        this.positionMs = position
        return Futures.immediateVoidFuture()
    }

    companion object {
        /** A queue entry for the playlist of the reported state. [tags] is what the player read from the file. */
        fun queued(item: MediaItem, tags: MediaMetadata? = null, durationMs: Long? = null): MediaItemData =
            MediaItemData.Builder(item.mediaId)
                .setMediaItem(item)
                .setMediaMetadata(tags)
                .apply { if (durationMs != null) setDurationUs(durationMs * MICROS_IN_MILLI) }
                .build()

        fun queued(id: String): MediaItemData = queued(MediaItem.Builder().setMediaId(id).build())

        private const val MICROS_IN_MILLI = 1000
    }
}
