/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.list

import com.siliconcircuits.tarangini.core.domain.model.Page
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedListTest {
    private var source = List(250) { it }
    private val asked = mutableListOf<Page>()

    private fun read(page: Page): List<Int> {
        asked += page
        return source.drop(page.offset).take(page.limit)
    }

    @Test
    fun `it is loading until the first page is read`() = runTest {
        val list = PagedList(backgroundScope, { it }, ::read)
        assertTrue(list.state.value.isLoading)

        list.refresh()
        testScheduler.runCurrent()

        assertFalse(list.state.value.isLoading)
        assertEquals(List(100) { it }, list.state.value.items)
        assertTrue(list.state.value.hasMore)
    }

    @Test
    fun `more pages are added until the end`() = runTest {
        val list = PagedList(backgroundScope, { it }, ::read)
        list.refresh()
        testScheduler.runCurrent()

        repeat(3) {
            list.loadMore()
            testScheduler.runCurrent()
        }

        assertEquals(source, list.state.value.items)
        assertFalse(list.state.value.hasMore)
        assertEquals(listOf(Page(100, 0), Page(100, 100), Page(100, 200)), asked)
    }

    @Test
    fun `a refresh reads as many rows as are shown, in pages the library allows`() = runTest {
        source = List(2_000) { it }
        val list = PagedList(backgroundScope, { it }, ::read)
        list.refresh()
        testScheduler.runCurrent()
        repeat(6) {
            list.loadMore()
            testScheduler.runCurrent()
        }
        asked.clear()
        source = source.map { it + 1 }

        list.refresh()
        testScheduler.runCurrent()

        assertEquals(List(700) { it + 1 }, list.state.value.items)
        assertEquals(listOf(Page(500, 0), Page(200, 500)), asked)
        assertTrue(list.state.value.hasMore)
    }

    @Test
    fun `a refresh from the top goes back to one page`() = runTest {
        val list = PagedList(backgroundScope, { it }, ::read)
        list.refresh()
        testScheduler.runCurrent()
        list.loadMore()
        testScheduler.runCurrent()

        list.refresh(fromTop = true)
        testScheduler.runCurrent()

        assertEquals(100, list.state.value.items.size)
    }

    @Test
    fun `a row that moved into the next page is not shown twice`() = runTest {
        val list = PagedList(backgroundScope, { it }, ::read)
        list.refresh()
        testScheduler.runCurrent()
        // A new first row pushes every row one place down, so row 99 is read again.
        source = listOf(-1) + source

        list.loadMore()
        testScheduler.runCurrent()

        val items = list.state.value.items
        assertEquals(items.distinct(), items)
        assertEquals(199, items.size)
    }

    @Test
    fun `a read that fails ends the loading and keeps the rows read before`() = runTest {
        var broken = true
        val list = PagedList(backgroundScope, { it }) { if (broken) error("disk full") else read(it) }

        list.refresh()
        testScheduler.runCurrent()
        assertEquals(PagedItems<Int>(isLoading = false, failed = true), list.state.value)

        broken = false
        list.refresh()
        testScheduler.runCurrent()
        assertEquals(100, list.state.value.items.size)
        assertFalse(list.state.value.failed)

        broken = true
        list.refresh()
        list.loadMore()
        testScheduler.runCurrent()
        assertEquals(100, list.state.value.items.size)
        assertTrue(list.state.value.failed)
    }

    @Test
    fun `a next page asked for during a refresh is added after it`() = runTest {
        val list = PagedList(backgroundScope, { it }) {
            delay(10)
            read(it)
        }
        list.refresh()
        testScheduler.advanceTimeBy(1_000)

        list.refresh()
        list.loadMore()
        testScheduler.advanceTimeBy(1_000)

        assertEquals(200, list.state.value.items.size)
    }

    @Test
    fun `an empty list is not loading and has no more`() = runTest {
        source = emptyList()
        val list = PagedList(backgroundScope, { it }, ::read)

        list.refresh()
        testScheduler.runCurrent()

        assertEquals(PagedItems<Int>(isLoading = false), list.state.value)
    }
}
