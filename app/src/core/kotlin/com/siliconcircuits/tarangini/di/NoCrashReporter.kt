/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.di

import com.siliconcircuits.tarangini.core.domain.premium.CrashReporter
import com.siliconcircuits.tarangini.core.domain.premium.ReportedProblem
import javax.inject.Inject

/** The build from the public code sends no reports: it has no crash report service. */
internal class NoCrashReporter @Inject constructor() : CrashReporter {
    override fun report(problem: ReportedProblem, kind: String) = Unit
}
