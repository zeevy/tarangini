/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio.form

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
class StationFormContentTest {
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
    private val actions = StationFormActions(
        onNameChange = { done += "name $it" },
        onAddressChange = { done += "address $it" },
        onTest = { done += "test" },
        onSave = { done += "save" },
        onBack = { done += "back" },
    )

    private fun show(state: StationFormUiState, name: String = "", address: String = "") {
        composeRule.setContent {
            // A text field needs its text back, as the ViewModel gives it in the app.
            var typedName by remember { mutableStateOf(name) }
            var typedAddress by remember { mutableStateOf(address) }
            val typing = actions.copy(
                onNameChange = {
                    typedName = it
                    actions.onNameChange(it)
                },
                onAddressChange = {
                    typedAddress = it
                    actions.onAddressChange(it)
                },
            )
            TaranginiTheme { StationFormContent(state, typedName, typedAddress, typing) }
        }
    }

    @Test
    fun `the form for a new station takes a name and an address, and each button does its own thing`() {
        show(StationFormUiState())

        composeRule.onNodeWithText("Add station").assertIsDisplayed()
        composeRule.onNodeWithText("Name").performTextInput("City")
        composeRule.onNodeWithText("Stream address").performTextInput("http://c")
        composeRule.onNodeWithText("Test").performClick()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("name City", "address http://c", "test", "save", "back"), done)
    }

    @Test
    fun `a stored station shows its texts under the title for a change`() {
        show(StationFormUiState(isEdit = true), "Radio Bhairavi", "https://bhairavi.example/stream")

        composeRule.onNodeWithText("Edit station").assertIsDisplayed()
        composeRule.onNodeWithText("Radio Bhairavi").assertIsDisplayed()
        composeRule.onNodeWithText("https://bhairavi.example/stream").assertIsDisplayed()
    }

    @Test
    fun `what is wrong shows under its field`() {
        show(StationFormUiState(nameFault = NameFault.EMPTY, addressFault = AddressFault.NOT_HTTP))

        composeRule.onNodeWithText("Give the station a name.").assertIsDisplayed()
        composeRule.onNodeWithText("The address must start with http:// or https://").assertIsDisplayed()
    }

    @Test
    fun `a test that found a stream says what answered`() {
        show(StationFormUiState(test = TestState.Done(StreamTest.Found("audio/mpeg", "City FM", 128))))

        composeRule.onNodeWithText(
            "A stream answers (audio/mpeg). It calls itself City FM. 128 kbps.",
        ).assertIsDisplayed()
    }

    @Test
    fun `a test that runs and one that found nothing say so`() {
        show(StationFormUiState(test = TestState.Done(StreamTest.Failed(StreamTestFault.NO_ANSWER))))

        composeRule.onNodeWithText("No server answers at this address.").assertIsDisplayed()
    }

    @Test
    fun `a failed save says so`() {
        show(StationFormUiState(saveFailed = true))

        composeRule.onNodeWithText("The station could not be saved.").assertIsDisplayed()
    }
}
