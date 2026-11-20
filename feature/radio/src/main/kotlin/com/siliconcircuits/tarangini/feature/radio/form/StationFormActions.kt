/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio.form

/** What the station form can do. */
internal data class StationFormActions(
    val onNameChange: (String) -> Unit,
    val onAddressChange: (String) -> Unit,
    val onTest: () -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit,
)
