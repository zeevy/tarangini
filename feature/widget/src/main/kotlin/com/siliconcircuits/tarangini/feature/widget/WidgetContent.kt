/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.widget

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.siliconcircuits.tarangini.core.domain.playback.NowPlayingCard
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR

/**
 * What the widget shows. On a small widget the cover and the buttons, on a wide one also the title
 * and the artist. With nothing loaded the play button still plays what was saved last.
 */
@Composable
internal fun WidgetContent(card: NowPlayingCard?, cover: Bitmap?, actions: WidgetActions) {
    val context = LocalContext.current
    val isWide = LocalSize.current.width >= NowPlayingWidget.WIDE.width
    Row(
        modifier = GlanceModifier.fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(CORNER)
            .padding(PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(cover, actions.open)
        if (isWide) {
            Spacer(GlanceModifier.width(PADDING))
            Texts(card, actions.open, GlanceModifier.defaultWeight())
        } else {
            Spacer(GlanceModifier.defaultWeight())
        }
        val pause = card?.showsPause == true
        Button(
            icon = if (pause) CoreUiR.drawable.ic_pause else CoreUiR.drawable.ic_play,
            label = context.getString(if (pause) CoreUiR.string.action_pause else CoreUiR.string.action_play),
            onClick = actions.playPause,
        )
        // "Next" shows only while something is loaded and the playback service runs.
        if (card != null && actions.next != null) {
            Button(CoreUiR.drawable.ic_skip_next, context.getString(CoreUiR.string.action_next), actions.next)
        }
    }
}

@Composable
private fun Cover(cover: Bitmap?, onOpen: Action?) {
    val context = LocalContext.current
    val tap = onOpen?.let { GlanceModifier.clickable(it) } ?: GlanceModifier
    Box(
        modifier = GlanceModifier.size(COVER)
            .cornerRadius(COVER_CORNER)
            .background(GlanceTheme.colors.secondaryContainer)
            .then(tap),
        contentAlignment = Alignment.Center,
    ) {
        if (cover == null) {
            Image(
                provider = ImageProvider(CoreUiR.drawable.ic_music_note),
                contentDescription = context.getString(R.string.widget_open),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                modifier = GlanceModifier.size(ICON),
            )
        } else {
            Image(
                provider = ImageProvider(cover),
                contentDescription = context.getString(R.string.widget_open),
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Texts(card: NowPlayingCard?, onOpen: Action?, modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current
    val title = when {
        card == null -> context.getString(R.string.widget_nothing_title)
        else -> card.title ?: context.getString(CoreUiR.string.unknown_title)
    }
    val line = when {
        card == null -> context.getString(R.string.widget_nothing_text)
        else -> card.artist ?: context.getString(CoreUiR.string.unknown_artist)
    }
    Column(modifier = onOpen?.let { modifier.clickable(it) } ?: modifier) {
        Text(
            text = title,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = TITLE_SIZE,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = line,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = LINE_SIZE),
        )
    }
}

/** A button of the widget: 48 dp to tap, as every touch target of the app, with a 24 dp icon in its middle. */
@Composable
private fun Button(icon: Int, label: String, onClick: Action) {
    Image(
        provider = ImageProvider(icon),
        contentDescription = label,
        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
        modifier = GlanceModifier.size(TOUCH).padding((TOUCH - ICON) / 2).clickable(onClick),
    )
}

private val PADDING = 8.dp
private val CORNER = 16.dp
private val COVER = 48.dp
private val COVER_CORNER = 8.dp
private val ICON = 24.dp
private val TOUCH = 48.dp
private val TITLE_SIZE = 14.sp
private val LINE_SIZE = 12.sp
