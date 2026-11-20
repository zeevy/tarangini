/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.work

import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.siliconcircuits.tarangini.core.domain.library.ScanFailure
import com.siliconcircuits.tarangini.core.domain.library.ScanOutcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestRescanApp::class)
class RescanWorkerTest {
    private val app = RuntimeEnvironment.getApplication() as TestRescanApp

    @Test
    fun `the job scans once and asks for the next job, also when the scan could not run`() = runTest {
        app.scanner.nextOutcome = ScanOutcome.Failure(ScanFailure.NO_PERMISSION)

        val result = TestListenableWorkerBuilder<RescanWorker>(app).build().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, app.scanner.scanCount)
        assertEquals(1, app.nextJobsAsked)
    }
}
