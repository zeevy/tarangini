/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont

/**
 * The cover picture of a song or an album, as a square with round corners.
 * While the picture loads, and when there is none, it shows a colour tile with
 * the first letters of [name], so a row looks finished with or without a picture.
 * The colour comes from the name, so an album has the same tile everywhere.
 *
 * The picture is decoration next to the title and is not read out. It is
 * loaded in the size of the square, so a list row never holds a large
 * picture in memory.
 *
 * @param artworkUri where the picture is asked for, or null to show the tile only
 * @param size the side of the square, from [AlbumArtSize]; [Dp.Unspecified] lets [modifier] set it
 * @param name the album name, for the colour and the letters of the tile
 */
@Composable
fun AlbumArt(artworkUri: String?, modifier: Modifier = Modifier, size: Dp = AlbumArtSize.List, name: String = "") {
    val tones = coverTones(name)
    BoxWithConstraints(
        modifier = modifier
            .then(if (size == Dp.Unspecified) Modifier else Modifier.size(size))
            .clip(MaterialTheme.shapes.small)
            .background(Brush.linearGradient(tones))
            // The letters only stand in for a picture; the row says the name already.
            .clearAndSetSemantics {},
    ) {
        val small = maxWidth < LargeTile
        Text(
            text = initialsOf(name).ifEmpty { "♪" },
            color = Color.White,
            fontFamily = DigitsFont,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(
                fontSize = if (small) 12.sp else 22.sp,
                shadow = Shadow(Color.Black.copy(alpha = SHADE), Offset(0f, 1f), blurRadius = 3f),
            ),
            modifier = if (small) {
                Modifier.align(
                    Alignment.Center,
                )
            } else {
                Modifier.align(Alignment.BottomStart).padding(10.dp)
            },
        )
        if (artworkUri != null) {
            // The picture lies over the tile. Until it is there, and when it fails, it draws nothing.
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** The sizes a cover picture is shown in. A screen that needs another size adds it here. */
object AlbumArtSize {
    /** In a row of a list. */
    val List = TileSize

    /** In the header of the album screen. */
    val Header = 104.dp
}

// Two colours of one tone for each tile. They hold white letters at 3:1 or more.
private val CoverTones = listOf(
    listOf(Color(0xFF0D7050), Color(0xFF38B984)),
    listOf(Color(0xFF6B3790), Color(0xFFB57AD9)),
    listOf(Color(0xFFA64A16), Color(0xFFE0A23A)),
    listOf(Color(0xFF1F5F9E), Color(0xFF5DA9E9)),
    listOf(Color(0xFF8E2A3C), Color(0xFFE06A78)),
    listOf(Color(0xFF3E5A2B), Color(0xFF8DBA52)),
)

private fun coverTones(name: String): List<Color> = CoverTones[Math.floorMod(name.hashCode(), CoverTones.size)]

private val LargeTile = 64.dp
private const val SHADE = 0.35f
