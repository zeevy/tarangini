/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

/**
 * What the compact card shows. The player and the mini player both make one.
 *
 * @property durationMs the length of the item, or null for a live stream
 * @property hasError true after the player stopped on an error
 * @property isStream true for a radio stream, which says "Live" when it has no artist
 */
internal data class CompactSong(
    val title: String?,
    val artist: String?,
    val positionMs: Long,
    val durationMs: Long?,
    val showPause: Boolean,
    val isBuffering: Boolean,
    val hasError: Boolean = false,
    val isStream: Boolean = false,
) {
    /** True while sound comes out, so the bars move. */
    val isMoving: Boolean get() = showPause && !isBuffering && !hasError
}

/** The item of the player as the compact card shows it. */
internal fun NowPlayingUiState.toCompact() =
    CompactSong(title, artist, positionMs, durationMs, showPause, isBuffering, error != null, isLive)
