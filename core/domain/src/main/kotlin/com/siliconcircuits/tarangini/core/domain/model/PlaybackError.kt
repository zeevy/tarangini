/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/** Why playback stopped, in words a screen can turn into a message. */
enum class PlaybackError {
    /** The file is gone or the app may not read it. */
    FILE_NOT_AVAILABLE,

    /** The network or the server did not answer. */
    NETWORK,

    /** The file or stream is in a form the player cannot play. */
    CANNOT_PLAY,

    /** Anything else. */
    UNKNOWN,
}
