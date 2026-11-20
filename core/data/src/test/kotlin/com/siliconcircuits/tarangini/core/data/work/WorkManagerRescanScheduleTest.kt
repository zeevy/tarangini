/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.work

import android.app.Application
import android.provider.MediaStore
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WorkManagerRescanScheduleTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val schedule = WorkManagerRescanSchedule(app)

    @Before
    fun startWorkManager() = WorkManagerTestInitHelper.initializeTestWorkManager(app)

    private fun jobs(): List<WorkInfo> =
        WorkManager.getInstance(app).getWorkInfosForUniqueWork(WorkManagerRescanSchedule.WORK_NAME).get()

    @Test
    fun `the job waits for a change of the audio files and for a battery that is not low`() {
        schedule.waitForChange()

        val constraints = jobs().single().constraints
        assertEquals(
            listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            constraints.contentUriTriggers.map { it.uri },
        )
        assertTrue(constraints.requiresBatteryNotLow())
        assertEquals(WorkInfo.State.ENQUEUED, jobs().single().state)
    }

    @Test
    fun `asking again at the next app start keeps the one job that waits`() {
        schedule.waitForChange()
        val first = jobs().single().id

        schedule.waitForChange()

        assertEquals(listOf(first), jobs().map { it.id })
    }
}
