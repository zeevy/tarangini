/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.equalizer

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerPreset
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import com.siliconcircuits.tarangini.core.domain.audio.FIVE_BANDS_HZ
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.checkAccessibility
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class EqualizerContentTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** The manifest of a library has no activity, so the plain one is added before Compose starts it. */
    private val registerActivity = object : TestWatcher() {
        override fun starting(description: Description) {
            shadowOf(app.packageManager)
                .addActivityIfNotPresent(ComponentName(app.packageName, ComponentActivity::class.java.name))
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private val done = mutableListOf<String>()
    private val actions = EqualizerActions(
        onSetOn = { done += "on $it" },
        onPickPreset = { done += "preset $it" },
        onSetBand = { index, gain -> done += "band $index $gain" },
        onSetBassBoost = { done += "bass $it" },
    )
    private val carnatic = EqualizerUiState(
        EqualizerSettings(isOn = true, preset = EqualizerPreset.CARNATIC, bassBoost = 40),
        FIVE_BANDS_HZ,
    )

    private fun show(state: EqualizerUiState?) = composeRule.setContent {
        TaranginiTheme { EqualizerContent(state, actions, { done += "back" }) }
    }

    @Test
    fun `the screen shows the preset, each band with its gain and the bass boost`() {
        show(carnatic)

        composeRule.onNodeWithText("Equalizer on").assertIsOn()
        composeRule.onNodeWithText("Carnatic").assertIsSelected()
        composeRule.onNodeWithText("60 Hz").assertExists()
        composeRule.onNodeWithText("910 Hz").assertExists()
        composeRule.onNodeWithText("3.6 kHz").assertExists()
        composeRule.onNodeWithText("14 kHz").assertExists()
        composeRule.onNodeWithText("+3 dB").assertExists()
        composeRule.onNodeWithText("40 %").assertExists()
    }

    @Test
    fun `the switch row, a preset and the back arrow do what they say`() {
        show(carnatic)

        composeRule.onNode(isToggleable()).performClick()
        composeRule.onNodeWithText("Bass").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("on false", "preset BASS", "back"), done)
    }

    @Test
    fun `a band moves in half dB steps, and the bass in whole percent`() {
        show(carnatic)

        composeRule.onNodeWithContentDescription("230 Hz").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(4.3f) }
        composeRule.onNodeWithContentDescription("Bass boost").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(57.6f) }

        assertEquals(listOf("band 1 4.5", "bass 58"), done)
    }

    @Test
    fun `with the equalizer off the presets and sliders cannot be used`() {
        show(EqualizerUiState(EqualizerSettings(), FIVE_BANDS_HZ))

        composeRule.onNode(isToggleable()).assertIsOff()
        composeRule.onNodeWithText("Bass").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("60 Hz").assertIsNotEnabled()
    }

    @Test
    fun `the screen passes the accessibility checks`() {
        show(carnatic)

        composeRule.checkAccessibility()
    }

    @Test
    fun `a preset on the screen`() = shot("equalizer") { show(carnatic) }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun `a preset with the largest font`() {
        RuntimeEnvironment.setFontScale(LARGEST_FONT)
        shot("equalizer_largest_font") { show(carnatic) }
    }

    @Test
    @Config(qualifiers = "w640dp-h320dp-xhdpi")
    fun `a preset on a wide screen`() = shot("equalizer_wide") { show(carnatic) }

    @Test
    fun `the equalizer off`() = shot("equalizer_off") { show(EqualizerUiState(EqualizerSettings(), FIVE_BANDS_HZ)) }

    private fun shot(name: String, content: () -> Unit) {
        val picture = File("src/test/screenshots/$name.png")
        // Without this, a test with no saved picture passes: there is nothing to compare with.
        assertTrue("No saved picture $picture. Run the tests with -PrecordScreenshots.", recording || picture.exists())
        content()
        composeRule.onRoot().captureRoboImage(picture.path)
    }

    private companion object {
        const val LARGEST_FONT = 2f
    }
}

private val recording = System.getProperty("roborazzi.test.record").toBoolean()
