/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * One folder of the phone that holds songs, or holds a folder that does.
 *
 * @property parentId the folder it is in, or null for a top folder
 * @property name the last part of [path], which a list shows
 */
data class Folder(val id: Long, val parentId: Long?, val name: String, val path: String)
