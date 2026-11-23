/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.ui.theme.lookColors

/** Where a chip lies, which sets its colours. */
enum class ChipPlace {
    /** On the ground or a framed card: a lit chip is a small white card with accent text. */
    FRAME,

    /** On the dark display: a lit chip glows in the display accent. */
    DISPLAY,
}

/** What a tap on a chip does, for TalkBack and for the switch access of the phone. */
enum class ChipKind {
    /** Picks one of a row of chips, such as the lists of the library. */
    TAB,

    /** Turns something on or off, such as shuffle. */
    SWITCH,

    /** Runs an action, such as the next repeat mode; the label says the state. */
    BUTTON,
}

/**
 * A chip with a lamp, as on the player and the library: the lamp lights while [lit] is true.
 * The touch area is always at least 48 by 48 dp; [height] is the part that is seen.
 *
 * @param text the words on the chip, or null for an icon only chip, which then needs [label]
 * @param label what TalkBack says in place of the [text], such as "Repeat: all songs"
 */
@Composable
fun LedChip(
    text: String?,
    lit: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    place: ChipPlace = ChipPlace.FRAME,
    kind: ChipKind = ChipKind.TAB,
    icon: Painter? = null,
    label: String? = null,
    height: Dp = ChipHeight,
    trailing: @Composable () -> Unit = {},
) {
    require(text != null || label != null) { "A chip needs a text or a label for TalkBack" }
    val colours = chipColours(place, lit)
    val shape = MaterialTheme.shapes.small
    val tap = when (kind) {
        ChipKind.TAB -> Modifier.selectable(selected = lit, role = Role.Tab, onClick = onClick)
        ChipKind.SWITCH -> Modifier.toggleable(value = lit, role = Role.Switch) { onClick() }
        ChipKind.BUTTON -> Modifier.clickable(role = Role.Button, onClick = onClick)
    }
    Box(
        modifier = modifier
            .heightIn(min = TouchHeight)
            .widthIn(min = TouchHeight)
            .then(tap)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = height)
                .widthIn(min = height)
                .background(colours.background, shape)
                .border(1.dp, colours.edge, shape)
                .padding(horizontal = if (text == null) 6.dp else 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Lamp(lit = lit, on = colours.lamp, off = colours.lampOff, glow = colours.glow)
            icon?.let {
                // The chip itself says what it is, so the icon says nothing of its own.
                Icon(it, contentDescription = null, Modifier.size(16.dp), colours.text)
            }
            text?.let {
                Text(
                    text = it,
                    color = colours.text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    // A label speaks in place of the text, so the text is not read a second time.
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .then(if (label != null) Modifier.clearAndSetSemantics {} else Modifier),
                )
            }
            trailing()
        }
    }
}

private class ChipColours(
    val background: Color,
    val edge: Color,
    val text: Color,
    val lamp: Color,
    val lampOff: Color,
    val glow: Color,
)

@Composable
private fun chipColours(place: ChipPlace, lit: Boolean): ChipColours {
    val look = lookColors
    return when (place) {
        ChipPlace.FRAME -> ChipColours(
            background = if (lit) look.list else Color.Transparent,
            edge = if (lit) look.frameEdge else Color.Transparent,
            text = if (lit) look.accentOnList else MaterialTheme.colorScheme.onSurfaceVariant,
            lamp = MaterialTheme.colorScheme.primary,
            lampOff = look.lampOff,
            glow = MaterialTheme.colorScheme.primary.copy(alpha = GLOW_ALPHA),
        )

        ChipPlace.DISPLAY -> ChipColours(
            background = if (lit) look.lcdTag else Color.Transparent,
            edge = look.lcdEdge,
            text = if (lit) look.lcdAccent else look.lcdMuted,
            lamp = look.lcdAccent,
            lampOff = look.lcdEdge,
            glow = look.glow,
        )
    }
}

/** The seen height of a chip on the ground, as in the library. A chip on the display is 30 dp. */
val ChipHeight = 34.dp

private val TouchHeight = 48.dp
private const val GLOW_ALPHA = 0.35f
