/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.siliconcircuits.tarangini.core.data.mediastore.scanQuietly
import com.siliconcircuits.tarangini.core.domain.library.LibraryScanner
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * The job that scans the library after the audio files changed while the
 * app was not open. WorkManager makes it, so it takes what it needs through
 * [Dependencies] and not through its constructor.
 */
internal class RescanWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    /** What the job needs from the app. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun libraryScanner(): LibraryScanner

        fun rescanSchedule(): RescanSchedule
    }

    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        // A scan that could not run, for example without the permission, is no reason to run the job again.
        dependencies.libraryScanner().scanQuietly()
        dependencies.rescanSchedule().waitForNextChange()
        return Result.success()
    }
}
