/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * How the song that plays is stored, as the player read it from the file or the stream. A part
 * the file does not tell is null and is left out where the format is shown.
 *
 * @property codec the short name of the sound format, such as "MP3", "AAC" or "FLAC"
 * @property bitrateKbps the bit rate in kbit/s, the average one for a file with a varying rate
 * @property sampleRateHz the samples per second of each channel, such as 44,100
 * @property channels how many channels the sound has: 1 is mono, 2 is stereo
 */
data class SongFormat(
    val codec: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val channels: Int? = null,
)
