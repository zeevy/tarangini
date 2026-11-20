/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.permission

import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PHONE)
class AudioPermissionScreenshotTest {
    @Test
    fun `not asked`() = capture("permission_not_asked", AudioPermissionState.NOT_ASKED)

    @Test
    fun `denied`() = capture("permission_denied", AudioPermissionState.DENIED)

    @Test
    fun `denied for good`() = capture("permission_blocked", AudioPermissionState.DENIED_FOR_GOOD)

    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun `denied for good on a narrow screen with the largest font`() {
        RuntimeEnvironment.setFontScale(LARGEST_FONT_SCALE)
        capture("permission_blocked_narrow_largest_font", AudioPermissionState.DENIED_FOR_GOOD)
    }

    private fun capture(name: String, state: AudioPermissionState) {
        val picture = File("src/test/screenshots/$name.png")
        // Without this, a test with no saved picture passes: there is nothing to compare with.
        assertTrue("No saved picture $picture. Run the tests with -PrecordScreenshots.", recording || picture.exists())
        captureRoboImage(picture.path) {
            TaranginiTheme {
                AudioPermissionScreen(state = state, onAllow = {}, onOpenSettings = {})
            }
        }
    }
}

private const val PHONE = "w390dp-h844dp-xhdpi"
private const val SMALL_PHONE = "w320dp-h640dp-xhdpi"
private const val LARGEST_FONT_SCALE = 2f
private val recording = System.getProperty("roborazzi.test.record").toBoolean()
