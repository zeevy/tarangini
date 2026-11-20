/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.navigation

import kotlinx.serialization.Serializable

/** The screen of one folder: the folders and the songs directly inside it. */
@Serializable
data class FolderRoute(val folderId: Long, val name: String = "") {
    internal companion object {
        const val FOLDER_ID = "folderId"
        const val NAME = "name"
    }
}
