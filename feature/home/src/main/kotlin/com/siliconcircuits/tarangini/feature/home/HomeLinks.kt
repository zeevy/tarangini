/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.home

import com.siliconcircuits.tarangini.core.domain.model.Playlist

/**
 * Where Home leads to.
 *
 * @property onResume goes on with the item that is loaded in the player
 */
internal class HomeLinks(
    val onResume: () -> Unit = {},
    val onOpenLibrary: () -> Unit = {},
    val onOpenPlaylist: (Playlist) -> Unit = {},
)
