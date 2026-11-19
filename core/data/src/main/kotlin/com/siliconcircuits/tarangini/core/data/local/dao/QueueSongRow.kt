/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.Embedded

/** A song of the saved queue with the queue position it was saved at. */
internal data class QueueSongRow(val position: Int, @Embedded val row: SongRow)
