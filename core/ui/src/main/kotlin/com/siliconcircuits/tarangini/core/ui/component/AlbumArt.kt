/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.siliconcircuits.tarangini.core.ui.R

/**
 * The cover picture of a song, as a square with round corners.
 * While the picture loads, and when there is none, it shows a music note on
 * a plain tile, so a row looks the same with and without a picture.
 *
 * The picture is decoration next to the title and is not read out. It is
 * loaded in the size of the square, so a list row never holds a large
 * picture in memory.
 *
 * @param artworkUri where the picture is asked for, or null to show the tile only
 * @param size the side of the square. Use [AlbumArtSize]
 */
@Composable
fun AlbumArt(artworkUri: String?, modifier: Modifier = Modifier, size: Dp = AlbumArtSize.List) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music_note),
            contentDescription = null,
            modifier = Modifier.size(size / 2),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (artworkUri != null) {
            // The picture lies over the note. Until it is there, and when it fails, it draws nothing.
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
    /** In a row of a list and in the mini player. */
    val List = 48.dp
}
