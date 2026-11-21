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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
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
import com.siliconcircuits.tarangini.core.domain.audio.VolumeLevelling
import com.siliconcircuits.tarangini.core.domain.model.Genre
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
        onSetGenresOn = { done += "genres $it" },
        onSetGenrePreset = { key, preset -> done += "genre $key $preset" },
        onSetLevelling = { done += "levelling $it" },
    )
    private val carnatic = EqualizerUiState(
        EqualizerSettings(isOn = true, preset = EqualizerPreset.CARNATIC, bassBoost = 40),
        FIVE_BANDS_HZ,
    )
    private val withGenres = carnatic.copy(
        settings = carnatic.settings.copy(genresOn = true, genrePresets = mapOf("rock" to EqualizerPreset.BASS)),
        genres = listOf(Genre(1, "Folk", 4, key = "folk"), Genre(2, "Rock", 9, key = "rock")),
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

        composeRule.onNodeWithText("Equalizer on").performClick()
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

        composeRule.onNodeWithText("Equalizer on").assertIsOff()
        composeRule.onNodeWithText("Bass").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("60 Hz").assertIsNotEnabled()
    }

    @Test
    fun `each genre shows its preset, and a pick from its list is passed on`() {
        show(withGenres)

        composeRule.onNodeWithText("Preset per genre").performScrollTo().assertIsOn()
        composeRule.onNodeWithText("Same as the main setting").performScrollTo()
        composeRule.onNodeWithText("Folk").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Vocal").onLast().performClick()
        composeRule.onNodeWithText("Rock").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Same as the main setting").onLast().performClick()

        assertEquals(listOf("genre folk VOCAL", "genre rock null"), done)
    }

    @Test
    fun `with the genre switch off the genres cannot be picked`() {
        show(withGenres.copy(settings = withGenres.settings.copy(genresOn = false)))

        composeRule.onNodeWithText("Rock").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Preset per genre").performScrollTo().performClick()

        assertEquals(listOf("genres true"), done)
    }

    @Test
    fun `volume levelling shows its mode and works also with the equalizer off`() {
        show(EqualizerUiState(EqualizerSettings(levelling = VolumeLevelling.TRACK), FIVE_BANDS_HZ))

        composeRule.onNodeWithText("Volume levelling").performScrollTo()
        composeRule.onNodeWithText("Same loudness for every song").performScrollTo().assertIsSelected()
        composeRule.onNodeWithText("Same loudness for every album").performScrollTo().assertIsEnabled().performClick()
        composeRule.onNodeWithText("Off").performScrollTo().performClick()

        assertEquals(listOf("levelling ALBUM", "levelling OFF"), done)
    }

    @Test
    fun `the volume levelling section passes the accessibility checks`() {
        show(carnatic)
        composeRule.onNodeWithText("Same loudness for every album").performScrollTo()

        composeRule.checkAccessibility()
    }

    @Test
    fun `the genre section passes the accessibility checks`() {
        show(withGenres)
        composeRule.onNodeWithText("Rock").performScrollTo()

        composeRule.checkAccessibility()
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
    fun `the presets per genre`() = shot("equalizer_genres") {
        show(withGenres)
        composeRule.onNodeWithText("Rock").performScrollTo()
    }

    @Test
    fun `the volume levelling`() = shot("equalizer_levelling") {
        show(carnatic.copy(settings = carnatic.settings.copy(levelling = VolumeLevelling.ALBUM)))
        composeRule.onNodeWithText("Same loudness for every album").performScrollTo()
    }

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
