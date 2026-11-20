/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.playlist

import com.siliconcircuits.tarangini.core.domain.model.PlaylistSong

/**
 * What the person can do on the screen of a playlist.
 *
 * @property onPlay plays the playlist from a place on, or from its start when the place is null
 */
internal class PlaylistActions(
    val onPlay: (PlaylistSong?) -> Unit = {},
    val onShuffle: () -> Unit = {},
    val onRename: (String) -> Unit = {},
    val onDelete: () -> Unit = {},
    val onRemove: (PlaylistSong) -> Unit = {},
    val onMove: (PlaylistSong, PlaylistMove) -> Unit = { _, _ -> },
    val onMore: () -> Unit = {},
    val onRetry: () -> Unit = {},
)
