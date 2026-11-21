/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

/** The drag of a row, on a real list with rows of 100 pixels. */
@RunWith(RobolectricTestRunner::class)
class ReorderStateTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private var rows by mutableStateOf(listOf("a", "b", "c", "d"))
    private val list = LazyListState()
    private val reorder =
        ReorderState(list) { from, to -> rows = rows.toMutableList().apply { add(to, removeAt(from)) } }

    private fun showList() {
        composeRule.setContent {
            val rowHeight = with(LocalDensity.current) { ROW_PX.toDp() }
            LazyColumn(state = list) {
                items(rows, key = { it }) { Text(it, Modifier.height(rowHeight)) }
            }
        }
        composeRule.waitForIdle()
    }

    private fun drag(pixels: Float) {
        composeRule.runOnUiThread { reorder.dragBy(pixels) }
        composeRule.waitForIdle()
    }

    @Test
    fun `a row dragged past the middle of the next one changes places with it and stays under the finger`() {
        showList()
        composeRule.runOnUiThread { reorder.start("a") }

        drag(40f)
        assertEquals(listOf("a", "b", "c", "d"), rows)

        drag(20f)
        assertEquals(listOf("b", "a", "c", "d"), rows)
        // The finger is 60 pixels from the start. The row now has its place 100 pixels down, so it is drawn 40 up.
        assertEquals(-40f, reorder.offset)
    }

    @Test
    fun `a long drag moves the row place by place, down and up again`() {
        showList()
        composeRule.runOnUiThread { reorder.start("a") }

        repeat(5) { drag(50f) }
        assertEquals(listOf("b", "c", "a", "d"), rows)

        repeat(5) { drag(-50f) }
        assertEquals(listOf("a", "b", "c", "d"), rows)
    }

    @Test
    fun `the end of a drag puts the row back in line`() {
        showList()
        composeRule.runOnUiThread { reorder.start("b") }
        drag(30f)

        composeRule.runOnUiThread { reorder.end() }

        assertNull(reorder.draggedKey)
        assertEquals(0f, reorder.offset)
    }

    private companion object {
        const val ROW_PX = 100
    }
}
