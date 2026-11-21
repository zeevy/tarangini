/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings.backup

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.siliconcircuits.tarangini.core.domain.repository.BackupRestore
import com.siliconcircuits.tarangini.core.testing.MainDispatcherRule
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryScanner
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.testing.settings.FakeBackupFiles
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.settings.SettingsActions
import com.siliconcircuits.tarangini.feature.settings.SettingsContent
import com.siliconcircuits.tarangini.feature.settings.SettingsScreen
import com.siliconcircuits.tarangini.feature.settings.SettingsUiState
import com.siliconcircuits.tarangini.feature.settings.checkAccessibilityOfDialog
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackupTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val files = FakeBackupFiles()
    private val scanner = FakeLibraryScanner()
    private val done = mutableListOf<String>()
    private val actions = BackupActions(
        onBackUp = { done += "back up" },
        onPickFile = { done += "pick" },
        onRestore = { replace -> done += "restore replace=$replace" },
        onCancel = { done += "cancel" },
        onClose = { done += "close" },
    )

    /** The backup rows inside the frame of Settings, as the app shows them. */
    private fun show(state: BackupUiState = BackupUiState(), asksHow: Boolean = false) = composeRule.setContent {
        TaranginiTheme {
            SettingsScreen(onBackClick = {}) {
                SettingsContent(SettingsUiState(), "1.0", SettingsActions(), backup = {
                    BackupContent(state, actions, asksHow)
                })
            }
        }
    }

    @Test
    fun `a backup is written, and a failure is told as one`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = BackupViewModel(files, scanner)

        viewModel.backUpTo("content://a")
        advanceUntilIdle()
        val saved = viewModel.state.value
        files.backupWorks = false
        viewModel.backUpTo("content://b")
        advanceUntilIdle()
        val failed = viewModel.state.value
        viewModel.close()

        assertEquals(BackupUiState(saved = true) to BackupUiState(saved = false), saved to failed)
        assertEquals(BackupUiState(), viewModel.state.value)
        assertEquals(listOf("backup content://a", "backup content://b"), files.calls)
    }

    @Test
    fun `a restore that was read is followed by a scan, and one that was refused is not`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel = BackupViewModel(files, scanner)
            files.restoreResult = BackupRestore.Done(favourites = 2, playlists = 1, songsNotFound = 0)

            viewModel.restoreFrom("content://a", replace = true)
            advanceUntilIdle()
            val read = viewModel.state.value.restored to scanner.scanCount
            files.restoreResult = BackupRestore.NotABackup
            viewModel.restoreFrom("content://b", replace = false)
            advanceUntilIdle()

            assertEquals(BackupRestore.Done(2, 1, 0) to 1, read)
            assertEquals(BackupRestore.NotABackup to 1, viewModel.state.value.restored to scanner.scanCount)
            assertEquals(listOf("restore content://a replace=true", "restore content://b replace=false"), files.calls)
        }

    @Test
    fun `while a restore runs a second one is not started`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = BackupViewModel(files, scanner)
        files.restoreWaitsFor = CompletableDeferred()

        viewModel.restoreFrom("content://a", replace = false)
        advanceUntilIdle()
        val between = viewModel.state.value
        viewModel.restoreFrom("content://b", replace = true)
        viewModel.backUpTo("content://c")
        files.restoreWaitsFor?.complete(Unit)
        advanceUntilIdle()

        assertEquals(BackupUiState(busy = true), between)
        assertEquals(listOf("restore content://a replace=false"), files.calls)
        assertEquals(false, viewModel.state.value.busy)
    }

    @Test
    fun `while a backup is read the rows say so and cannot be tapped`() {
        show(BackupUiState(busy = true))

        composeRule.onNodeWithText("Back up to a file").performScrollTo().performClick()
        composeRule.onNodeWithText("Restore from a file").performScrollTo().performClick()

        assertEquals(emptyList<String>(), done)
        composeRule.onAllNodesWithText("Working. Please wait.").assertCountEquals(2)
    }

    @Test
    fun `the rows open the file pickers`() {
        show()

        composeRule.onNodeWithText("Back up to a file").performScrollTo().performClick()
        composeRule.onNodeWithText("Restore from a file").performScrollTo().performClick()

        assertEquals(listOf("back up", "pick"), done)
    }

    @Test
    fun `a chosen file asks how to read it, and each answer is passed on`() {
        var asks by mutableStateOf(true)
        composeRule.setContent { TaranginiTheme { BackupContent(BackupUiState(), actions, asks) } }

        composeRule.onNodeWithText("Replace").performClick()
        composeRule.onNodeWithText("Add").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        asks = false

        assertEquals(listOf("restore replace=true", "restore replace=false", "cancel"), done)
        composeRule.onNodeWithText("Replace").assertDoesNotExist()
    }

    @Test
    fun `the end of a restore is told with its numbers, and each refusal with its reason`() {
        var state by mutableStateOf(BackupUiState(restored = BackupRestore.Done(12, 3, 2)))
        composeRule.setContent { TaranginiTheme { BackupContent(state, actions) } }

        composeRule
            .onNodeWithText("Restored. Favourites: 12. Playlists: 3. 2 songs of the file are not in your library.")
            .assertIsDisplayed()
        state = BackupUiState(restored = BackupRestore.NoLibrary)
        composeRule.onNodeWithText("Your library is empty.", substring = true).assertIsDisplayed()
        state = BackupUiState(restored = BackupRestore.NewerVersion)
        composeRule.onNodeWithText("newer version", substring = true).assertIsDisplayed()
        state = BackupUiState(saved = false)
        composeRule.onNodeWithText("The backup could not be saved.").assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(listOf("close"), done)
    }

    @Test
    fun `the question how to restore passes the accessibility checks`() {
        show(asksHow = true)

        composeRule.checkAccessibilityOfDialog()
    }

    @Test
    fun `the question how to restore with the largest font`() {
        val picture = savedPicture("settings_restore_question_largest_font")
        RuntimeEnvironment.setFontScale(LARGEST_FONT)
        show(asksHow = true)
        // The dialog opens in a later frame than the content.
        composeRule.waitForIdle()

        captureScreenRoboImage(picture.path)
    }
}

private const val LARGEST_FONT = 2f
