/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the baseline profile: the start of the app and a scroll of the songs list. Run it with
 * `./gradlew :app:generateCoreReleaseBaselineProfile` while a device or an emulator is connected.
 * The profile is written to `app/src/coreRelease/generated/baselineProfiles/`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    /** Runs the journey several times and keeps the code it ran. */
    @get:Rule
    val rule = BaselineProfileRule()

    /** Starts the app and scrolls the songs list. */
    @Test
    fun startAndScrollTheSongs() = rule.collect(packageName = TARGET_PACKAGE) {
        allowAudio()
        pressHome()
        startActivityAndWait()
        scrollTheSongs()
    }
}
