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
import java.util.Locale

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
).joinToString(FORMAT_SEPARATOR)

@Composable
private fun channelsText(channels: Int): String = when (channels) {
    1 -> stringResource(R.string.format_mono)
    2 -> stringResource(R.string.format_stereo)
    SURROUND_CHANNELS -> stringResource(R.string.format_surround)
    else -> pluralStringResource(R.plurals.format_channels, channels, channels)
}

// 44,100 Hz is "44.1", 48,000 Hz is "48": one decimal, and none when it is a whole number.
private fun kiloHertz(hertz: Int): String {
    val tenths = (hertz + HALF_TENTH_HZ) / TENTH_KHZ_HZ
    val whole = tenths / TENTHS
    val rest = tenths % TENTHS
    return if (rest == 0) whole.toString() else String.format(Locale.ROOT, "%d.%d", whole, rest)
}

private const val FORMAT_SEPARATOR = " · "
private const val SURROUND_CHANNELS = 6
private const val TENTH_KHZ_HZ = 100
private const val HALF_TENTH_HZ = 50
private const val TENTHS = 10
