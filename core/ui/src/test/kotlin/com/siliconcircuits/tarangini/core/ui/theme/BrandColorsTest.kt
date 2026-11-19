/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandColorsTest {
    @Test
    fun `text on every light brand colour meets WCAG AA contrast`() {
        assertReadable(BrandLightColors)
    }

    @Test
    fun `text on every dark brand colour meets WCAG AA contrast`() {
        assertReadable(BrandDarkColors)
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
        const val LUMINANCE_OFFSET = 0.05f
    }
}
