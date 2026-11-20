/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * One part of a long list: [limit] rows from row [offset] on. A list is
 * always read in parts, so a library of any size never sits in memory whole.
 */
data class Page(val limit: Int = DEFAULT_LIMIT, val offset: Int = 0) {
    init {
        require(limit in 1..MAX_LIMIT) { "A page has 1 to $MAX_LIMIT rows, not $limit" }
        require(offset >= 0) { "A page cannot start before the first row" }
    }

    /** The page that follows this one. */
    fun next(): Page = copy(offset = offset + limit)

    companion object {
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 500
    }
}
