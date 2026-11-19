/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.testing.NoPlaybackServiceRule
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PHONE)
class TaranginiShellScreenshotTest {
    @get:Rule
    val noPlaybackService = NoPlaybackServiceRule()

    @Test
    fun `brand theme, light`() = capture("shell_brand_light", darkTheme = false, dynamicColor = false)

    @Test
    fun `brand theme, dark`() = capture("shell_brand_dark", darkTheme = true, dynamicColor = false)

    @Test
    fun `wallpaper colours, light`() = capture("shell_dynamic_light", darkTheme = false, dynamicColor = true)

    @Test
    fun `wallpaper colours, dark`() = capture("shell_dynamic_dark", darkTheme = true, dynamicColor = true)

    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun `narrow screen with the largest font`() {
        RuntimeEnvironment.setFontScale(LARGEST_FONT_SCALE)
        capture("shell_narrow_largest_font", darkTheme = false, dynamicColor = false)
    }

    private fun capture(name: String, darkTheme: Boolean, dynamicColor: Boolean) {
        val picture = File("src/test/screenshots/$name.png")
        // Without this, a test with no saved picture passes: there is nothing to compare with.
        assertTrue("No saved picture $picture. Run the tests with -PrecordScreenshots.", recording || picture.exists())
        captureRoboImage(picture.path) {
            TaranginiTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) { TaranginiShell() }
        }
    }
}

private const val PHONE = "w390dp-h844dp-xhdpi"
private const val SMALL_PHONE = "w320dp-h640dp-xhdpi"
private const val LARGEST_FONT_SCALE = 2f
private val recording = System.getProperty("roborazzi.test.record").toBoolean()
