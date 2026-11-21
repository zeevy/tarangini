/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

/** What the person can do on the Settings screen. */
internal class SettingsActions(
    val onSetMinTrackSeconds: (Int) -> Unit = {},
    val onSetFolderHidden: (path: String, hidden: Boolean) -> Unit = { _, _ -> },
    val onRescan: () -> Unit = {},
    val onClearHistory: () -> Unit = {},
    val onOpenSource: () -> Unit = {},
    val onOpenLicences: () -> Unit = {},
    val onOpenPrivacy: () -> Unit = {},
)
