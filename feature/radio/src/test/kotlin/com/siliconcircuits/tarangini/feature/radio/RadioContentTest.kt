/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
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
import org.junit.Assert.assertEquals
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

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class RadioContentTest {
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
    private val actions = RadioActions(
        onPlay = { done += "play ${it.name}" },
        onAdd = { done += "add" },
        onEdit = { done += "edit ${it.name}" },
        onRemove = { done += "remove ${it.name}" },
    )
    private val stations = listOf(
        Station(1, "Alpha", listOf("http://alpha.example/")),
        Station(2, "Beta", listOf("https://beta.example/live.m3u8")),
    )

    private fun show(state: RadioUiState) {
        composeRule.setContent { TaranginiTheme(dynamicColor = false) { RadioContent(state, actions) } }
    }

    @Test
    fun `with no station the screen says so and offers to add one`() {
        show(RadioUiState(emptyList()))

        composeRule.onNodeWithText("No stations yet").assertIsDisplayed()
        composeRule.onNodeWithText("Add station").performClick()

        assertEquals(listOf("add"), done)
    }

    @Test
    fun `each station shows its name and address, and a tap plays it`() {
        show(RadioUiState(stations))

        composeRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeRule.onNodeWithText("https://beta.example/live.m3u8").assertIsDisplayed()
        composeRule.onNodeWithText("Beta").performClick()
        composeRule.onNodeWithContentDescription("Add station").performClick()

        assertEquals(listOf("play Beta", "add"), done)
    }

    @Test
    fun `the menu of a station edits it`() {
        show(RadioUiState(stations))

        composeRule.onNodeWithContentDescription("More for Alpha").performClick()
        composeRule.onNodeWithText("Edit").performClick()

        assertEquals(listOf("edit Alpha"), done)
    }

    @Test
    fun `a station is removed only after the person said yes`() {
        show(RadioUiState(stations))

        composeRule.onNodeWithContentDescription("More for Beta").performClick()
        composeRule.onNodeWithText("Remove").performClick()
        composeRule.onNodeWithText("Remove Beta?").assertIsDisplayed()
        assertEquals(emptyList<String>(), done)

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Remove Beta?").assertDoesNotExist()
        assertEquals(emptyList<String>(), done)

        composeRule.onNodeWithContentDescription("More for Beta").performClick()
        composeRule.onNodeWithText("Remove").performClick()
        composeRule.onNodeWithText("Remove").performClick()
        assertEquals(listOf("remove Beta"), done)
    }

    @Test
    fun `a failed read says so`() {
        show(RadioUiState(failed = true))

        composeRule.onNodeWithText("The stations could not be read").assertIsDisplayed()
    }
}
