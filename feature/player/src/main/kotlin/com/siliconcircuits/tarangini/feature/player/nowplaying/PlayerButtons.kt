/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The row of buttons: shuffle, previous, play or pause, next, repeat. A live stream is one item
 * with no end, so shuffle and repeat are left out for it.
 */
@Composable
internal fun PlayerButtons(state: NowPlayingUiState, actions: NowPlayingActions, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!state.isLive) ShuffleButton(state.shuffle, actions.onToggleShuffle)
        IconButton(onClick = actions.onPrevious) {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_skip_previous),
                contentDescription = stringResource(CoreUiR.string.action_previous),
            )
        }
        FilledIconButton(
            onClick = actions.onTogglePlay,
            modifier = Modifier.size(Spacing.extraLarge + Spacing.medium),
            // The main button of a screen is white with a dark icon.
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Icon(
                painter = painterResource(if (state.showPause) CoreUiR.drawable.ic_pause else CoreUiR.drawable.ic_play),
                contentDescription = stringResource(
                    if (state.showPause) CoreUiR.string.action_pause else CoreUiR.string.action_play,
                ),
            )
        }
        IconButton(onClick = actions.onNext) {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_skip_next),
                contentDescription = stringResource(CoreUiR.string.action_next),
            )
        }
        if (!state.isLive) RepeatButton(state.repeat, actions.onCycleRepeat)
    }
}

@Composable
private fun ShuffleButton(on: Boolean, onToggle: () -> Unit) {
    IconToggleButton(checked = on, onCheckedChange = { onToggle() }) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_shuffle),
            contentDescription = stringResource(R.string.now_playing_shuffle),
        )
    }
}

/** One button for the three repeat modes. Its name for a screen reader is the mode that is on. */
@Composable
private fun RepeatButton(mode: RepeatMode, onCycle: () -> Unit) {
    IconButton(onClick = onCycle) {
        Icon(
            painter = painterResource(
                if (mode == RepeatMode.ONE) CoreUiR.drawable.ic_repeat_one else CoreUiR.drawable.ic_repeat,
            ),
            contentDescription = stringResource(
                when (mode) {
                    RepeatMode.OFF -> R.string.now_playing_repeat_off
                    RepeatMode.ALL -> R.string.now_playing_repeat_all
                    RepeatMode.ONE -> R.string.now_playing_repeat_one
                },
            ),
            tint = if (mode == RepeatMode.OFF) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}
