/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

/**
 * What the person can do on the now playing screen.
 *
 * @property onSeek jumps to a place in the item, in milliseconds
 * @property onToggleFavourite marks the song that plays as a favourite, or takes the mark away
 */
internal data class NowPlayingActions(
    val onTogglePlay: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onToggleShuffle: () -> Unit,
    val onCycleRepeat: () -> Unit,
    val onToggleFavourite: () -> Unit = {},
)
