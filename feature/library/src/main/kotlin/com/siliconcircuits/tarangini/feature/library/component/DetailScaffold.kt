/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.ui.component.ListCard
import com.siliconcircuits.tarangini.core.ui.component.ScreenHeader

/**
 * The frame of a screen that opens from a list: a header with a back button, the [title] and the
 * [actions], an optional [hero] card such as the cover of an album, and the list on its own card
 * with the [count] and the [tools] at its top.
 */
@Composable
internal fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    count: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    tools: @Composable RowScope.() -> Unit = {},
    hero: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 10.dp),
    ) {
        ScreenHeader(title = title, onBack = onBack, actions = actions)
        hero?.invoke()
        ListCard(
            count = count,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp, bottom = 10.dp),
            tools = tools,
            content = content,
        )
    }
}
