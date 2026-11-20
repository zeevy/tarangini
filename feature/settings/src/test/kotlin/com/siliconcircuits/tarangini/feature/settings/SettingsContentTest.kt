/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.siliconcircuits.tarangini.core.domain.model.ScanSettings
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
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
class SettingsContentTest {
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
    private val actions = SettingsActions(
        onSetMinTrackSeconds = { done += "length $it" },
        onSetFolderHidden = { path, hidden -> done += "hide $path $hidden" },
        onRescan = { done += "rescan" },
        onClearHistory = { done += "clear" },
        onOpenSource = { done += "source" },
    )
    private val state = SettingsUiState(
        scan = ScanSettings(30, setOf("external_primary/Calls")),
        folders = listOf("external_primary/Calls", "external_primary/Music"),
    )

    private fun show() = composeRule.setContent {
        TaranginiTheme { SettingsContent(state, "1.0", actions) }
    }

    @Test
    fun `the rows say what is set, and Rescan and Source code act at once`() {
        show()

        composeRule.onNodeWithText("1 folder is hidden").assertExists()
        composeRule.onNodeWithText("30 seconds").assertExists()
        composeRule.onNodeWithText("1.0").assertExists()
        composeRule.onNodeWithText("Rescan now").performClick()
        composeRule.onNodeWithText("Source code").performClick()

        assertEquals(listOf("rescan", "source"), done)
    }

    @Test
    fun `a folder is hidden or shown again with one tap`() {
        show()

        composeRule.onNodeWithText("Hidden folders").performClick()
        composeRule.onNodeWithText("Music").performClick()
        composeRule.onNodeWithText("Calls").performClick()

        assertEquals(listOf("hide external_primary/Music true", "hide external_primary/Calls false"), done)
    }

    @Test
    fun `a folder inside a hidden folder shows as hidden and cannot be changed by itself`() {
        composeRule.setContent {
            TaranginiTheme {
                SettingsContent(
                    state.copy(folders = listOf("external_primary/Calls", "external_primary/Calls/Old")),
                    "1.0",
                    actions,
                )
            }
        }

        composeRule.onNodeWithText("Hidden folders").performClick()
        composeRule.onNodeWithText("Calls/Old").assertIsOn().assertIsNotEnabled()
        composeRule.onNodeWithText("Calls/Old").performClick()

        assertEquals(emptyList<String>(), done)
    }

    @Test
    fun `a pick of the shortest track is saved and closes the dialog`() {
        show()

        composeRule.onNodeWithText("Shortest track").performClick()
        composeRule.onNodeWithText("1 minute").performClick()

        assertEquals(listOf("length 60"), done)
        composeRule.onNodeWithText("2 minutes").assertDoesNotExist()
    }

    @Test
    fun `the history is cleared only after the question is answered with Clear`() {
        show()

        composeRule.onNodeWithText("Clear history").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        val afterCancel = done.toList()
        composeRule.onNodeWithText("Clear history").performClick()
        composeRule.onNodeWithText("Clear").performClick()

        assertEquals(emptyList<String>(), afterCancel)
        assertEquals(listOf("clear"), done)
    }
}
