/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.list

import com.siliconcircuits.tarangini.core.domain.model.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The rows of a long list that a screen has read so far.
 *
 * @property isLoading true until the first read has ended
 * @property hasMore true when the library may have rows after [items]
 * @property failed true when the last read from the first row went wrong. The rows read before stay
 */
internal data class PagedItems<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = false,
    val failed: Boolean = false,
)

/**
 * Reads a long list of the library page by page, as the person scrolls.
 * [refresh] reads the rows again after the library changed, and [loadMore]
 * adds the next page.
 *
 * Rows can move between two reads while a scan writes. A row with a [key]
 * that is in the list already is left out, because a list must never hold
 * the same key twice.
 */
internal class PagedList<T>(
    private val scope: CoroutineScope,
    private val key: (T) -> Any,
    private val read: suspend (Page) -> List<T>,
) {
    private val rows = MutableStateFlow(PagedItems<T>())
    val state: StateFlow<PagedItems<T>> = rows.asStateFlow()

    private var refreshing: Job? = null
    private var adding: Job? = null

    /** True when the next page was asked for while a refresh ran. The refresh adds it at its end. */
    private var moreWanted = false

    /**
     * Reads the list again from its first row: as many rows as are shown
     * now, or one page when [fromTop] is true or nothing is shown yet. The
     * old rows stay on screen until the new ones are there.
     */
    fun refresh(fromTop: Boolean = false) {
        refreshing?.cancel()
        adding?.cancel()
        refreshing = scope.launch {
            val wanted = if (fromTop) PAGE_SIZE else maxOf(rows.value.items.size, PAGE_SIZE)
            attempt { readFirst(wanted) }
                .onSuccess {
                    rows.value = PagedItems(it.distinctBy(key), isLoading = false, hasMore = it.size >= wanted)
                }
                .onFailure { rows.value = rows.value.copy(isLoading = false, failed = true) }
            if (moreWanted) {
                moreWanted = false
                addPage()
            }
        }
    }

    /** Adds the next page. While a refresh runs the page is added after it, and one page is added at a time. */
    fun loadMore() {
        when {
            refreshing?.isActive == true -> moreWanted = true
            adding?.isActive != true -> adding = scope.launch { addPage() }
        }
    }

    private suspend fun addPage() {
        val shown = rows.value
        if (!shown.hasMore) return
        // A page that cannot be read is asked for again when the person scrolls on.
        attempt { read(Page(PAGE_SIZE, shown.items.size)) }.onSuccess { more ->
            rows.value = shown.copy(items = (shown.items + more).distinctBy(key), hasMore = more.size == PAGE_SIZE)
        }
    }

    private suspend fun readFirst(count: Int): List<T> {
        val fresh = mutableListOf<T>()
        while (fresh.size < count) {
            val limit = minOf(count - fresh.size, Page.MAX_LIMIT)
            val page = read(Page(limit, fresh.size))
            fresh += page
            if (page.size < limit) break
        }
        return fresh
    }

    companion object {
        /** The rows one scroll step reads. A screen shows about ten rows, so this is many screens. */
        const val PAGE_SIZE = 100
    }
}
