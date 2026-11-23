/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset

/**
 * Runs the checks of the Accessibility Test Framework over what is on screen: a label a screen
 * reader can say, the size of touch targets, the contrast of text and more. A warning fails the
 * test as an error does. The test needs `@GraphicsMode(NATIVE)`, or there is no picture to
 * measure the contrast on.
 */
internal fun ComposeTestRule.checkAccessibility() = onRoot().checkRoboAccessibility(
    RoborazziATFAccessibilityCheckOptions(
        checker = RoborazziATFAccessibilityChecker(preset = AccessibilityCheckPreset.LATEST),
        failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
    ),
)
