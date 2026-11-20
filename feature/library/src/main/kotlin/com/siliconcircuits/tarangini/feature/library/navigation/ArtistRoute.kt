/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.navigation

import kotlinx.serialization.Serializable

/** The screen of one artist: the albums and the songs. */
@Serializable
data class ArtistRoute(val artistId: Long, val name: String = "") {
    internal companion object {
        const val ARTIST_ID = "artistId"
        const val NAME = "name"
    }
}
