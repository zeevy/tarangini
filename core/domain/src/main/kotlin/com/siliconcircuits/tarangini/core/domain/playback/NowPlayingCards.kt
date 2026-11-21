/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.playback

/**
 * What a view of the playback outside the app shows, such as the home screen widget.
 *
 * @property title the title of the item, or null when it has none
 * @property artist the artist, or null when it has none
 * @property songUri the MediaStore address of the song, from which its cover can be read. Null for
 *   an item that is no song of the phone
 * @property showsPause true when a play button shows "pause", by the same rule as the screens
 */
data class NowPlayingCard(val title: String?, val artist: String?, val songUri: String?, val showsPause: Boolean)

/**
 * A view of the playback outside the app. The playback service tells every view about each change
 * of the item and of play and pause. A module adds a view to the set through Hilt.
 */
fun interface NowPlayingCards {
    /** Shows [card], or that nothing is loaded when it is null. */
    suspend fun show(card: NowPlayingCard?)
}
