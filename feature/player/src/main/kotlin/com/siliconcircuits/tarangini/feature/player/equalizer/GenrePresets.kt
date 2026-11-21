/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.equalizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerPreset
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import com.siliconcircuits.tarangini.core.domain.model.Genre
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The preset of each genre: a switch for all of them and one row per genre. A genre with no
 * preset of its own plays with the main setting.
 */
@Composable
internal fun GenrePresets(
    settings: EqualizerSettings,
    genres: List<Genre>,
    onSetGenresOn: (Boolean) -> Unit,
    onSetGenrePreset: (String, EqualizerPreset?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        SwitchRow(stringResource(R.string.equalizer_genres), settings.genresOn, settings.isOn, onSetGenresOn)
        if (genres.isEmpty()) {
            Text(stringResource(R.string.equalizer_genres_none), style = MaterialTheme.typography.bodyMedium)
        }
        val isEnabled = settings.isOn && settings.genresOn
        genres.forEach { genre ->
            GenreRow(genre, settings.genrePresets[genre.key], isEnabled) { onSetGenrePreset(genre.key, it) }
        }
    }
}

/** A genre with its preset. A tap opens the list of presets; the first line follows the main setting. */
@Composable
private fun GenreRow(genre: Genre, preset: EqualizerPreset?, isEnabled: Boolean, onPick: (EqualizerPreset?) -> Unit) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    val main = stringResource(R.string.equalizer_genre_main)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_HEIGHT)
                .clickable(enabled = isEnabled, role = Role.DropdownList) { isOpen = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(genre.name, modifier = Modifier.weight(1f))
            Text(preset?.let { presetName(it) } ?: main, style = MaterialTheme.typography.bodyMedium)
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            DropdownMenuItem(text = { Text(main) }, onClick = {
                isOpen = false
                onPick(null)
            })
            EqualizerPreset.entries.forEach { choice ->
                DropdownMenuItem(text = { Text(presetName(choice)) }, onClick = {
                    isOpen = false
                    onPick(choice)
                })
            }
        }
    }
}

/** A touch target of 48 dp. */
private val ROW_HEIGHT = 48.dp
