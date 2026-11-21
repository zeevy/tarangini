/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.card

import android.net.Uri
import androidx.media3.common.Player
import com.siliconcircuits.tarangini.core.domain.playback.NowPlayingCard
import com.siliconcircuits.tarangini.core.domain.playback.NowPlayingCards
import com.siliconcircuits.tarangini.core.playback.session.isMediaStoreAudioItem
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Tells the views outside the app, such as the home screen widget, what the player plays. A player
 * listener, added in the playback service.
 *
 * Only a change a view shows is passed on: a card equal to the last one, as after a seek, is not sent
 * again. A view that is slow gets only the newest card: an older one that it did not show yet is
 * dropped. The card of the player at the start is not sent: the service starts with an empty player
 * and restores the saved queue a moment later, and a view must keep the last song until then.
 */
internal class NowPlayingFeed(
    player: Player,
    private val cards: Set<NowPlayingCards>,
    private val scope: CoroutineScope,
) : Player.Listener {
    private val card = MutableStateFlow(cardOf(player))
    private val feed = scope.launch { card.drop(1).collectLatest(::showEverywhere) }

    override fun onEvents(player: Player, events: Player.Events) {
        card.value = cardOf(player)
    }

    /** The service ends, so nothing plays any more. The views show the item with a play button. */
    fun end() {
        feed.cancel()
        val last = card.value?.copy(showsPause = false)
        scope.launch { showEverywhere(last) }
    }

    private suspend fun showEverywhere(now: NowPlayingCard?) = cards.forEach { view ->
        // A view that fails must not stop the others, and must not end the playback.
        runCatching { view.show(now) }.onFailure { if (it is CancellationException) throw it }
    }
}

/**
 * What a view shows of the item the player has, or null when it has none.
 *
 * The play button follows the rule of the screens: it shows "pause" from the tap on play, also
 * while the player loads, and "play" after an error and at the end of the queue.
 */
internal fun cardOf(player: Player): NowPlayingCard? = player.currentMediaItem?.let { item ->
    val texts = player.mediaMetadata
    NowPlayingCard(
        title = texts.title?.toString()?.take(MAX_TEXT),
        artist = texts.artist?.toString()?.take(MAX_TEXT),
        songUri = item.mediaId.takeIf { Uri.parse(it).isMediaStoreAudioItem() },
        showsPause = player.playWhenReady && player.playerError == null && player.playbackState != Player.STATE_ENDED,
    )
}

/** A view shows one or two lines. Longer texts are cut before they leave the service. */
private const val MAX_TEXT = 200
