/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.premium

import com.siliconcircuits.tarangini.core.domain.premium.CrashReporter
import com.siliconcircuits.tarangini.core.domain.premium.ReportedProblem

/** A [CrashReporter] for tests. It keeps every report in [reports]. */
class FakeCrashReporter : CrashReporter {
    val reports = mutableListOf<Pair<ReportedProblem, String>>()

    override fun report(problem: ReportedProblem, kind: String) {
        reports += problem to kind
    }
}
