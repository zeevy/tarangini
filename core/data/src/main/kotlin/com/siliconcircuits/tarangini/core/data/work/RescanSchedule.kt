/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.work

/**
 * Asks the system to scan the library after the audio files changed, also
 * while the app is not open. The system runs such a job once, so the job
 * asks for the next one when it is done.
 */
internal interface RescanSchedule {
    /** Makes sure that one job waits for a change. Called when the app starts; a job that waits already is kept. */
    fun waitForChange()

    /** Puts the next job behind the one that runs now. Only the job itself calls this. */
    fun waitForNextChange()
}
