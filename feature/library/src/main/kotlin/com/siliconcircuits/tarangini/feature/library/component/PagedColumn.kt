/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.feature.library.R
import com.siliconcircuits.tarangini.feature.library.list.PagedItems

/**
 * A long list that asks for its next page a little before the person
 * reaches the end of the rows it has.
 *
 * @param key the id of a row. It must be the same for the same row after the list was read again
 * @param state the scroll place. A screen with tabs keeps one per tab, so a tab comes back where it was
 * @param header rows above the list, such as the albums on the screen of an artist
 */
@Composable
internal fun <T : Any> PagedColumn(
    list: PagedItems<T>,
    key: (T) -> Any,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    header: LazyListScope.() -> Unit = {},
    row: @Composable (T) -> Unit,
) {
    val nearEnd by remember(state) {
        derivedStateOf {
            isNearEnd(state.layoutInfo.totalItemsCount, state.layoutInfo.visibleItemsInfo.lastOrNull()?.index)
        }
    }
    LoadAhead(nearEnd, list, onLoadMore)
    LazyColumn(state = state, modifier = modifier.fillMaxSize()) {
        header()
        items(items = list.items, key = key) { row(it) }
    }
}

/**
 * A long list as a grid of two or more columns, such as the albums. It asks for its next page a
 * little before the person reaches the end, as [PagedColumn] does.
 */
@Composable
internal fun <T : Any> PagedGrid(
    list: PagedItems<T>,
    key: (T) -> Any,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    tile: @Composable (T) -> Unit,
) {
    val nearEnd by remember(state) {
        derivedStateOf {
            isNearEnd(state.layoutInfo.totalItemsCount, state.layoutInfo.visibleItemsInfo.lastOrNull()?.index)
        }
    }
    LoadAhead(nearEnd, list, onLoadMore)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(TILE_WIDTH),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = list.items, key = key) { tile(it) }
    }
}

@Composable
private fun LoadAhead(nearEnd: Boolean, list: PagedItems<*>, onLoadMore: () -> Unit) {
    val loadMore by rememberUpdatedState(onLoadMore)
    // The number of rows is a key, so a page that did not fill the screen asks for the next one.
    LaunchedEffect(nearEnd, list.items.size, list.hasMore) {
        if (nearEnd && list.hasMore) loadMore()
    }
}

// Before the first layout nothing is known, and that is not the end of the list.
private fun isNearEnd(total: Int, lastShown: Int?): Boolean = total > 0 && (lastShown ?: 0) >= total - LOAD_AHEAD

/**
 * The body of a list by what is known of it: a spinner until the first
 * read has ended, a message with a button when that read failed and there is
 * nothing to show, else the [content].
 */
@Composable
internal fun ListBody(isLoading: Boolean, failed: Boolean, onRetry: () -> Unit, content: @Composable () -> Unit) {
    when {
        isLoading -> Loading()

        failed -> MessageState(
            title = stringResource(R.string.library_error_title),
            text = stringResource(R.string.library_error_text),
            icon = painterResource(CoreUiR.drawable.ic_library),
            actionLabel = stringResource(R.string.library_error_retry),
            onAction = onRetry,
        )

        else -> content()
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.library_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = label })
    }
}

private const val LOAD_AHEAD = 20

// Two tiles side by side on a phone, more on a wide screen.
private val TILE_WIDTH = 120.dp
