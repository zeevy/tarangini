/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings.backup

/**
 * What the person can do with a backup.
 *
 * @property onBackUp opens the file picker to write a backup
 * @property onPickFile opens the file picker to choose a backup to read
 * @property onRestore reads the chosen file; true replaces what is here
 * @property onCancel the person chose a file and then decided against reading it
 * @property onClose the person has read a result
 */
internal class BackupActions(
    val onBackUp: () -> Unit = {},
    val onPickFile: () -> Unit = {},
    val onRestore: (replace: Boolean) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onClose: () -> Unit = {},
)
