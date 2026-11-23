/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.R
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.core.ui.theme.lookColors
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The parts of the new look: chips with lamps, the display, the framed card, the list card and the switch. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class LookPartsTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(RegisterComponentActivityRule()).around(composeRule)

    @Test
    fun `the parts in the light look`() = shot("look_parts", darkTheme = false)

    @Test
    fun `the parts in the dark look`() = shot("look_parts_dark", darkTheme = true)

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi", fontScale = 2f)
    fun `the parts at the largest font on a narrow phone`() = shot("look_parts_largest_font", darkTheme = false)

    @Test
    fun `a tab chip, a switch chip and an icon chip say their state`() {
        composeRule.setContent { TaranginiTheme(darkTheme = false) { Parts() } }

        composeRule.onNodeWithText("Songs").assertIsSelected()
        composeRule.onNodeWithContentDescription("Shuffle").assertIsOff().performClick().assertIsOn()
    }

    @Test
    fun `a chip with a label says the label in place of its text, and is 48 dp wide`() {
        composeRule.setContent {
            TaranginiTheme {
                LedChip("1", lit = true, onClick = {}, kind = ChipKind.BUTTON, label = "Repeat: this song")
            }
        }

        composeRule.onNodeWithContentDescription("Repeat: this song").assertWidthIsAtLeast(48.dp)
        composeRule.onNodeWithText("1").assertDoesNotExist()
    }

    @Test
    fun `the parts pass the accessibility checks in both looks`() {
        var dark by mutableStateOf(false)
        composeRule.setContent { TaranginiTheme(darkTheme = dark) { Parts() } }
        composeRule.checkAccessibility()

        dark = true
        composeRule.waitForIdle()
        composeRule.checkAccessibility()
    }

    private fun shot(name: String, darkTheme: Boolean) {
        composeRule.setContent { TaranginiTheme(darkTheme = darkTheme) { Parts() } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(savedPicture(name).path)
    }
}

@Composable
private fun Parts() {
    var tab by remember { mutableStateOf("Songs") }
    var shuffle by remember { mutableStateOf(false) }
    var gapless by remember { mutableStateOf(true) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            listOf("Songs", "Albums", "Artists").forEach { name ->
                LedChip(name, lit = tab == name, onClick = { tab = name })
            }
        }
        FrameCard(Modifier.fillMaxWidth()) {
            LcdPanel(Modifier.fillMaxWidth()) {
                Row {
                    Text("01:42", Modifier.weight(1f), color = lookColors.lcdAccent)
                    LedChip(
                        text = null,
                        lit = shuffle,
                        onClick = { shuffle = !shuffle },
                        place = ChipPlace.DISPLAY,
                        kind = ChipKind.SWITCH,
                        icon = painterResource(R.drawable.ic_shuffle),
                        label = "Shuffle",
                        height = 30.dp,
                    )
                }
                Text("Raag Bhairavi")
            }
        }
        ListCard("252 songs", Modifier.fillMaxWidth()) {
            Text("Raga Yaman Kalyan", Modifier.padding(8.dp))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(gapless, role = Role.Switch) {
                    gapless = it
                }.padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Gapless", Modifier.weight(1f))
                LedSwitch(on = gapless)
            }
        }
    }
}
