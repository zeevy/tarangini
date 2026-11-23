/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.ui.R
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.lookColors

/**
 * The on and off mark of a settings row: a small plate with a lamp and ON or OFF. It only shows
 * the state; the row around it takes the tap and says the state to TalkBack, with
 * `Modifier.toggleable` and `Role.Switch`.
 */
@Composable
fun LedSwitch(on: Boolean, modifier: Modifier = Modifier) {
    val look = lookColors
    val accent = MaterialTheme.colorScheme.primary
    val shape = MaterialTheme.shapes.extraSmall
    Row(
        modifier = modifier
            .clearAndSetSemantics {}
            .heightIn(min = 28.dp)
            .widthIn(min = 58.dp)
            .background(if (on) accent.copy(alpha = PLATE_ALPHA) else Color.Transparent, shape)
            .border(1.dp, look.frameEdge, shape)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Lamp(lit = on, on = accent, off = look.lampOff, glow = accent.copy(alpha = GLOW_ALPHA))
        Text(
            text = stringResource(if (on) R.string.switch_on else R.string.switch_off),
            color = if (on) look.accentOnList else look.listMuted,
            fontFamily = DigitsFont,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private const val PLATE_ALPHA = 0.12f
private const val GLOW_ALPHA = 0.35f
