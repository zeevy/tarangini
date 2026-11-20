/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * A list of songs the person made.
 *
 * @property songCount how many of its songs can be played now; a song whose file is gone is not counted
 */
data class Playlist(val id: Long, val name: String, val songCount: Int)

/**
 * One place in a playlist. The same song can be in a playlist more than once, so a place has its own id.
 *
 * @property itemId the id of the place, which stays the same when the place moves
 */
data class PlaylistSong(val itemId: Long, val song: Song)

/** The longest name a playlist can have. */
const val MAX_PLAYLIST_NAME = 100

/** The name of a playlist as it is saved: no space at the ends. Null when nothing is left or it is too long. */
fun checkPlaylistName(typed: String): String? =
    typed.trim().takeIf { it.isNotEmpty() && it.length <= MAX_PLAYLIST_NAME }
