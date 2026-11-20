/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import kotlinx.coroutines.flow.Flow

/**
 * Where the scanner gets the list of audio files from. The app has one, the
 * media index of the phone. Tests use a list they make themselves.
 */
internal interface AudioFileSource {
    /** False when the app may not read the audio files. The scanner asks this first. */
    fun hasPermission(): Boolean

    /**
     * Every audio file the phone knows, or null when the phone gives no
     * answer. This reads from the phone and can take long: call it off the
     * main thread.
     */
    fun files(): List<AudioFile>?

    /**
     * Gives a value each time the phone says that its audio files changed.
     * It says only that something changed, not what, and it can say so many
     * times for one copied file.
     */
    fun changes(): Flow<Unit>
}
