/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.work

import android.content.Context
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The rescan job in WorkManager. The job has no timer: the system starts it
 * only after the audio files changed, and not while the battery is low. A
 * phone on which nothing changes runs nothing.
 */
internal class WorkManagerRescanSchedule @Inject constructor(@ApplicationContext private val context: Context) :
    RescanSchedule {
    override fun waitForChange() = enqueue(ExistingWorkPolicy.KEEP)

    // "Keep" would do nothing here, because the job that asks still runs.
    override fun waitForNextChange() = enqueue(ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun enqueue(policy: ExistingWorkPolicy) {
        val constraints = Constraints.Builder()
            .addContentUriTrigger(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true)
            // Files are often copied many at a time. Wait until the copying is quiet, but not for ever.
            .setTriggerContentUpdateDelay(QUIET_SECONDS, TimeUnit.SECONDS)
            .setTriggerContentMaxDelay(LATEST_SECONDS, TimeUnit.SECONDS)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = OneTimeWorkRequestBuilder<RescanWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }

    internal companion object {
        /** The name of the one rescan job. It never changes, or an old job would stay beside the new one. */
        const val WORK_NAME = "library-rescan"
        private const val QUIET_SECONDS = 30L
        private const val LATEST_SECONDS = 300L
    }
}
