/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorsTest {
    @Test
    fun `text on every colour of the light look meets WCAG AA contrast`() {
        assertReadable(LightColors)
    }

    @Test
    fun `text on every colour of the dark look meets WCAG AA contrast`() {
        assertReadable(DarkColors)
    }

    @Test
    fun `text on the display, the lists and the snackbar of the light look meets WCAG AA contrast`() {
        assertReadable(LightLook, LightColors)
    }

    @Test
    fun `text on the display, the lists and the snackbar of the dark look meets WCAG AA contrast`() {
        assertReadable(DarkLook, DarkColors)
    }

    @Test
    fun `lamps and icons that are lit stand out from what is behind them`() {
        listOf(LightLook to LightColors, DarkLook to DarkColors).forEach { (look, scheme) ->
            mapOf(
                "a lit lamp on the display" to (look.lcdAccent to look.lcd),
                "the heart on the display" to (look.heart to look.lcd),
                "the heart on a list" to (look.heart to look.list),
                "the accent pill of the current tab" to (scheme.primary to scheme.surfaceContainer),
            ).forEach { (name, colours) ->
                val ratio = contrast(colours.first, colours.second)
                assertTrue("$name has contrast $ratio, below $NON_TEXT", ratio >= NON_TEXT)
            }
        }
    }

    private fun assertReadable(look: LookColors, scheme: ColorScheme) {
        val pairs = with(look) {
            mapOf(
                "text on the display" to (lcdText to lcd),
                "muted text on the display" to (lcdMuted to lcd),
                "accent text on the display" to (lcdAccent to lcd),
                "accent text on a lit tag" to (lcdAccent to lcdTag.compositeOver(lcd)),
                "an error line on the display" to (warn to lcd),
                "text on a list" to (listText to list),
                "muted text on a list" to (listMuted to list),
                "the song that plays on a list" to (accentOnList to rowOn),
                "muted text on the row that plays" to (listMuted to rowOn),
                "a delete action on a card" to (danger to scheme.surfaceContainer),
                "snackbar text" to (onSnack to snack),
                "snackbar button" to (snackAction to snack),
                "muted text on a card" to (scheme.onSurfaceVariant to scheme.surfaceContainer),
                "muted text on the ground" to (scheme.onSurfaceVariant to scheme.background),
            )
        }
        pairs.forEach { (name, colours) ->
            val ratio = contrast(colours.first, colours.second)
            assertTrue("$name has contrast $ratio, below $WCAG_AA", ratio >= WCAG_AA)
        }
    }

    private fun assertReadable(scheme: ColorScheme) {
        val pairs = with(scheme) {
            mapOf(
                "onPrimary" to (onPrimary to primary),
                "onPrimaryContainer" to (onPrimaryContainer to primaryContainer),
                "onSecondary" to (onSecondary to secondary),
                "onSecondaryContainer" to (onSecondaryContainer to secondaryContainer),
                "onTertiary" to (onTertiary to tertiary),
                "onTertiaryContainer" to (onTertiaryContainer to tertiaryContainer),
                "onBackground" to (onBackground to background),
                "onSurface" to (onSurface to surface),
                "onSurface on the bottom bar" to (onSurface to surfaceContainer),
                "onSurfaceVariant" to (onSurfaceVariant to surface),
                "onSurfaceVariant on the bottom bar" to (onSurfaceVariant to surfaceContainer),
                "onSurfaceVariant on the mini player" to (onSurfaceVariant to surfaceContainerHighest),
                "primary as text" to (primary to surface),
                "primary as text on a card" to (primary to surfaceContainer),
                "primary as text on a dialog" to (primary to surfaceContainerHigh),
                "primary as text on the selected tab" to (primary to secondaryContainer),
                "tertiary as text on a card" to (tertiary to surfaceContainer),
                "inverseOnSurface" to (inverseOnSurface to inverseSurface),
                "onError" to (onError to error),
                "onErrorContainer" to (onErrorContainer to errorContainer),
            )
        }
        pairs.forEach { (name, colours) ->
            val ratio = contrast(colours.first, colours.second)
            assertTrue("$name has contrast $ratio, below $WCAG_AA", ratio >= WCAG_AA)
        }
    }

    private fun contrast(text: Color, background: Color): Float {
        val lighter = maxOf(text.luminance(), background.luminance())
        val darker = minOf(text.luminance(), background.luminance())
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    private companion object {
        const val WCAG_AA = 4.5f

        // WCAG 1.4.11: icons and other parts that are not text.
        const val NON_TEXT = 3f
        const val LUMINANCE_OFFSET = 0.05f
    }
}
