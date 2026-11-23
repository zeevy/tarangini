/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerPreset
import com.siliconcircuits.tarangini.core.domain.audio.MAX_BAND_GAIN_DB
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.PanelColor
import com.siliconcircuits.tarangini.core.ui.theme.PanelEdge
import com.siliconcircuits.tarangini.feature.player.R
import kotlin.math.roundToInt

/**
 * The equalizer as a panel: a title row with the preset, Reset and the switch, one vertical fader
 * per band, and the presets in a row. The now playing screen and the equalizer screen both show
 * it, with the state of one [EqualizerViewModel]. With the equalizer off it is dimmed and only the
 * switch works.
 */
@Composable
internal fun EqualizerPanel(state: EqualizerUiState, actions: EqualizerActions, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.large
    val isOn = state.settings.isOn
    Column(
        modifier = modifier.background(PanelColor, shape).border(PanelEdge, shape)
            .padding(start = PanelEdgeSpace, end = PanelEdgeSpace, bottom = PanelEdgeSpace),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TitleRow(state, actions)
        Column(
            modifier = Modifier.alpha(if (isOn) 1f else OFF_ALPHA),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Faders(state, isOn, actions.onSetBand)
            Presets(state.settings.preset, isOn, actions.onPickPreset)
        }
    }
}

/** The panel on the now playing screen, with its own state. It shows nothing until the settings are read. */
@Composable
internal fun NowPlayingEqualizer(modifier: Modifier = Modifier, viewModel: EqualizerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let { EqualizerPanel(it, viewModel.actions(), modifier) }
}

@Composable
private fun TitleRow(state: EqualizerUiState, actions: EqualizerActions) {
    val preset = state.settings.preset?.let { presetName(it) } ?: stringResource(R.string.equalizer_custom)
    val bands = pluralStringResource(R.plurals.equalizer_band_count, state.bandsHz.size, state.bandsHz.size)
    Row(modifier = Modifier.fillMaxWidth().height(TitleHeight), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.equalizer_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = stringResource(R.string.equalizer_summary, preset, bands),
            modifier = Modifier.weight(1f).padding(start = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Reset is Flat: every band at 0 dB.
        TextButton(
            onClick = { actions.onPickPreset(EqualizerPreset.FLAT) },
            enabled = state.settings.isOn && state.settings.preset != EqualizerPreset.FLAT,
        ) { Text(stringResource(R.string.equalizer_reset)) }
        val switchName = stringResource(R.string.equalizer_on)
        Switch(
            checked = state.settings.isOn,
            onCheckedChange = actions.onSetOn,
            modifier = Modifier.semantics { contentDescription = switchName },
        )
    }
}

/**
 * One strip per band. The strips share the width; each is at least 48 dp wide for a finger, and
 * when that is wider than the panel the row scrolls sideways, starting with the lowest bands.
 */
@Composable
private fun Faders(state: EqualizerUiState, isOn: Boolean, onSetBand: (Int, Float) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val count = state.bandsHz.size
        val shared = (maxWidth - StripGap * (count - 1)) / count
        val strip = max(shared, SmallestStrip)
        Row(
            // Every strip as high as the highest, also when a label got smaller to fit.
            modifier = Modifier.horizontalScroll(rememberScrollState()).height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(StripGap),
        ) {
            state.bandsHz.forEachIndexed { index, hz ->
                Fader(hz, state.gains[index], isOn, strip) { onSetBand(index, it) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Fader(hz: Float, gain: Float, isOn: Boolean, width: Dp, onChange: (Float) -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val name = bandName(hz)
    val value = gainText(gain)
    Column(
        modifier = Modifier.width(width).fillMaxHeight()
            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A band at 0 dB is not changed, so its number steps back.
        FaderLabel(shortGain(gain), if (gain == 0f) muted else MaterialTheme.colorScheme.onSurface)
        val slider = remember { SliderState(steps = STEPS, trackRange = -MAX_BAND_GAIN_DB..MAX_BAND_GAIN_DB) }
        slider.value = gain
        VerticalSlider(
            state = slider,
            onValueChange = { onChange(it.roundToInt().toFloat()) },
            enabled = isOn,
            // The top is the most gain, as on a mixing desk.
            topToBottom = false,
            // A mark per dB would draw a dotted line down each fader.
            colors = SliderDefaults.colors(activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent),
            modifier = Modifier.height(FaderHeight).semantics {
                contentDescription = name
                stateDescription = value
            },
        )
        FaderLabel(shortBandName(hz), muted)
    }
}

/** A number on a fader. With a large font it gets smaller to stay on one line in the 48 dp strip. */
@Composable
private fun FaderLabel(text: String, color: Color) {
    BasicText(
        text = text,
        style = TextStyle(fontFamily = DigitsFont, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = color),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 11.sp),
    )
}

/**
 * The preset names. They wrap onto a second line on a narrow screen, so the picked one is always in
 * view. After a band was moved by hand none is picked.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Presets(picked: EqualizerPreset?, isOn: Boolean, onPick: (EqualizerPreset) -> Unit) {
    val colors = MaterialTheme.colorScheme
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        EqualizerPreset.entries.forEach { preset ->
            FilterChip(
                selected = preset == picked,
                onClick = { onPick(preset) },
                enabled = isOn,
                label = { Text(presetName(preset)) },
                // The picked preset is filled with the accent, as the main key of the card is.
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.secondary,
                    selectedLabelColor = colors.onSecondary,
                ),
            )
        }
    }
}

/** "60", "910", "3.6k", "14k": short enough to stand under a fader of 48 dp. */
internal fun shortBandName(hz: Float): String = if (hz < KILO) {
    hz.roundToInt().toString()
} else {
    val kilo = hz / KILO
    (if (kilo % 1f == 0f) kilo.roundToInt().toString() else "%.1f".format(kilo)) + "k"
}

/** "+3", "0", "-2". */
private fun shortGain(gain: Float): String {
    val whole = gain.roundToInt()
    return if (whole > 0) "+$whole" else whole.toString()
}

private val PanelEdgeSpace = 10.dp
private val TitleHeight = 48.dp
private val StripGap = 6.dp
private val SmallestStrip = 48.dp
private val FaderHeight = 132.dp
private const val OFF_ALPHA = 0.5f

// From -12 to +12 dB in steps of 1 dB: 23 stops between the ends.
private const val STEPS = 23
