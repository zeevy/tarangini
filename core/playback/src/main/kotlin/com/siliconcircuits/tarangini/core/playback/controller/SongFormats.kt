/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.siliconcircuits.tarangini.core.domain.model.SongFormat

/** The format of the sound track the player has picked, or null while no sound track is picked. */
internal fun Tracks.playingFormat(): SongFormat? =
    groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }?.let { group ->
        (0 until group.length).firstOrNull(group::isTrackSelected)?.let { track ->
            group.getTrackFormat(track).toSongFormat()
        }
    }

/** What a person reads in the format line. A value the file did not give is [Format.NO_VALUE], and becomes null. */
// The average and peak bit rate of a format are API of Media3 that may still change.
@OptIn(UnstableApi::class)
internal fun Format.toSongFormat(): SongFormat = SongFormat(
    codec = CODEC_NAMES[sampleMimeType],
    // A file states the average rate, the peak rate or both. The average is the one people know. The part
    // below a whole kbit is cut off, as other players do.
    bitrateKbps = listOf(averageBitrate, bitrate, peakBitrate).firstOrNull { it > 0 }?.div(BITS_PER_KBIT),
    sampleRateHz = sampleRate.takeIf { it > 0 },
    channels = channelCount.takeIf { it > 0 },
)

// Only the formats the player can open. Any other one shows no name.
private val CODEC_NAMES = mapOf(
    MimeTypes.AUDIO_MPEG to "MP3",
    MimeTypes.AUDIO_AAC to "AAC",
    MimeTypes.AUDIO_ALAC to "ALAC",
    MimeTypes.AUDIO_FLAC to "FLAC",
    MimeTypes.AUDIO_OPUS to "Opus",
    MimeTypes.AUDIO_VORBIS to "Vorbis",
)

private const val BITS_PER_KBIT = 1000
