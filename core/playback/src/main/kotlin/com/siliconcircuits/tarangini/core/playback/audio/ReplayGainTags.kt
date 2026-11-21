/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import com.siliconcircuits.tarangini.core.domain.audio.ReplayGain

/**
 * The ReplayGain tags in the [metadata] of a file, or null when it has none. An MP3 keeps them in
 * ID3 `TXXX` frames, an AAC or M4A file in iTunes `----` atoms; both are named
 * `REPLAYGAIN_TRACK_GAIN`, `REPLAYGAIN_TRACK_PEAK`, `REPLAYGAIN_ALBUM_GAIN` and
 * `REPLAYGAIN_ALBUM_PEAK`, in any case.
 */
// The frame classes of the extractors are API that may still change; they are only read here.
@OptIn(UnstableApi::class)
internal fun replayGainOf(metadata: Metadata?): ReplayGain? {
    if (metadata == null) return null
    val tags = HashMap<String, String>()
    for (index in 0 until metadata.length()) {
        val (name, value) = nameAndValue(metadata.get(index)) ?: continue
        // The first tag of a name counts, as in other players.
        if (name in NAMES) tags.putIfAbsent(name, value)
    }
    val gain = ReplayGain(
        trackGainDb = ReplayGain.gainOf(tags[TRACK_GAIN]),
        trackPeak = ReplayGain.peakOf(tags[TRACK_PEAK]),
        albumGainDb = ReplayGain.gainOf(tags[ALBUM_GAIN]),
        albumPeak = ReplayGain.peakOf(tags[ALBUM_PEAK]),
    )
    return gain.takeUnless { it.isEmpty }
}

/** The upper case name and the value of a text tag; null for any other entry. */
@OptIn(UnstableApi::class)
private fun nameAndValue(entry: Metadata.Entry): Pair<String, String>? {
    val (name, value) = when (entry) {
        is TextInformationFrame -> if (entry.id == "TXXX") entry.description to entry.value else null
        is InternalFrame -> entry.description to entry.text
        else -> null
    } ?: return null
    return name?.uppercase()?.let { it to value }
}

private const val TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
private const val TRACK_PEAK = "REPLAYGAIN_TRACK_PEAK"
private const val ALBUM_GAIN = "REPLAYGAIN_ALBUM_GAIN"
private const val ALBUM_PEAK = "REPLAYGAIN_ALBUM_PEAK"
private val NAMES = setOf(TRACK_GAIN, TRACK_PEAK, ALBUM_GAIN, ALBUM_PEAK)
