/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import com.siliconcircuits.tarangini.core.domain.model.Song

/**
 * What a row of a song can do.
 *
 * @property onPlay plays the song with its list as the queue
 * @property onPlayNext puts the song right after the one that plays now
 * @property onAddToQueue puts the song at the end of the queue
 */
class SongRowActions(val onPlay: (Song) -> Unit, val onPlayNext: (Song) -> Unit, val onAddToQueue: (Song) -> Unit)
