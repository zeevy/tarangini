/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.library

import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import com.siliconcircuits.tarangini.core.domain.repository.SearchRepository

/**
 * A search for tests. It gives [results] for every text that has a letter or
 * a digit, and keeps the texts it was asked for in [searched].
 */
class FakeSearchRepository : SearchRepository {
    var results = SearchResults()
    val searched = mutableListOf<String>()

    override suspend fun search(text: String, limit: Int): SearchResults {
        searched += text
        return if (text.any(Char::isLetterOrDigit)) results else SearchResults()
    }
}
