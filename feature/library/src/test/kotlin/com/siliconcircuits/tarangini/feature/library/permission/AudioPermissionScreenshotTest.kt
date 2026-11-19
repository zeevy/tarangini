/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
    fun `not asked, light`() = capture("permission_not_asked_light", AudioPermissionState.NOT_ASKED)

    @Test
    fun `denied, light`() = capture("permission_denied_light", AudioPermissionState.DENIED)

    @Test
    fun `denied for good, light`() = capture("permission_blocked_light", AudioPermissionState.DENIED_FOR_GOOD)

    @Test
    fun `not asked, dark`() = capture("permission_not_asked_dark", AudioPermissionState.NOT_ASKED, darkTheme = true)

    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun `denied for good on a narrow screen with the largest font`() {
        RuntimeEnvironment.setFontScale(LARGEST_FONT_SCALE)
        capture("permission_blocked_narrow_largest_font", AudioPermissionState.DENIED_FOR_GOOD)
    }

    private fun capture(name: String, state: AudioPermissionState, darkTheme: Boolean = false) {
        val picture = File("src/test/screenshots/$name.png")
        // Without this, a test with no saved picture passes: there is nothing to compare with.
        assertTrue("No saved picture $picture. Run the tests with -PrecordScreenshots.", recording || picture.exists())
        captureRoboImage(picture.path) {
            TaranginiTheme(darkTheme = darkTheme, dynamicColor = false) {
                AudioPermissionScreen(state = state, onAllow = {}, onOpenSettings = {})
            }
        }
    }
}

private const val PHONE = "w390dp-h844dp-xhdpi"
private const val SMALL_PHONE = "w320dp-h640dp-xhdpi"
private const val LARGEST_FONT_SCALE = 2f
private val recording = System.getProperty("roborazzi.test.record").toBoolean()
