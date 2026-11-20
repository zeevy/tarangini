/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import com.siliconcircuits.tarangini.core.domain.model.Station

/** What the row of a station can do. */
data class StationRowActions(
    val onPlay: (Station) -> Unit,
    val onEdit: (Station) -> Unit,
    val onRemove: (Station) -> Unit,
)
