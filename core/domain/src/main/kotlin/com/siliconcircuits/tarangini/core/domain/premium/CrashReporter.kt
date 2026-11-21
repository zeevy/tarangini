/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.premium

/**
 * Takes note of an error the app caught and went on from, so the maintainers can learn of it.
 *
 * The build from the public code alone has a version that does nothing. The Play build sends
 * the report to a crash report service, together with the crashes that service finds by itself.
 *
 * What may be passed: only a [ReportedProblem] and a fixed name from the code, such as the name
 * of an error code. Nothing of the person or the phone goes in: no name of a file, folder, song,
 * artist, playlist or station, no address of a file or a stream, no search text, and no message
 * of an exception, because such a message often holds a path or an address. For this reason the
 * interface takes no exception at all.
 */
interface CrashReporter {
    /**
     * Notes that [problem] happened.
     *
     * @param kind a fixed name from the code that tells one cause from another, such as
     * `ERROR_CODE_DECODING_FAILED`. Never a text that comes from a file, a stream or the person
     */
    fun report(problem: ReportedProblem, kind: String)
}

/** The caught errors that are worth knowing about. */
enum class ReportedProblem {
    /** The phone gave no list of its audio files, so the library could not be brought in step. */
    SCAN_FAILED,

    /** The player gave up on an item. A file that is simply gone is not reported. */
    PLAYBACK_FAILED,
}
