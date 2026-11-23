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
 * @property onSeekBy moves on in the item by this many milliseconds, or back for a negative value
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
    val onSeekBy: (Long) -> Unit = {},
)

/**
 * What the person can set for the playback as a whole.
 *
 * @property onSetSpeed sets how fast the queue plays: 1 is the normal speed
 * @property onSleepAfter pauses after this many minutes
 * @property onSleepAtEnd pauses at the end of the item that plays
 */
internal data class PlaybackOptionActions(
    val onSetSpeed: (Float) -> Unit = {},
    val onSleepAfter: (Int) -> Unit = {},
    val onSleepAtEnd: () -> Unit = {},
    val onCancelSleep: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)
