/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import java.io.IOException

/** The player was asked to open an address that is neither an audio file of the phone nor a stream. */
internal class RefusedAddressException :
    IOException("Only audio files of the phone and http or https streams are opened")
