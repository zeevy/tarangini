/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player

import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController

/**
 * True when the play button pauses. It is true from the tap on play, also while the player still
 * loads, so a second tap pauses. After an error and at the end of the queue the player still says
 * that play was asked for, but nothing plays: the notification offers play then, and so do the screens.
 */
internal val PlaybackState.showsPause: Boolean
    get() = playWhenReady && error == null && !isEnded

/** What a tap on the play button does: pauses when it shows "pause", else goes on with the current item. */
internal fun PlaybackController.togglePlay() {
    if (state.value.showsPause) pause() else resume()
}
