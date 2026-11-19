/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * One item in the player's queue. A text is null when the player does not know it.
 *
 * @property id the id the item was loaded with
 * @property artworkUri the address of the cover picture
 */
data class PlaybackItem(
    val id: String,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artworkUri: String? = null,
)
