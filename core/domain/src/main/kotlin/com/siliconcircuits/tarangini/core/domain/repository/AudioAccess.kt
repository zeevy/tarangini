/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

/** Says whether the person lets this app read the audio files of the phone. */
fun interface AudioAccess {
    /** True while the permission is given. It can be taken away at any time, so ask each time. */
    fun isAllowed(): Boolean
}
