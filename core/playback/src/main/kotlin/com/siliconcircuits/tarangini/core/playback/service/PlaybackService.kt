/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.service

import android.app.PendingIntent
import android.os.Looper
import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.siliconcircuits.tarangini.core.domain.playback.ResumeStore
import com.siliconcircuits.tarangini.core.domain.repository.LibraryLetters
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.domain.repository.SearchRepository
import com.siliconcircuits.tarangini.core.domain.repository.SongLookup
import com.siliconcircuits.tarangini.core.domain.repository.StationStreams
import com.siliconcircuits.tarangini.core.playback.R
import com.siliconcircuits.tarangini.core.playback.browse.BrowseItems
import com.siliconcircuits.tarangini.core.playback.browse.BrowseTree
import com.siliconcircuits.tarangini.core.playback.browse.CarSearch
import com.siliconcircuits.tarangini.core.playback.browse.LibraryChangeNotices
import com.siliconcircuits.tarangini.core.playback.browse.browseLabels
import com.siliconcircuits.tarangini.core.playback.di.PlaybackCoroutineScope
import com.siliconcircuits.tarangini.core.playback.mapper.LibraryTexts
import com.siliconcircuits.tarangini.core.playback.player.LastSound
import com.siliconcircuits.tarangini.core.playback.player.SkipSilentEnd
import com.siliconcircuits.tarangini.core.playback.player.createPlayer
import com.siliconcircuits.tarangini.core.playback.resume.PlaybackResume
import com.siliconcircuits.tarangini.core.playback.session.CallerPolicy
import com.siliconcircuits.tarangini.core.playback.session.CarLibrary
import com.siliconcircuits.tarangini.core.playback.session.CarPlay
import com.siliconcircuits.tarangini.core.playback.session.CertificateCallers
import com.siliconcircuits.tarangini.core.playback.session.NothingToPlay
import com.siliconcircuits.tarangini.core.playback.session.PlaybackSessionCallback
import com.siliconcircuits.tarangini.core.playback.stream.StreamFailover
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * The one playback service of the app. It owns the one player and the one
 * media session. The phone UI, the notification, the lock screen, Bluetooth
 * and the car all reach playback through this session.
 */
internal class PlaybackService : MediaLibraryService() {
    private var session: MediaLibrarySession? = null
    private var resume: PlaybackResume? = null
    private var notices: LibraryChangeNotices? = null
    private var scope: CoroutineScope? = null

    /** What the service takes from the app. The system makes a service, so nothing can be handed to its constructor. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun resumeStore(): ResumeStore

        fun songLookup(): SongLookup

        fun libraryRepository(): LibraryRepository

        fun libraryLetters(): LibraryLetters

        fun searchRepository(): SearchRepository

        fun httpClient(): OkHttpClient

        fun stationStreams(): StationStreams

        @PlaybackCoroutineScope
        fun playbackScope(): CoroutineScope
    }

    override fun onCreate() {
        super.onCreate()
        val app = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        val sound = LastSound()
        val player = createPlayer(this, sound, app::httpClient)
        player.addListener(StreamFailover(player, app.stationStreams(), app.playbackScope()))
        player.addListener(SkipSilentEnd(player, sound, app.playbackScope()))
        val texts = LibraryTexts(app.songLookup(), app.stationStreams())
        val resume = PlaybackResume(player, app.resumeStore(), app.playbackScope(), texts::addTo)
        scope = app.playbackScope()
        val items = BrowseItems(browseLabels(this))
        val tree = BrowseTree(app.libraryRepository(), app.libraryLetters(), items)
        val search = CarSearch(app.searchRepository(), app.libraryRepository(), items)
        val callback = PlaybackSessionCallback(
            callers = CallerPolicy(packageName, Process.myUid(), CertificateCallers(packageManager)),
            scope = app.playbackScope(),
            car = CarLibrary(
                tree,
                search,
                CarPlay(app.playbackScope(), tree, search, app.libraryRepository(), resume::resumption),
            ),
            spokenDone = ::spokenDone,
            texts = texts::addTo,
            resumption = resume::resumption,
        )
        val builder = MediaLibrarySession.Builder(this, player, callback)
        openAppIntent()?.let(builder::setSessionActivity)
        val session = builder.build()
        this.session = session
        this.resume = resume
        resume.start()
        // Int.MAX_VALUE as the number of rows means "not known": the car reads the list again and counts itself.
        notices = LibraryChangeNotices(app.libraryRepository(), app.playbackScope(), callback.opened::all) { listId ->
            session.notifyChildrenChanged(listId, Int.MAX_VALUE, null)
        }.also(LibraryChangeNotices::start)
    }

    /**
     * Ends a spoken request. With a queue found, playback starts here, once the queue is in the player:
     * a caller that sends "play" itself would also start the old queue when nothing was found. With
     * nothing found, the caller that asked gets the reason as text, so the driver reads it and does
     * not wait in silence.
     */
    // Sending an error text through the session is API of Media3 that may still change.
    @OptIn(UnstableApi::class)
    private fun spokenDone(session: MediaSession, caller: MediaSession.ControllerInfo, missing: NothingToPlay?) {
        val text = when (missing) {
            null -> null
            NothingToPlay.NOTHING_FOUND -> R.string.car_nothing_found
            NothingToPlay.NO_SONGS -> R.string.car_no_songs
        }
        if (text == null) {
            // Started as its own piece of work, so it runs after Media3 has put the queue into the player.
            scope?.launch {
                session.player.prepare()
                session.player.play()
            }
        } else {
            session.sendError(caller, SessionError(SessionError.ERROR_BAD_VALUE, getString(text)))
        }
    }

    /** What a tap on the notification or the lock screen card opens: this app. */
    private fun openAppIntent(): PendingIntent? = packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
        PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onDestroy() {
        notices?.end()
        notices = null
        resume?.end()
        resume = null
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
