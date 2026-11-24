/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.domain.model.Look
import com.siliconcircuits.tarangini.core.ui.component.Lamp
import com.siliconcircuits.tarangini.core.ui.component.LedSwitch
import com.siliconcircuits.tarangini.core.ui.component.OpenArrow
import com.siliconcircuits.tarangini.core.ui.component.SectionTitle
import com.siliconcircuits.tarangini.core.ui.component.rowTextLines
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.lookColors

/** The title of a group of settings, in small capitals. */
@Composable
internal fun SettingsGroupTitle(text: String) {
    SectionTitle(text, Modifier.padding(start = 6.dp, top = 8.dp))
}

/**
 * One setting: its name, a short [note] under it and its [value] at the end. A row that opens
 * something has an arrow. A row with no [onClick] only shows something.
 *
 * @param isFirst true for the first row of a group, which has no line above it
 * @param isDanger true for a row that removes something, such as Clear play history: its name is
 *   in the danger colour and it has no arrow
 */
@Composable
internal fun SettingsRow(
    title: String,
    note: String?,
    onClick: (() -> Unit)?,
    value: String? = null,
    isFirst: Boolean = false,
    isDanger: Boolean = false,
) {
    val tap = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    RowFrame(title, note, isFirst, isDanger, tap, value) {
        if (onClick != null && !isDanger) OpenArrow()
    }
}

/** A setting that is on or off. The whole row takes the tap and TalkBack hears it as a switch. */
@Composable
internal fun SettingsSwitchRow(
    title: String,
    note: String,
    on: Boolean,
    onChange: (Boolean) -> Unit,
    isFirst: Boolean = false,
) {
    val tap = Modifier.toggleable(value = on, role = Role.Switch, onValueChange = onChange)
    RowFrame(title, note, isFirst, isDanger = false, modifier = tap) {
        LedSwitch(on, Modifier.padding(end = 6.dp))
    }
}

@Composable
private fun RowFrame(
    title: String,
    note: String?,
    isFirst: Boolean,
    isDanger: Boolean,
    modifier: Modifier = Modifier,
    value: String? = null,
    end: @Composable RowScope.() -> Unit,
) {
    val look = lookColors
    // With a large font a value at the end leaves the name no room, so it goes under the name.
    val valueBelow = rowTextLines() > 1
    Column(modifier) {
        if (!isFirst) HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = look.frameEdge)
        Row(
            modifier = Modifier.heightIn(min = 56.dp).padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = if (isDanger) look.danger else look.listText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                note?.let {
                    Text(it, color = look.listMuted, style = MaterialTheme.typography.bodySmall)
                }
                if (valueBelow) value?.let { ValueText(it) }
            }
            if (!valueBelow) value?.let { ValueText(it) }
            end()
        }
    }
}

@Composable
private fun ValueText(text: String) {
    Text(
        text = text,
        color = lookColors.listMuted,
        fontFamily = DigitsFont,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelMedium,
    )
}

/**
 * The three looks side by side in a well, as on the switch of a hardware player: the one that is
 * set is raised and its lamp is lit. TalkBack hears a group of radio buttons.
 */
@Composable
internal fun LookPicker(look: Look, onPick: (Look) -> Unit) {
    val colours = lookColors
    val shape = MaterialTheme.shapes.small
    val well = Modifier
        .padding(horizontal = 10.dp, vertical = 4.dp)
        .background(MaterialTheme.colorScheme.surfaceContainer, shape)
        .border(1.dp, colours.frameEdge, shape)
        .padding(3.dp)
        .selectableGroup()
    // With a large font three names do not fit side by side, so they go one under another.
    if (rowTextLines() > 1) {
        Column(well.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Look.entries.forEach { choice ->
                LookChoice(choice, picked = choice == look, onPick = { onPick(choice) }, Modifier.fillMaxWidth())
            }
        }
    } else {
        Row(well, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Look.entries.forEach { choice ->
                LookChoice(choice, picked = choice == look, onPick = { onPick(choice) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LookChoice(choice: Look, picked: Boolean, onPick: () -> Unit, modifier: Modifier = Modifier) {
    val colours = lookColors
    val accent = MaterialTheme.colorScheme.primary
    val shape = MaterialTheme.shapes.extraSmall
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (picked) Modifier.shadow(2.dp, shape).background(colours.list, shape) else Modifier)
            .selectable(selected = picked, role = Role.RadioButton, onClick = onPick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Lamp(lit = picked, on = accent, off = colours.lampOff, glow = accent.copy(alpha = GLOW_ALPHA))
        Text(
            text = lookLabel(choice),
            color = if (picked) colours.accentOnList else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            // "Follow phone" goes on two lines on a narrow phone rather than lose a word.
            textAlign = TextAlign.Center,
        )
    }
}

private const val GLOW_ALPHA = 0.35f
