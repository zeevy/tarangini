/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.work

import android.app.Application
import com.siliconcircuits.tarangini.core.domain.library.LibraryScanner
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryScanner
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager

/**
 * The application of a test that runs the rescan job. It hands the job what
 * the real app hands it through Hilt, and counts what the job asks for.
 */
internal class TestRescanApp :
    Application(),
    GeneratedComponentManager<Any> {
    val scanner = FakeLibraryScanner()
    var nextJobsAsked = 0
        private set

    private val schedule = object : RescanSchedule {
        override fun waitForChange() = error("Only the app start asks for this")

        override fun waitForNextChange() {
            nextJobsAsked++
        }
    }

    override fun generatedComponent(): Any = object : RescanWorker.Dependencies, GeneratedComponent {
        override fun libraryScanner(): LibraryScanner = scanner

        override fun rescanSchedule(): RescanSchedule = schedule
    }
}
