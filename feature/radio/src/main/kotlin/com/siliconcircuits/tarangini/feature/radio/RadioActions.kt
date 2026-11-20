/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio

import com.siliconcircuits.tarangini.core.domain.model.Station

/**
 * What the Radio tab can do.
 *
 * @property onRemove removes the station. The screen has asked the person before it calls this
 */
internal data class RadioActions(
    val onPlay: (Station) -> Unit,
    val onAdd: () -> Unit,
    val onEdit: (Station) -> Unit,
    val onRemove: (Station) -> Unit,
)
