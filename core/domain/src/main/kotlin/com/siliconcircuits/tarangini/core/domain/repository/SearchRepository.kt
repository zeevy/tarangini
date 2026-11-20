/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.SearchResults

/**
 * Search of the library. The search field of the phone, the search screen of
 * the car and a spoken "play ..." all ask here, so they find the same things.
 */
interface SearchRepository {
    /**
     * Finds songs, albums and artists for [text], which a person typed or
     * spoke. A word of the text matches a word that starts with it, in any
     * case and in any script. Signs between words do not count.
     *
     * Each group has at most [limit] rows. A name that starts with the whole
     * text comes first, then a song with every word in its title, then the
     * rest; inside these by name. A text with no letter and no digit finds
     * nothing.
     *
     * @throws IllegalArgumentException when [limit] is not from 1 to [MAX_LIMIT]
     */
    suspend fun search(text: String, limit: Int = DEFAULT_LIMIT): SearchResults

    companion object {
        /** The rows of a group when the caller names no number. */
        const val DEFAULT_LIMIT = 20

        /** The most rows a group can have. */
        const val MAX_LIMIT = 100
    }
}
