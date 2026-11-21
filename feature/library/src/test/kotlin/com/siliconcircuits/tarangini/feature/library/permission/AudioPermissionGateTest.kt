/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.permission

import android.app.Application
import android.content.ComponentName
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class AudioPermissionGateTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val permission = audioPermissionFor(Build.VERSION.SDK_INT)
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    /** What the user answers to the question of Android. */
    private var userAllows = false

    /** What Android says after a no: true after a first no, false once it will not ask again. */
    private var androidAsksAgain = false
    private var requests = 0

    /** Stands in for the question of Android and answers it at once. */
    private val answers = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                requests++
                assertEquals(permission, input)
                if (userAllows) shadowOf(app).grantPermissions(permission)
                shadowOf(app.packageManager).setShouldShowRequestPermissionRationale(permission, androidAsksAgain)
                dispatchResult(requestCode, userAllows)
            }
        }
    }

    private fun showGate() {
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides answers) {
                AudioPermissionGate { Text("the library") }
            }
        }
    }

    @Test
    fun `with the permission the library shows and nothing is asked`() {
        shadowOf(app).grantPermissions(permission)

        showGate()

        composeRule.onNodeWithText("the library").assertIsDisplayed()
        assertEquals(0, requests)
    }

    @Test
    fun `without the permission the reason shows first, and Android is not asked yet`() {
        showGate()

        composeRule.onNodeWithText("Allow access to your music").assertIsDisplayed()
        composeRule.onNodeWithText("the library").assertDoesNotExist()
        assertEquals(0, requests)
    }

    @Test
    fun `a yes shows the library`() {
        userAllows = true
        showGate()

        composeRule.onNodeWithText("Allow access").performClick()

        composeRule.onNodeWithText("the library").assertIsDisplayed()
        assertEquals(1, requests)
    }

    @Test
    fun `a first no says what is missing and lets the user try again`() {
        androidAsksAgain = true
        showGate()

        composeRule.onNodeWithText("Allow access").performClick()

        composeRule.onNodeWithText("Your music is not shown").assertIsDisplayed()
        userAllows = true
        composeRule.onNodeWithText("Allow access").performClick()
        composeRule.onNodeWithText("the library").assertIsDisplayed()
    }

    @Test
    fun `a second no leads to the app settings`() {
        androidAsksAgain = true
        showGate()
        composeRule.onNodeWithText("Allow access").performClick()
        androidAsksAgain = false

        composeRule.onNodeWithText("Allow access").performClick()

        composeRule.onNodeWithText("Access to music is switched off").assertIsDisplayed()
        assertNull(shadowOf(app).nextStartedActivity)
        composeRule.onNodeWithText("Open app settings").performClick()
        val opened = shadowOf(app).nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, opened.action)
        assertEquals("package:${app.packageName}", opened.dataString)
    }

    @Test
    fun `a question closed without an answer keeps the button that asks`() {
        showGate()

        composeRule.onNodeWithText("Allow access").performClick()

        composeRule.onNodeWithText("Your music is not shown").assertIsDisplayed()
        composeRule.onNodeWithText("Allow access").assertIsDisplayed()
        composeRule.onNodeWithText("Open app settings").assertDoesNotExist()
    }

    @Test
    fun `when Android no longer shows its question, the second try leads to the app settings`() {
        showGate()

        composeRule.onNodeWithText("Allow access").performClick()
        composeRule.onNodeWithText("Allow access").performClick()

        composeRule.onNodeWithText("Access to music is switched off").assertIsDisplayed()
        composeRule.onNodeWithText("Open app settings").assertIsDisplayed()
        assertEquals(2, requests)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `Android 12 asks for the permission for all files and names its place in the settings`() {
        showGate()

        composeRule.onNodeWithText("Allow access").performClick()
        composeRule.onNodeWithText("Allow access").performClick()

        assertEquals("android.permission.READ_EXTERNAL_STORAGE", permission)
        composeRule.onNodeWithText("Files and media", substring = true).assertIsDisplayed()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `Android 13 asks for the permission for audio alone and names its place in the settings`() {
        showGate()

        composeRule.onNodeWithText("Allow access").performClick()
        composeRule.onNodeWithText("Allow access").performClick()

        assertEquals("android.permission.READ_MEDIA_AUDIO", permission)
        composeRule.onNodeWithText("Music and audio", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a permission given in the app settings shows the library when the user comes back`() {
        showGate()
        composeRule.onNodeWithText("Allow access").performClick()
        composeRule.onNodeWithText("Allow access").performClick()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        shadowOf(app).grantPermissions(permission)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.onNodeWithText("the library").assertIsDisplayed()
    }

    @Test
    fun `a permission taken away in the app settings shows the reason when the user comes back`() {
        shadowOf(app).grantPermissions(permission)
        showGate()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        shadowOf(app).denyPermissions(permission)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.onNodeWithText("Allow access to your music").assertIsDisplayed()
        composeRule.onNodeWithText("the library").assertDoesNotExist()
    }
}
