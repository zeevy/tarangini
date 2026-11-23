/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/** Application id of the app under test. */
internal const val TARGET_PACKAGE = "com.siliconcircuits.tarangini"

private const val WAIT_MS = 5_000L

/**
 * Gives the app the audio permission, so the songs list shows in place of the reason screen. The
 * name of the permission depends on the Android version; the one the device does not know fails
 * quietly.
 */
internal fun MacrobenchmarkScope.allowAudio() {
    listOf("android.permission.READ_MEDIA_AUDIO", "android.permission.READ_EXTERNAL_STORAGE").forEach { name ->
        device.executeShellCommand("pm grant $packageName $name")
    }
}

/**
 * Opens the Library tab and scrolls the songs list down and back up. This is the path a person
 * takes most after the start, so its code goes into the profile and its frames are measured.
 * It fails when the songs never show, so a run with no permission or no music does not pass with
 * a profile that has no list in it.
 */
internal fun MacrobenchmarkScope.scrollTheSongs() {
    device.wait(Until.findObject(By.text("Library")), WAIT_MS)?.click()
    // The tab row above the list scrolls too. The list is the tallest one, and it waits for the
    // scan to put enough songs in it.
    val end = System.currentTimeMillis() + SCAN_WAIT_MS
    while ((songsList()?.childCount ?: 0) < LEAST_ROWS && System.currentTimeMillis() < end) {
        Thread.sleep(POLL_MS)
    }
    check((songsList()?.childCount ?: 0) >= LEAST_ROWS) {
        "The songs list did not show $LEAST_ROWS songs within $SCAN_WAIT_MS ms"
    }
    val list = checkNotNull(songsList())
    // Keep clear of the gesture bar, or a fling opens the recent apps.
    list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_PARTS)
    repeat(FLINGS) { list.fling(Direction.DOWN) }
    repeat(FLINGS) { list.fling(Direction.UP) }
    device.waitForIdle()
}

private fun MacrobenchmarkScope.songsList(): UiObject2? =
    device.findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() }

private const val SCAN_WAIT_MS = 30_000L
private const val LEAST_ROWS = 5
private const val POLL_MS = 250L
private const val FLINGS = 3
private const val GESTURE_MARGIN_PARTS = 5
