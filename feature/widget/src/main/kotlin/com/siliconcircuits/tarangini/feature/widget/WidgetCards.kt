/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.widget

import android.content.Context
import android.os.Process
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.siliconcircuits.tarangini.core.domain.playback.NowPlayingCard
import com.siliconcircuits.tarangini.core.domain.playback.NowPlayingCards
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/**
 * Writes what plays into every widget on the home screen and draws them again. With no widget
 * placed it does nothing.
 */
internal class WidgetCards @Inject constructor(@param:ApplicationContext private val context: Context) :
    NowPlayingCards {
    override suspend fun show(card: NowPlayingCard?) {
        val widget = NowPlayingWidget()
        GlanceAppWidgetManager(context).getGlanceIds(NowPlayingWidget::class.java).forEach { id ->
            updateAppWidgetState(context, id) { saved -> saved.write(card, Process.myPid()) }
            widget.update(context, id)
        }
    }
}

/** Adds the widget to the views the playback service tells about each change. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class WidgetModule {
    @Binds
    @IntoSet
    abstract fun widgetCards(cards: WidgetCards): NowPlayingCards
}
