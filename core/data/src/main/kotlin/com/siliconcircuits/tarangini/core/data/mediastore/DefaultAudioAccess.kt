/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import com.siliconcircuits.tarangini.core.domain.repository.AudioAccess
import javax.inject.Inject

/** The audio permission, as the one class that talks to MediaStore sees it. */
internal class DefaultAudioAccess @Inject constructor(private val source: AudioFileSource) : AudioAccess {
    override fun isAllowed(): Boolean = source.hasPermission()
}
