/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * One album of one artist.
 *
 * @property title the album title, or an empty text for the songs that name no album
 * @property songCount how many of its songs can be played now
 */
data class Album(
    val id: Long,
    val title: String,
    val artistId: Long,
    val artist: String,
    val year: Int?,
    val songCount: Int,
)
