/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import org.hamcrest.Matcher
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.not

/**
 * Runs the checks of the Accessibility Test Framework over what is on screen: a label a screen
 * reader can say, the size of touch targets, the contrast of text and more. A warning fails the
 * test as an error does. The test needs `@GraphicsMode(NATIVE)`, or there is no picture to
 * measure the contrast on.
 */
internal fun ComposeTestRule.checkAccessibility() = onRoot().checkRoboAccessibility(checkOptions())

/**
 * The same checks for a dialog. A dialog is a window of its own on top of the screen, so the
 * screen has more than one root while it is open; the window opened last is checked.
 */
internal fun ComposeTestRule.checkAccessibilityOfDialog() = topWindow().checkRoboAccessibility(checkOptions())

/**
 * The checks for a menu, without the contrast of its texts. Robolectric gives the checker the
 * picture of the screen under a menu, not of the menu, so a contrast measured there says nothing.
 * The labels and the size of the touch targets are checked as everywhere.
 */
internal fun ComposeTestRule.checkAccessibilityOfMenu() =
    topWindow().checkRoboAccessibility(checkOptions(matchesCheck(TextContrastCheck::class.java)))

/** The window on top of the screen. With no such window the test fails, or it would check the screen alone. */
private fun ComposeTestRule.topWindow(): SemanticsNodeInteraction {
    val windows = onAllNodes(isRoot())
    check(windows.fetchSemanticsNodes().size > 1) { "No dialog or menu is open." }
    return windows.onLast()
}

private fun checkOptions(leftOut: Matcher<in AccessibilityViewCheckResult> = not(anything())) =
    RoborazziATFAccessibilityCheckOptions(
        checker = RoborazziATFAccessibilityChecker(preset = AccessibilityCheckPreset.LATEST, suppressions = leftOut),
        failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
    )
