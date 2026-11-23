/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerPreset
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import com.siliconcircuits.tarangini.core.ui.component.BackButton
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.core.ui.theme.clearTopBarColors
import com.siliconcircuits.tarangini.feature.player.R
import kotlin.math.roundToInt

/** The equalizer screen: the panel of faders and presets, the bass boost, volume levelling and a preset per genre. */
@Composable
internal fun EqualizerScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EqualizerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = viewModel.actions()
    EqualizerContent(state, actions, onBackClick, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EqualizerContent(
    state: EqualizerUiState?,
    actions: EqualizerActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = clearTopBarColors(),
                title = { Text(stringResource(R.string.equalizer_title)) },
                navigationIcon = { BackButton(onClick = onBackClick) },
            )
        },
    ) { padding ->
        if (state != null) {
            Column(
                modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                EqualizerPanel(state, actions)
                val bass = state.settings.bassBoost
                LabelledSlider(
                    label = stringResource(R.string.equalizer_bass),
                    value = stringResource(R.string.equalizer_bass_value, bass),
                    position = bass.toFloat(),
                    range = 0f..EqualizerSettings.MAX_BASS_PERCENT.toFloat(),
                    isEnabled = state.settings.isOn,
                ) { actions.onSetBassBoost(it.roundToInt()) }
                VolumeLevellingSection(state.settings.levelling, actions.onSetLevelling)
                GenrePresets(state.settings, state.genres, actions.onSetGenresOn, actions.onSetGenrePreset)
            }
        }
    }
}

/** A switch with its text in front; the whole row is the touch target. */
@Composable
internal fun SwitchRow(text: String, isOn: Boolean, isEnabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(
            value = isOn,
            enabled = isEnabled,
            role = Role.Switch,
            onValueChange = onChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(checked = isOn, onCheckedChange = null, enabled = isEnabled)
    }
}

/** A name over a slider, with its value at the end of the line. At the largest font the two lines still fit. */
@Composable
private fun LabelledSlider(
    label: String,
    value: String,
    position: Float,
    range: ClosedFloatingPointRange<Float>,
    isEnabled: Boolean,
    onChange: (Float) -> Unit,
) {
    Column {
        Row {
            Text(label, modifier = Modifier.weight(1f))
            Text(value)
        }
        val slider = remember(range) { SliderState(trackRange = range) }
        slider.value = position
        Slider(
            state = slider,
            onValueChange = onChange,
            enabled = isEnabled,
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = value
            },
        )
    }
}

@Composable
internal fun presetName(preset: EqualizerPreset): String = stringResource(
    when (preset) {
        EqualizerPreset.FLAT -> R.string.equalizer_preset_flat
        EqualizerPreset.BASS -> R.string.equalizer_preset_bass
        EqualizerPreset.VOCAL -> R.string.equalizer_preset_vocal
        EqualizerPreset.CLASSICAL -> R.string.equalizer_preset_classical
        EqualizerPreset.CARNATIC -> R.string.equalizer_preset_carnatic
        EqualizerPreset.BOLLYWOOD -> R.string.equalizer_preset_bollywood
    },
)

/** 60 Hz, 910 Hz, 3.6 kHz, 14 kHz. */
@Composable
internal fun bandName(hz: Float): String = if (hz < KILO) {
    stringResource(R.string.equalizer_band_hz, hz.roundToInt())
} else {
    val kilo = hz / KILO
    val text = if (kilo % 1f == 0f) kilo.roundToInt().toString() else "%.1f".format(kilo)
    stringResource(R.string.equalizer_band_khz, text)
}

/** +3 dB, 0 dB, -2.5 dB. */
@Composable
internal fun gainText(gain: Float): String {
    val number = if (gain % 1f == 0f) gain.roundToInt().toString() else "%.1f".format(gain)
    return stringResource(R.string.equalizer_gain, if (gain > 0) "+$number" else number)
}

internal const val KILO = 1_000f
