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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
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
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.savedPicture
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
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

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

    private val tenBands = EqualizerUiState(
        EqualizerSettings(isOn = true, preset = null, ownGains = listOf(4f, 3f, 2f, 0f, -1f, -2f, 0f, 2f, 3f, 5f)),
        listOf(31f, 62f, 125f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f),
    )

    private fun show(state: EqualizerUiState?) = composeRule.setContent {
        TaranginiTheme { EqualizerContent(state, actions, { done += "back" }) }
    }

    @Test
    fun `the screen shows the preset, each band with its gain and the bass boost`() {
        show(carnatic)

        composeRule.onNodeWithContentDescription("Equalizer on").assertIsOn()
        composeRule.onNodeWithText("Carnatic").assertIsSelected()
        composeRule.onNodeWithText("Carnatic · 5 bands").assertExists()
        listOf("60", "910", "3.6k", "14k", "+3").forEach { composeRule.onNodeWithText(it).assertExists() }
        // A screen reader hears the full band name and the gain with its unit.
        composeRule.onNodeWithContentDescription("910 Hz").assertExists()
        composeRule.onNodeWithContentDescription("3.6 kHz").assertExists()
        composeRule.onNodeWithText("40 %").assertExists()
    }

    @Test
    fun `the switch row, a preset and the back arrow do what they say`() {
        show(carnatic)

        composeRule.onNodeWithContentDescription("Equalizer on").performClick()
        composeRule.onNodeWithText("Bass").performClick()
        composeRule.onNodeWithText("Reset").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("on false", "preset BASS", "preset FLAT", "back"), done)
    }

    @Test
    fun `a band moves in whole dB steps, and the bass in whole percent`() {
        show(carnatic)

        composeRule.onNodeWithContentDescription("230 Hz").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(4.3f) }
        composeRule.onNodeWithContentDescription("Bass boost").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(57.6f) }

        assertEquals(listOf("band 1 4.0", "bass 58"), done)
    }

    @Test
    fun `with the equalizer off the presets and sliders cannot be used`() {
        show(EqualizerUiState(EqualizerSettings(), FIVE_BANDS_HZ))

        composeRule.onNodeWithContentDescription("Equalizer on").assertIsOff()
        composeRule.onNodeWithText("Bass").assertIsNotEnabled()
        composeRule.onNodeWithText("Reset").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("60 Hz").assertIsNotEnabled()
        // The switch still works, or the equalizer could not be turned on again.
        composeRule.onNodeWithContentDescription("Equalizer on").assertIsEnabled()
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
    @Config(qualifiers = "+night")
    fun `the screen passes the accessibility checks in the dark look`() {
        show(carnatic)

        composeRule.checkAccessibility()
    }

    @Test
    fun `a preset on the screen`() = shot("equalizer") { show(carnatic) }

    @Test
    @Config(qualifiers = "+night")
    fun `a preset on the screen in the dark look`() = shot("equalizer_dark") { show(carnatic) }

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
    fun `ten bands scroll sideways and start with the lowest in view`() {
        show(tenBands)

        composeRule.onNodeWithText("Custom · 10 bands").assertExists()
        composeRule.onNodeWithContentDescription("31 Hz").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("500 Hz").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("16 kHz").assertIsNotDisplayed()
    }

    @Test
    fun `ten bands`() = shot("equalizer_ten_bands") { show(tenBands) }

    @Test
    @Config(qualifiers = "+night")
    fun `ten bands in the dark look`() = shot("equalizer_ten_bands_dark") { show(tenBands) }

    @Test
    @Config(qualifiers = "w640dp-h320dp-xhdpi")
    fun `ten bands on a wide screen`() = shot("equalizer_ten_bands_wide") { show(tenBands) }

    @Test
    @Config(qualifiers = "+night")
    fun `the equalizer off in the dark look`() = shot("equalizer_off_dark") {
        show(EqualizerUiState(EqualizerSettings(), FIVE_BANDS_HZ))
    }

    @Test
    fun `the equalizer off`() = shot("equalizer_off") { show(EqualizerUiState(EqualizerSettings(), FIVE_BANDS_HZ)) }

    private fun shot(name: String, content: () -> Unit) {
        val picture = savedPicture("$name")
        content()
        composeRule.onRoot().captureRoboImage(picture.path)
    }

    private companion object {
        const val LARGEST_FONT = 2f
    }
}
