/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import com.siliconcircuits.tarangini.core.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * What a row of a song can do, and which songs are favourites.
 *
 * @property onPlay plays the song with its list as the queue
 * @property onPlayNext puts the song right after the one that plays now
 * @property onAddToQueue puts the song at the end of the queue
 * @property favourites the ids of the favourite songs; a row marks itself when its song is among them
 * @property onSetFavourite marks the song as a favourite (true) or takes the mark away
 * @property playlists what "Add to playlist" needs; without it the menu has no such entry
 */
class SongRowActions(
    val onPlay: (Song) -> Unit,
    val onPlayNext: (Song) -> Unit,
    val onAddToQueue: (Song) -> Unit,
    val favourites: StateFlow<Set<Long>> = MutableStateFlow(emptySet()),
    val onSetFavourite: (Song, Boolean) -> Unit = { _, _ -> },
    val playlists: PlaylistPick? = null,
)
