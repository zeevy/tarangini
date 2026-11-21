/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.library.ScanOutcome
import com.siliconcircuits.tarangini.core.domain.library.ScanState
import com.siliconcircuits.tarangini.core.domain.library.ScanSummary
import com.siliconcircuits.tarangini.core.domain.model.ScanSettings
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.settings.backup.BackupActions
import com.siliconcircuits.tarangini.feature.settings.backup.BackupContent
import com.siliconcircuits.tarangini.feature.settings.backup.BackupUiState
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings at the narrowest width. The app has one dark look, so each state has one picture. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class SettingsScreenshotTest {
    @Test
    fun `a new install`() = shot("settings_first", SettingsUiState())

    private val set = SettingsUiState(
        scan = ScanSettings(120, setOf("external_primary/Calls", "external_primary/WhatsApp")),
        scanState = ScanState.Finished(ScanOutcome.Success(ScanSummary(12, 0, 3, 100))),
    )

    @Test
    fun `with things left out and a scan that ended`() = shot("settings_set", set)

    @Test
    fun `with the largest font`() {
        RuntimeEnvironment.setFontScale(2f)
        shot("settings_set_largest_font", set)
    }

    @Test
    @Config(qualifiers = "w640dp-h320dp-xhdpi")
    fun `on a wide screen`() = shot("settings_set_wide", set)

    private fun shot(name: String, state: SettingsUiState) {
        val picture = File("src/test/screenshots/$name.png")
        // Without this, a test with no saved picture passes: there is nothing to compare with.
        assertTrue("No saved picture $picture. Run the tests with -PrecordScreenshots.", recording || picture.exists())
        captureRoboImage(picture.path) {
            TaranginiTheme {
                SettingsScreen(onBackClick = {}) {
                    SettingsContent(state, "1.0.0", SettingsActions(), backup = {
                        BackupContent(BackupUiState(), BackupActions())
                    })
                }
            }
        }
    }
}

private val recording = System.getProperty("roborazzi.test.record").toBoolean()
