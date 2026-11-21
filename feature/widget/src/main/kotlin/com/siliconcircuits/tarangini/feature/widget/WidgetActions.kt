/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import androidx.media3.session.MediaLibraryService

/**
 * What the parts of the widget do on a tap.
 *
 * @property open opens the app, or null when the app has no start screen
 * @property playPause plays or pauses
 * @property next goes to the next song, or null when the playback service cannot be found
 */
internal class WidgetActions(val open: Action?, val playPause: Action, val next: Action?) {
    /** The same actions with no "next". */
    fun withoutNext(): WidgetActions = WidgetActions(open, playPause, next = null)

    companion object {
        /**
         * The actions of this app.
         *
         * Play and pause go to the media button receiver of Media3, as a headset button does. It starts
         * the playback service when the app is not running and plays what was saved last. The receiver
         * passes on only "play" keys, so "next" goes to the running playback service itself.
         */
        // The media button receiver of Media3 is marked as unstable API, but it is the one Media3 gives
        // for a press from outside the app.
        @OptIn(UnstableApi::class)
        fun of(context: Context): WidgetActions = WidgetActions(
            open = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.let { actionStartActivity(it) },
            playPause = actionSendBroadcast(
                mediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    .setComponent(ComponentName(context, MediaButtonReceiver::class.java)),
            ),
            next = playbackService(context)?.let { service ->
                val key = mediaButton(KeyEvent.KEYCODE_MEDIA_NEXT).setComponent(service)
                actionStartService(key, isForegroundService = false)
            },
        )

        /** A media button press, as a headset sends it. */
        fun mediaButton(keyCode: Int): Intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            // Both buttons send the same action. The address keeps their two waiting intents apart, so
            // one button does not take the key of the other.
            .setData(Uri.parse("tarangini-widget:key/$keyCode"))
            .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))

        /** The playback service of this app: the one service that answers the action of a media library. */
        private fun playbackService(context: Context): ComponentName? = context.packageManager
            .queryIntentServices(Intent(MediaLibraryService.SERVICE_INTERFACE).setPackage(context.packageName), 0)
            .singleOrNull()
            ?.serviceInfo
            ?.let { ComponentName(it.packageName, it.name) }
    }
}
