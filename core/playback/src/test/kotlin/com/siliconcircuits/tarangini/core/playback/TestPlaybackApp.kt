/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback

import android.app.Application
import com.siliconcircuits.tarangini.core.domain.playback.ResumeStore
import com.siliconcircuits.tarangini.core.domain.premium.CrashReporter
import com.siliconcircuits.tarangini.core.domain.repository.AudioAccess
import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import com.siliconcircuits.tarangini.core.domain.repository.GenreRepository
import com.siliconcircuits.tarangini.core.domain.repository.LibraryLetters
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.domain.repository.PlayHistory
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import com.siliconcircuits.tarangini.core.domain.repository.SearchRepository
import com.siliconcircuits.tarangini.core.domain.repository.SongLookup
import com.siliconcircuits.tarangini.core.domain.repository.StationStreams
import com.siliconcircuits.tarangini.core.playback.service.PlaybackService
import com.siliconcircuits.tarangini.core.playback.sleep.DefaultSleepTimer
import com.siliconcircuits.tarangini.core.testing.library.FakeFavouriteRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeGenreRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryLetters
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.library.FakePlayHistory
import com.siliconcircuits.tarangini.core.testing.library.FakePlaylistRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeSearchRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeSongLookup
import com.siliconcircuits.tarangini.core.testing.playback.FakeResumeStore
import com.siliconcircuits.tarangini.core.testing.premium.FakeCrashReporter
import com.siliconcircuits.tarangini.core.testing.station.FakeStationStreams
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import okhttp3.OkHttpClient

/**
 * The application of a test that starts the playback service. It hands the
 * service what the real app hands it through Hilt, with a store a test can
 * fill and read.
 */
internal class TestPlaybackApp :
    Application(),
    GeneratedComponentManager<Any> {
    val resumeStore = FakeResumeStore()
    val songs = FakeSongLookup()
    val library = FakeLibraryRepository()
    val search = FakeSearchRepository()
    val favourites = FakeFavouriteRepository()
    val history = FakePlayHistory()
    val playlists = FakePlaylistRepository()
    var audioAllowed = true
    private val scope = MainScope()
    private val sleepTimer = DefaultSleepTimer(scope)

    override fun generatedComponent(): Any = object : PlaybackService.Dependencies, GeneratedComponent {
        override fun resumeStore(): ResumeStore = resumeStore

        override fun songLookup(): SongLookup = songs

        override fun libraryRepository(): LibraryRepository = library

        override fun libraryLetters(): LibraryLetters = FakeLibraryLetters(library)

        override fun searchRepository(): SearchRepository = search

        override fun favouriteRepository(): FavouriteRepository = favourites

        override fun playHistory(): PlayHistory = history

        override fun playlistRepository(): PlaylistRepository = playlists

        override fun genreRepository(): GenreRepository = FakeGenreRepository()

        override fun audioAccess(): AudioAccess = AudioAccess { audioAllowed }

        override fun httpClient(): OkHttpClient = OkHttpClient()

        override fun stationStreams(): StationStreams = FakeStationStreams()

        override fun crashReporter(): CrashReporter = FakeCrashReporter()

        override fun playbackScope(): CoroutineScope = scope

        override fun sleepTimer(): DefaultSleepTimer = sleepTimer
    }
}
