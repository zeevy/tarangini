/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures the cold start and the scroll of the songs list, with and without the baseline profile.
 * Run it with `./gradlew :baselineprofile:connectedCoreBenchmarkReleaseAndroidTest` on the test
 * phone; numbers from an emulator say little. The target is a cold start under 1.5 seconds.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    /** Starts the app from outside and reads the system's timings. */
    @get:Rule
    val rule = MacrobenchmarkRule()

    /** Cold start with no ahead of time compiled code. */
    @Test
    fun coldStartWithoutProfile() = coldStart(CompilationMode.None())

    /** Cold start with the code of the baseline profile compiled. */
    @Test
    fun coldStartWithProfile() = coldStart(CompilationMode.Partial(BaselineProfileMode.Require))

    /** Frames of a scroll of the songs list with the baseline profile. */
    @Test
    fun scrollWithProfile() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        startupMode = StartupMode.WARM,
        iterations = ITERATIONS,
        setupBlock = { allowAudio() },
    ) {
        startActivityAndWait()
        scrollTheSongs()
    }

    private fun coldStart(mode: CompilationMode) = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
        iterations = ITERATIONS,
    ) {
        pressHome()
        startActivityAndWait()
    }

    private companion object {
        const val ITERATIONS = 10
    }
}
