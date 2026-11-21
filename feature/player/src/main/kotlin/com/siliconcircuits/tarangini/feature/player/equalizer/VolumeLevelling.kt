/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.domain.audio.VolumeLevelling
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The three ways of volume levelling, one row each. It works also while the equalizer is off, so
 * the rows are never turned off with it.
 */
@Composable
internal fun VolumeLevellingSection(levelling: VolumeLevelling, onPick: (VolumeLevelling) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            stringResource(R.string.equalizer_levelling),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringResource(R.string.equalizer_levelling_about), style = MaterialTheme.typography.bodyMedium)
        Column(modifier = Modifier.selectableGroup()) {
            VolumeLevelling.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(selected = mode == levelling, role = Role.RadioButton) { onPick(mode) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == levelling, onClick = null)
                    Text(
                        levellingName(mode),
                        modifier = Modifier.padding(start = Spacing.medium),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun levellingName(mode: VolumeLevelling): String = stringResource(
    when (mode) {
        VolumeLevelling.OFF -> R.string.equalizer_levelling_off
        VolumeLevelling.TRACK -> R.string.equalizer_levelling_track
        VolumeLevelling.ALBUM -> R.string.equalizer_levelling_album
    },
)
