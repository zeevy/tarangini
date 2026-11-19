/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.playback.service

import android.app.PendingIntent
import android.os.Process
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.siliconcircuits.tarangini.core.playback.player.createPlayer
import com.siliconcircuits.tarangini.core.playback.session.CallerPolicy
import com.siliconcircuits.tarangini.core.playback.session.PlaybackSessionCallback

/**
 * The one playback service of the app. It owns the one player and the one
 * media session. The phone UI, the notification, the lock screen, Bluetooth
 * and the car all reach playback through this session.
 */
internal class PlaybackService : MediaLibraryService() {
    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        val callback = PlaybackSessionCallback(CallerPolicy(ownPackage = packageName, ownUid = Process.myUid()))
        val builder = MediaLibrarySession.Builder(this, createPlayer(this), callback)
        openAppIntent()?.let(builder::setSessionActivity)
        session = builder.build()
    }

    /** What a tap on the notification or the lock screen card opens: this app. */
    private fun openAppIntent(): PendingIntent? = packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
        PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
