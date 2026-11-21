/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Process
import android.util.Size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlin.coroutines.cancellation.CancellationException

/**
 * The home screen widget: the cover, the title and the artist, play or pause, and next. It follows
 * the light or dark look of the phone and the colours of its wallpaper.
 *
 * What it shows is written into its saved state by [WidgetCards], which the playback service tells
 * about every change.
 */
class NowPlayingWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val actions = WidgetActions.of(context)
        provideContent {
            val saved = currentState<Preferences>()
            val card = saved.card(Process.myPid())
            // "Next" goes to the running playback service. A card of a process that has ended has no
            // service to take it, so the widget shows only play then.
            val shown = if (saved.isFromProcess(Process.myPid())) actions else actions.withoutNext()
            val song = card?.songUri
            val cover by produceState<Bitmap?>(null, song) { value = song?.let { coverOf(context, it) } }
            GlanceTheme { WidgetContent(card, cover, shown) }
        }
    }

    /**
     * The cover of the song, small. The system keeps such pictures ready. A song with no cover, or a
     * file that is gone, has none.
     */
    private fun coverOf(context: Context, songUri: String): Bitmap? = runCatching {
        context.contentResolver.loadThumbnail(Uri.parse(songUri), Size(COVER_PIXELS, COVER_PIXELS), null)
    }.onFailure { if (it is CancellationException) throw it }.getOrNull()

    internal companion object {
        /** Two cells wide: the cover and the buttons. */
        val SMALL = DpSize(110.dp, 40.dp)

        /** Wide enough for the title and the artist too. */
        val WIDE = DpSize(250.dp, 40.dp)

        /** Twice the shown size, for a sharp picture on a dense screen. */
        const val COVER_PIXELS = 192
    }
}

/** The receiver the launcher talks to. */
class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}
