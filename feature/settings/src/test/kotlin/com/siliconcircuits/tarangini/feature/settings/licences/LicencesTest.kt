/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings.licences

import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.model.LibraryLicence
import com.siliconcircuits.tarangini.core.domain.model.OpenSourceLibrary
import com.siliconcircuits.tarangini.core.domain.repository.LicenceCatalog
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.settings.MainDispatcherRule
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class LicencesTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val apache = LibraryLicence("Apache-2.0", "Apache License 2.0")
    private val libraries = listOf(
        OpenSourceLibrary("Activity", "1.13.0", apache),
        OpenSourceLibrary("A library with a very long name that does not fit on one line of a phone", "2", apache),
        OpenSourceLibrary("Manrope", "", LibraryLicence("Manrope-OFL", "SIL Open Font License 1.1")),
    )
    private val catalog = object : LicenceCatalog {
        override suspend fun libraries() = libraries

        override suspend fun text(licenceId: String) = "Text of $licenceId".takeIf { licenceId == "Apache-2.0" }
    }

    @Test
    fun `the libraries show, a tap opens the licence text, and close returns to the list`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel = LicencesViewModel(catalog)
            val first = viewModel.state.value

            viewModel.open(libraries[0])
            val opened = viewModel.state.value.opened
            viewModel.open(libraries[2])
            val noText = viewModel.state.value.opened
            viewModel.close()

            assertEquals(LicencesUiState(libraries), first)
            assertEquals(OpenedLicence("Apache License 2.0", "Text of Apache-2.0"), opened)
            assertEquals(OpenedLicence("SIL Open Font License 1.1", null), noText)
            assertNull(viewModel.state.value.opened)
        }

    @Test
    fun `the list`() = shot("licences_list", LicencesUiState(libraries))

    @Test
    fun `a licence text`() {
        val text = "Apache License\nVersion 2.0, January 2004\n\n" + "Terms. ".repeat(80)
        shot("licences_text", LicencesUiState(libraries, OpenedLicence("Apache License 2.0", text)))
    }

    @Test
    fun `a licence text with the notice of the library above it`() {
        val opened = OpenedLicence("Apache License 2.0", "Apache License\nVersion 2.0", "Made by A\nCopyright 2020 A")
        shot("licences_text_notice", LicencesUiState(libraries, opened))
    }

    @Test
    fun `a library with a notice opens with it`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = LicencesViewModel(catalog)

        viewModel.open(libraries[0].copy(notice = "Made by A"))

        assertEquals(
            OpenedLicence("Apache License 2.0", "Text of Apache-2.0", "Made by A"),
            viewModel.state.value.opened,
        )
    }

    @Test
    fun `the list with the largest font`() {
        RuntimeEnvironment.setFontScale(2f)
        shot("licences_list_largest_font", LicencesUiState(libraries))
    }

    private fun shot(name: String, state: LicencesUiState) {
        val picture = File("src/test/screenshots/$name.png")
        // Without this, a test with no saved picture passes: there is nothing to compare with.
        assertTrue("No saved picture $picture. Run the tests with -PrecordScreenshots.", recording || picture.exists())
        captureRoboImage(picture.path) {
            TaranginiTheme { LicencesContent(state, onOpen = {}, onClose = {}, onBack = {}) }
        }
    }
}

private val recording = System.getProperty("roborazzi.test.record").toBoolean()
