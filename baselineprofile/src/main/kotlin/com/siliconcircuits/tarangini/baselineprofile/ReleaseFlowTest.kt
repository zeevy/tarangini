/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.baselineprofile

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The main flow on the shrunk release app, driven from outside as a person would: the scan finds
 * new songs, a tap plays one, and a favourite shows in the Favourites tab.
 *
 * This test shares no code with the app, so R8 shrinks the app exactly as for a release. A class
 * that R8 removed or renamed by mistake (Hilt, Room, Media3, the navigation routes) makes one of
 * these steps fail. The in-app end to end test runs on the debug build, which R8 does not touch.
 *
 * Run it on an emulator or a test phone only: it leaves a favourite in the app.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)

    @get:Rule
    val files = ReleaseAudioFiles(instrumentation.context)

    @Before
    fun startTheApp() {
        // The profile and the benchmarks run every test of this module; this one runs only when asked.
        assumeTrue(
            "Pass the argument releaseFlow=true to run it",
            InstrumentationRegistry.getArguments().getString(RUN_ARGUMENT) == "true",
        )
        listOf("android.permission.READ_MEDIA_AUDIO", "android.permission.READ_EXTERNAL_STORAGE").forEach { name ->
            device.executeShellCommand("pm grant $TARGET_PACKAGE $name")
        }
        device.executeShellCommand("am force-stop $TARGET_PACKAGE")
        val context = instrumentation.context
        val start = checkNotNull(context.packageManager.getLaunchIntentForPackage(TARGET_PACKAGE)) {
            "The app $TARGET_PACKAGE is not installed"
        }
        context.startActivity(start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        need(By.pkg(TARGET_PACKAGE).depth(0), "the app to open")
    }

    @Test
    fun aNewSongIsFoundPlaysAndBecomesAFavourite() {
        val first = files.titles.first()

        // The scan: the new songs show in the songs list of the Library tab.
        need(By.text("Library"), "the Library tab").click()
        need(By.text(first), "the scan to list $first", scroll = true).click()

        // Play: the mini player opens. The whole list is the queue and each test song is about a
        // second long, so the song is paused and the one the mini player shows is used from here on.
        need(By.desc("Next"), "the mini player")
        device.executeShellCommand("input keyevent KEYCODE_MEDIA_PAUSE")
        val song = checkNotNull(miniPlayerSong()) { "The mini player shows none of ${files.titles}" }

        // Favourite: the row gets the heart, and the Favourites tab lists the song. Only a list row
        // has the menu button, so this does not find the title in the mini player.
        need(By.desc("More options for $song"), "the menu of $song", scroll = true).click()
        need(By.text("Add to favourites"), "the favourite entry").click()
        need(By.desc("Favourite"), "the heart on the row of $song")
        need(By.text("Favourites"), "the Favourites tab").click()
        need(By.desc("More options for $song"), "$song in Favourites")
    }

    /** The test song the mini player shows: a title of the test songs below every list row of it. */
    private fun miniPlayerSong(): String? {
        val end = System.currentTimeMillis() + WAIT_MS
        while (System.currentTimeMillis() < end) {
            val lowest = files.titles.flatMap { title -> device.findObjects(By.text(title)) }
                .maxByOrNull { it.visibleBounds.top }
            // The mini player sits under the list, so its title is the lowest one on the screen.
            val title = lowest?.text
            if (title != null && device.findObjects(By.text(title)).size >= 2) return title
            Thread.sleep(POLL_MS)
        }
        return null
    }

    /** Waits for an item on the screen, and with [scroll] scrolls the tallest list down to find it. */
    private fun need(selector: BySelector, what: String, scroll: Boolean = false): UiObject2 {
        val end = System.currentTimeMillis() + if (scroll) SCAN_WAIT_MS else 0
        var found = device.wait(Until.findObject(selector), WAIT_MS)
        while (found == null && System.currentTimeMillis() < end) {
            found = scrollTo(selector)
        }
        return checkNotNull(found) { "Timed out waiting for $what" }
    }

    /** One pass down the tallest list, then back to the top and a short wait for the scan to add rows. */
    private fun scrollTo(selector: BySelector): UiObject2? {
        val list = device.findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() }
        // Keep clear of the gesture bar, or a swipe opens the recent apps.
        list?.setGestureMargin(device.displayWidth / GESTURE_MARGIN_PARTS)
        return list?.scrollUntil(Direction.DOWN, Until.findObject(selector)) ?: run {
            list?.scroll(Direction.UP, FULL_SCROLL)
            device.wait(Until.findObject(selector), POLL_MS)
        }
    }

    private companion object {
        const val WAIT_MS = 10_000L
        const val SCAN_WAIT_MS = 60_000L
        const val POLL_MS = 500L
        const val GESTURE_MARGIN_PARTS = 5
        const val FULL_SCROLL = 50f
        const val RUN_ARGUMENT = "releaseFlow"
    }
}
