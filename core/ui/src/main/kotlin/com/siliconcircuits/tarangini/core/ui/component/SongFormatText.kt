/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.domain.model.SongFormat
import com.siliconcircuits.tarangini.core.ui.R
import java.text.NumberFormat

/**
 * The format of a song in one line, such as "MP3 · 320 kbps · 44.1 kHz · Stereo". A part the
 * file did not tell is left out; a format with no part at all gives an empty text.
 */
@Composable
fun songFormatText(format: SongFormat): String = listOfNotNull(
    format.codec,
    format.bitrateKbps?.let { stringResource(R.string.format_kbps, it) },
    format.sampleRateHz?.let { stringResource(R.string.format_khz, kiloHertz(it)) },
    format.channels?.let { channelsText(it) },
).joinToString(stringResource(R.string.format_separator))

@Composable
private fun channelsText(channels: Int): String = when (channels) {
    1 -> stringResource(R.string.format_mono)

    2 -> stringResource(R.string.format_stereo)

    // Six channels are almost always 5.1. The layout itself is not read, so a rare 6.0 file is named 5.1 too.
    SURROUND_CHANNELS -> stringResource(R.string.format_surround)

    else -> pluralStringResource(R.plurals.format_channels, channels, channels)
}

// 44,100 Hz is "44.1", 48,000 Hz is "48" and 22,050 Hz is "22.05": as many decimals as the rate has.
private fun kiloHertz(hertz: Int): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = KHZ_DECIMALS
    isGroupingUsed = false
}.format(hertz / HZ_PER_KHZ)

private const val SURROUND_CHANNELS = 6
private const val KHZ_DECIMALS = 3
private const val HZ_PER_KHZ = 1000.0
