/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.model.PlaylistSong
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.testing.library.testAlbum
import com.siliconcircuits.tarangini.core.testing.library.testArtist
import com.siliconcircuits.tarangini.core.testing.library.testFolder
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.library.album.AlbumContent
import com.siliconcircuits.tarangini.feature.library.artist.ArtistContent
import com.siliconcircuits.tarangini.feature.library.folder.FolderContent
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.playlist.PlaylistActions
import com.siliconcircuits.tarangini.feature.library.playlist.PlaylistContent
import com.siliconcircuits.tarangini.feature.library.playlist.PlaylistsContent
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Every screen of the Library tab at the narrowest width. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class LibraryScreenshotTest {
    private val songActions = SongRowActions({}, {}, {})
    private val songs = PagedItems(
        items = listOf(
            testSong(1, "Bhairavi", artist = "Ravi Shankar"),
            testSong(
                2,
                "A very long title of a song that does not fit on one line",
                artist = "An artist with a long name",
            ),
            testSong(3, "Yaman", artist = "", durationMs = 3_723_000),
            testSong(4, "ఎందరో మహానుభావులు", artist = "త్యాగరాజు", durationMs = 0),
        ),
        isLoading = false,
    )
    private val albums = listOf(
        testAlbum(1, "Morning Ragas", artist = "Ravi Shankar", songCount = 8),
        testAlbum(2, "", artist = "", songCount = 1),
    )
    private val lists = LibraryState(
        songs = songs,
        albums = PagedItems(albums, isLoading = false),
        artists = PagedItems(listOf(testArtist(1, "Ravi Shankar"), testArtist(2, "", songCount = 1)), false),
        folders = listOf(testFolder(1, "Download"), testFolder(2, "Music")),
    )

    @Test
    fun songs() = shot("library_songs") { LibraryContent(lists, songActions, LibraryActions()) }

    @Test
    fun albums() = shot("library_albums") {
        LibraryContent(lists, songActions, LibraryActions(), firstTab = LibraryTab.ALBUMS)
    }

    @Test
    fun artists() = shot("library_artists") {
        LibraryContent(lists, songActions, LibraryActions(), firstTab = LibraryTab.ARTISTS)
    }

    @Test
    fun folders() = shot("library_folders") {
        LibraryContent(lists, songActions, LibraryActions(), firstTab = LibraryTab.FOLDERS)
    }

    @Test
    fun `an empty library`() = shot("library_empty") {
        val empty = LibraryState(songs = PagedItems(emptyList<Song>(), isLoading = false), folders = emptyList())
        LibraryContent(empty, songActions, LibraryActions())
    }

    @Test
    fun album() = shot("library_album") {
        AlbumContent("Morning Ragas", "Ravi Shankar", songs, songActions, onMoreSongs = {}, onRetry = {}, onBack = {})
    }

    @Test
    fun artist() = shot("library_artist") {
        ArtistContent("Ravi Shankar", albums, songs, songActions, onMoreSongs = {
        }, onRetry = {}, onOpenAlbum = {}, onBack = {})
    }

    @Test
    fun folder() = shot("library_folder") {
        FolderContent(
            name = "Music",
            folders = listOf(testFolder(3, "Ragas", parentId = 2)),
            songs = songs,
            songActions = songActions,
            onMoreSongs = {},
            onRetry = {},
            onOpenFolder = {},
            onBack = {},
        )
    }

    @Test
    fun `a folder with nothing left`() = shot("library_folder_empty") {
        FolderContent("Old", emptyList(), PagedItems(isLoading = false), songActions, {}, {}, {}, {})
    }

    @Test
    fun playlists() = shot("library_playlists") {
        LibraryContent(lists, songActions, LibraryActions(), firstTab = LibraryTab.PLAYLISTS) {
            PlaylistsContent(
                listOf(
                    Playlist(1, "Drive", 12),
                    Playlist(2, "A playlist with a name that does not fit on one line", 1),
                ),
                onOpenPlaylist = {},
                onCreate = {},
            )
        }
    }

    @Test
    fun playlist() = shot("library_playlist") {
        val places = PagedItems(songs.items.mapIndexed { index, song -> PlaylistSong(index + 1L, song) }, false)
        PlaylistContent("Drive", places, PlaylistActions(), onBack = {})
    }

    @Test
    fun `a playlist with no songs`() = shot("library_playlist_empty") {
        PlaylistContent("Drive", PagedItems(isLoading = false), PlaylistActions(), onBack = {})
    }

    private fun shot(name: String, screen: @Composable () -> Unit) {
        val picture = File("src/test/screenshots/$name.png")
        // Without this, a test with no saved picture passes: there is nothing to compare with.
        assertTrue(
            "No saved picture $picture. Run the tests with -PrecordScreenshots.",
            recording || picture.exists(),
        )
        captureRoboImage(picture.path) {
            TaranginiTheme { screen() }
        }
    }
}

private val recording = System.getProperty("roborazzi.test.record").toBoolean()
