/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.entity

/** The repeat setting as it is stored. The name is stored in the database, so a name never changes. */
internal enum class StoredRepeatMode {
    OFF,
    ONE,
    ALL,
}
