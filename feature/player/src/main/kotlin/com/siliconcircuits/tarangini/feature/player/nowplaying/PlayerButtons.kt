/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The row of square keys: shuffle, previous, play or pause, next, repeat. Play or pause is the
 * filled key. A live stream is one item with no end, so shuffle and repeat are left out for it.
 */
@Composable
internal fun PlayerButtons(state: NowPlayingUiState, actions: NowPlayingActions, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
        if (!state.isLive) ShuffleKey(state.shuffle, actions.onToggleShuffle)
        Key(
            icon = painterResource(CoreUiR.drawable.ic_skip_previous),
            name = stringResource(CoreUiR.string.action_previous),
            onClick = actions.onPrevious,
        )
        Key(
            icon = painterResource(if (state.showPause) CoreUiR.drawable.ic_pause else CoreUiR.drawable.ic_play),
            name = stringResource(if (state.showPause) CoreUiR.string.action_pause else CoreUiR.string.action_play),
            onClick = actions.onTogglePlay,
            // The main key of the screen is filled with the accent colour.
            container = MaterialTheme.colorScheme.secondary,
            tint = MaterialTheme.colorScheme.onSecondary,
        )
        Key(
            icon = painterResource(CoreUiR.drawable.ic_skip_next),
            name = stringResource(CoreUiR.string.action_next),
            onClick = actions.onNext,
        )
        if (!state.isLive) RepeatKey(state.repeat, actions.onCycleRepeat)
    }
}

@Composable
private fun RowScope.Key(
    icon: Painter,
    name: String,
    onClick: () -> Unit,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    KeyFace(
        icon,
        tint,
        container,
        Modifier.clickable(role = Role.Button, onClick = onClick).semantics {
            contentDescription = name
        },
    )
}

/** Shuffle is a key that stays on. A screen reader says whether it is on. */
@Composable
private fun RowScope.ShuffleKey(on: Boolean, onToggle: () -> Unit) {
    val name = stringResource(R.string.now_playing_shuffle)
    KeyFace(
        icon = painterResource(CoreUiR.drawable.ic_shuffle),
        tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        container = if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { contentDescription = name },
    )
}

/** One key for the three repeat modes. Its name for a screen reader is the mode that is on. */
@Composable
private fun RowScope.RepeatKey(mode: RepeatMode, onCycle: () -> Unit) {
    val on = mode != RepeatMode.OFF
    val icon = if (mode == RepeatMode.ONE) CoreUiR.drawable.ic_repeat_one else CoreUiR.drawable.ic_repeat
    Key(
        icon = painterResource(icon),
        name = stringResource(
            when (mode) {
                RepeatMode.OFF -> R.string.now_playing_repeat_off
                RepeatMode.ALL -> R.string.now_playing_repeat_all
                RepeatMode.ONE -> R.string.now_playing_repeat_one
            },
        ),
        onClick = onCycle,
        container = if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// The touch area is the whole key: as wide as a fifth of the row and 52 dp high.
@Composable
private fun RowScope.KeyFace(icon: Painter, tint: Color, container: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier.weight(1f).height(KeyHeight).clip(MaterialTheme.shapes.small).background(container)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = icon, contentDescription = null, tint = tint)
    }
}

private val KeyHeight = 52.dp
private val KeyGap = 6.dp
