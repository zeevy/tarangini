/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/** The orders the list of all songs can have. */
enum class SongSort {
    /** By title, A to Z. */
    TITLE,

    /** By artist name, then by title. */
    ARTIST,

    /** By album title, then in album order. */
    ALBUM,

    /** The newest file first. */
    DATE_ADDED,

    /** The shortest song first. */
    DURATION,
}
