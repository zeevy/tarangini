/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.library.testAlbum
import com.siliconcircuits.tarangini.core.testing.library.testFolder
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.component.PlaylistPick
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.library.album.AlbumContent
import com.siliconcircuits.tarangini.feature.library.artist.ArtistContent
import com.siliconcircuits.tarangini.feature.library.favourites.FavouritesContent
import com.siliconcircuits.tarangini.feature.library.folder.FolderContent
import com.siliconcircuits.tarangini.feature.library.genre.GenreContent
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.playlist.PlaylistsContent
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/**
 * The accessibility checks of the screens that list songs under one name, and of the menus and
 * dialogs of the Library tab. A missing label, a small touch target or low contrast fails a test.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityOfListsTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private val songActions = SongRowActions(onPlay = {}, onPlayNext = {}, onAddToQueue = {})
    private val songs = PagedItems(listOf(testSong(1, "Bhairavi", artist = "Ravi"), testSong(2, "Yaman")), false)

    private fun show(screen: @Composable () -> Unit) = composeRule.setContent { TaranginiTheme { screen() } }

    private fun showLibrary(playlists: @Composable () -> Unit = {}, firstTab: LibraryTab = LibraryTab.SONGS) = show {
        LibraryContent(
            LibraryState(songs = songs),
            songActions,
            LibraryActions(),
            firstTab = firstTab,
            playlists = playlists,
        )
    }

    @Test
    fun `the Favourites tab passes the checks`() {
        show { FavouritesContent(songs, songActions, onMoreSongs = {}, onRetry = {}) }

        composeRule.checkAccessibility()
    }

    @Test
    fun `the screen of an album passes the checks`() {
        show { AlbumContent("Morning", "Ravi", songs, songActions, onMoreSongs = {}, onRetry = {}, onBack = {}) }

        composeRule.checkAccessibility()
    }

    @Test
    fun `the screen of an artist passes the checks`() {
        show {
            ArtistContent(
                name = "Ravi",
                albums = listOf(testAlbum(1, "Morning", artist = "Ravi", songCount = 2)),
                songs = songs,
                songActions = songActions,
                onMoreSongs = {},
                onRetry = {},
                onOpenAlbum = {},
                onBack = {},
            )
        }

        composeRule.checkAccessibility()
    }

    @Test
    fun `the screen of a folder passes the checks`() {
        show {
            FolderContent(
                name = "Music",
                folders = listOf(testFolder(2, "Ragas", parentId = 1)),
                songs = songs,
                songActions = songActions,
                onMoreSongs = {},
                onRetry = {},
                onOpenFolder = {},
                onBack = {},
            )
        }

        composeRule.checkAccessibility()
    }

    @Test
    fun `the screen of a genre passes the checks`() {
        show { GenreContent("Carnatic", songs, songActions, onBack = {}) }

        composeRule.checkAccessibility()
    }

    @Test
    fun `the menu of a song passes the checks`() {
        showLibrary()

        composeRule.onNodeWithContentDescription("More options for Bhairavi").performClick()

        composeRule.checkAccessibilityOfMenu()
    }

    @Test
    fun `the sort menu passes the checks`() {
        showLibrary()

        composeRule.onNodeWithContentDescription("Sort songs").performClick()

        composeRule.checkAccessibilityOfMenu()
    }

    @Test
    fun `the dialog for the name of a playlist passes the checks`() {
        showLibrary(firstTab = LibraryTab.PLAYLISTS, playlists = {
            PlaylistsContent(listOf(Playlist(1, "Drive", 2)), onOpenPlaylist = {}, onCreate = {})
        })

        composeRule.onNodeWithText("New playlist").performClick()
        // With a name the Create button is on; a button that is off is allowed to be dim.
        composeRule.onNodeWithText("Name").performTextInput("Morning")

        composeRule.checkAccessibilityOfDialog()
    }

    @Test
    fun `the Add to playlist dialog passes the checks`() {
        openAddToPlaylist()
        // With a name the Create button is on; a button that is off is allowed to be dim.
        composeRule.onNodeWithText("New playlist").performTextInput("Evening")

        composeRule.checkAccessibilityOfDialog()
    }

    @Test
    fun `the Add to playlist dialog with the largest font`() {
        RuntimeEnvironment.setFontScale(LARGEST_FONT)
        openAddToPlaylist()

        shot("library_add_to_playlist_largest_font")
    }

    @Test
    fun `the dialog for the name of a playlist with the largest font`() {
        RuntimeEnvironment.setFontScale(LARGEST_FONT)
        showLibrary(firstTab = LibraryTab.PLAYLISTS, playlists = {
            PlaylistsContent(listOf(Playlist(1, "Drive", 2)), onOpenPlaylist = {}, onCreate = {})
        })
        composeRule.onNodeWithText("New playlist").performClick()

        shot("library_playlist_name_largest_font")
    }

    private fun openAddToPlaylist() {
        val pick = PlaylistPick(
            playlists = MutableStateFlow(listOf(Playlist(1, "Drive", 2), Playlist(2, "Morning ragas", 40))),
            onAdd = { _, _ -> },
            onCreate = { _, _ -> },
        )
        show {
            val actions = SongRowActions(onPlay = {}, onPlayNext = {}, onAddToQueue = {}, playlists = pick)
            LibraryContent(LibraryState(songs = songs), actions, LibraryActions())
        }
        composeRule.onNodeWithContentDescription("More options for Bhairavi").performClick()
        composeRule.onNodeWithText("Add to playlist").performClick()
    }

    /** A picture of the whole screen, so a dialog is in it: a dialog is a window of its own. */
    private fun shot(name: String) {
        val picture = savedPicture("$name")
        // The dialog opens in a later frame than the tap.
        composeRule.waitForIdle()
        captureScreenRoboImage(picture.path)
    }
}

private const val LARGEST_FONT = 2f
