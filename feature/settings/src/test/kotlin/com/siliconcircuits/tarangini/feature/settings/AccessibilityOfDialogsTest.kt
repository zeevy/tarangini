/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.siliconcircuits.tarangini.core.domain.model.LibraryLicence
import com.siliconcircuits.tarangini.core.domain.model.OpenSourceLibrary
import com.siliconcircuits.tarangini.core.domain.model.ScanSettings
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.settings.licences.LicencesContent
import com.siliconcircuits.tarangini.feature.settings.licences.LicencesUiState
import java.io.File
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
import org.robolectric.annotation.GraphicsMode

/**
 * The accessibility checks of the dialogs of Settings and of the licences screen, and a picture of
 * each dialog at the largest font. A missing label, a small touch target or low contrast fails a test.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityOfDialogsTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private val state = SettingsUiState(
        scan = ScanSettings(30, setOf("external_primary/Calls")),
        folders = listOf("external_primary/Calls", "external_primary/Music", "external_primary/Calls/Old"),
    )

    private fun open(row: String) {
        composeRule.setContent {
            TaranginiTheme { SettingsScreen(onBackClick = {}) { SettingsContent(state, "1.0", SettingsActions()) } }
        }
        // A row can be below the fold, and a tap there does nothing.
        composeRule.onNodeWithText(row).performScrollTo().performClick()
    }

    @Test
    fun `the dialog of the hidden folders passes the checks`() {
        open("Hidden folders")

        composeRule.checkAccessibilityOfDialog()
    }

    @Test
    fun `the dialog of the shortest track passes the checks`() {
        open("Shortest track")

        composeRule.checkAccessibilityOfDialog()
    }

    @Test
    fun `the question before the history is cleared passes the checks`() {
        open("Clear history")

        composeRule.checkAccessibilityOfDialog()
    }

    @Test
    fun `the licences screen passes the checks`() {
        val apache = LibraryLicence("Apache-2.0", "Apache License 2.0")
        val libraries =
            listOf(OpenSourceLibrary("Activity", "1.13.0", apache), OpenSourceLibrary("Manrope", "", apache))
        composeRule.setContent {
            TaranginiTheme { LicencesContent(LicencesUiState(libraries), onOpen = {}, onClose = {}, onBack = {}) }
        }

        composeRule.checkAccessibility()
    }

    @Test
    fun `the hidden folders with the largest font`() = shotAtLargestFont("Hidden folders", "settings_hidden_folders")

    @Test
    fun `the shortest track with the largest font`() = shotAtLargestFont("Shortest track", "settings_shortest_track")

    @Test
    fun `the clear history question with the largest font`() =
        shotAtLargestFont("Clear history", "settings_clear_history")

    private fun shotAtLargestFont(row: String, name: String) {
        val picture = savedPicture("${name}_largest_font")
        RuntimeEnvironment.setFontScale(LARGEST_FONT)
        open(row)
        // The dialog opens in a later frame than the tap.
        composeRule.waitForIdle()
        // The whole screen, so the dialog is in the picture: it is a window of its own.
        captureScreenRoboImage(picture.path)
    }
}

private const val LARGEST_FONT = 2f
