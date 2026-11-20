/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.model.Station
import com.siliconcircuits.tarangini.core.domain.station.AddressFault
import com.siliconcircuits.tarangini.core.domain.station.NameFault
import com.siliconcircuits.tarangini.core.domain.station.StreamTest
import com.siliconcircuits.tarangini.core.domain.station.StreamTestFault
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.radio.form.StationFormActions
import com.siliconcircuits.tarangini.feature.radio.form.StationFormContent
import com.siliconcircuits.tarangini.feature.radio.form.StationFormUiState
import com.siliconcircuits.tarangini.feature.radio.form.TestState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Radio tab and the station form at the narrowest width, in light and dark. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class RadioScreenshotTest {
    private val radioActions = RadioActions({}, {}, {}, {})
    private val formActions = StationFormActions({}, {}, {}, {}, {})
    private val stations = listOf(
        Station(1, "Radio Bhairavi", listOf("https://bhairavi.example/stream")),
        Station(
            2,
            "A station with a very long name that does not fit",
            listOf("http://long.example/a/very/long/address/of/a/stream.m3u8"),
        ),
    )

    @Test
    fun `no stations`() = both("radio_empty") { RadioContent(RadioUiState(emptyList()), radioActions) }

    @Test
    fun `stations`() = both("radio_stations") { RadioContent(RadioUiState(stations), radioActions) }

    @Test
    fun `the form with a message and a test result`() = both("station_form") {
        val state = StationFormUiState(
            nameFault = NameFault.EMPTY,
            test = TestState.Done(StreamTest.Found("audio/mpeg", "Radio Bhairavi", 128)),
        )
        StationFormContent(state, "", "https://bhairavi.example/stream", formActions)
    }

    private fun both(name: String, content: @Composable () -> Unit) {
        listOf("light" to false, "dark" to true).forEach { (theme, dark) ->
            val picture = File("src/test/screenshots/${name}_$theme.png")
            // Without this, a test with no saved picture passes: there is nothing to compare with.
            assertTrue(
                "No saved picture $picture. Run the tests with -PrecordScreenshots.",
                recording || picture.exists(),
            )
            captureRoboImage(picture.path) {
                TaranginiTheme(darkTheme = dark, dynamicColor = false) { content() }
            }
        }
    }
}

private val recording = System.getProperty("roborazzi.test.record").toBoolean()
