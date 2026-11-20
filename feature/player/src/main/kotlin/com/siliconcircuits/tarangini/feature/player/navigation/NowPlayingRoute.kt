/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.navigation

import kotlinx.serialization.Serializable

/** The now playing screen. It opens over any screen, and the mini player is hidden on it. */
@Serializable
data object NowPlayingRoute

/** The queue. It opens from the now playing screen. */
@Serializable
internal data object QueueRoute
