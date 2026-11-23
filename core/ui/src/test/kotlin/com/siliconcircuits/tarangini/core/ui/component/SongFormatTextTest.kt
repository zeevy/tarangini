/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.siliconcircuits.tarangini.core.domain.model.SongFormat
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SongFormatTextTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    // A test can show content once, so all the formats of a test are written in one go.
    private fun textsOf(vararg formats: SongFormat): List<String> {
        var texts = emptyList<String>()
        composeRule.setContent { texts = formats.map { songFormatText(it) } }
        composeRule.waitForIdle()
        return texts
    }

    private fun textOf(format: SongFormat): String = textsOf(format).single()

    @Test
    fun `a full format reads as one line`() {
        assertEquals("MP3 · 320 kbps · 44.1 kHz · Stereo", textOf(SongFormat("MP3", 320, 44_100, 2)))
    }

    @Test
    fun `a whole kHz has no decimal, and other channel counts get their names`() {
        val texts = textsOf(
            SongFormat("AAC", null, 48_000, 1),
            SongFormat("FLAC", sampleRateHz = 96_000, channels = 6),
            SongFormat(channels = 4),
        )

        assertEquals(listOf("AAC · 48 kHz · Mono", "FLAC · 96 kHz · 5.1", "4 channels"), texts)
    }

    @Test
    fun `a sample rate gets one decimal at most`() {
        assertEquals("22.1 kHz", textOf(SongFormat(sampleRateHz = 22_050)))
    }

    @Test
    fun `a format that tells nothing gives an empty text`() {
        assertEquals("", textOf(SongFormat()))
    }
}
