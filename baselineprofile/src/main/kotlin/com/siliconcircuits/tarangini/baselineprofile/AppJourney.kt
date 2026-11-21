/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

/** Application id of the app under test. */
internal const val TARGET_PACKAGE = "com.siliconcircuits.tarangini"

private const val WAIT_MS = 5_000L

/**
 * Opens the Library tab and scrolls the songs list down and back up. This is the path a person
 * takes most after the start, so its code goes into the profile and its frames are measured.
 * The device needs some music and the audio permission given; with no list it only opens the tab.
 */
internal fun MacrobenchmarkScope.scrollTheSongs() {
    device.wait(Until.findObject(By.text("Library")), WAIT_MS)?.click()
    val list = device.wait(Until.findObject(By.scrollable(true)), WAIT_MS) ?: return
    // Keep clear of the gesture bar, or a fling opens the recent apps.
    list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_PARTS)
    repeat(FLINGS) { list.fling(Direction.DOWN) }
    repeat(FLINGS) { list.fling(Direction.UP) }
    device.waitForIdle()
}

private const val FLINGS = 3
private const val GESTURE_MARGIN_PARTS = 5
