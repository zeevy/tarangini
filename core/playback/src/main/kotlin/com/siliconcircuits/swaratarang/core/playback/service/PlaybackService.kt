/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.service

import android.os.Process
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.siliconcircuits.swaratarang.core.playback.player.createPlayer
import com.siliconcircuits.swaratarang.core.playback.session.CallerPolicy
import com.siliconcircuits.swaratarang.core.playback.session.PlaybackSessionCallback

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
        session = MediaLibrarySession.Builder(this, createPlayer(this), callback).build()
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
