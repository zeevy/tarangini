/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini

import android.Manifest
import android.content.ComponentName
import android.os.Build
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import java.util.Collections
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * The main flow of the real app on a device: the scan finds new audio files, a song plays, it
 * becomes a favourite and goes into a new playlist. Each step is checked on the screen and in the
 * media session, the way the car and the notification see it.
 *
 * Run it on an emulator or a test phone: it adds three short files to the music of the device
 * (and removes them again), and leaves a favourite and a playlist in the app.
 */
@RunWith(AndroidJUnit4::class)
class MainFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext
    private val files = TestAudioFiles(app, instrumentation.context.assets)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    // The permission and the files must be in place before the app starts and scans.
    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(GrantPermissionRule.grant(audioPermission()))
        .around(files)
        .around(composeRule)

    private var browser: MediaBrowser? = null
    private val played = Collections.synchronizedSet(mutableSetOf<String>())

    @After
    fun closeBrowser() {
        browser?.let { instrumentation.runOnMainSync(it::release) }
    }

    @Test
    fun aSongPlaysBecomesAFavouriteAndGoesIntoANewPlaylist() {
        val session = connect()
        val titles = files.titles
        val playlist = "Test list ${files.run}"

        // The scan: the new songs show in the songs list of the Library tab.
        tab("Library")
        waitFor("the scan to list ${titles.first()}") { scrollTo(titles.first()) }

        // Play: a tap plays the song, and the session tells it as heard.
        composeRule.onAllNodesWithText(titles.first()).onFirst().performClick()
        waitFor("the session to play ${titles.first()}") { titles.first() in played }
        // The songs are short; a pause keeps one of them as the current song for the next steps.
        onMain { session.pause() }
        val current = onMain { session.currentMediaItem?.mediaMetadata?.title?.toString() }
        assertTrue("The session plays one of the new songs, not $current", current in titles)
        val song = requireNotNull(current)
        // The mini player names the song, as well as its row in the list. It is one button, so
        // TalkBack reads its name and not the texts inside.
        waitFor("the mini player to show $song") {
            composeRule.onAllNodes(hasContentDescription("$song,", substring = true)).fetchNodes().isNotEmpty()
        }

        // Favourite: the row gets the heart, the session the filled heart and a Favourites entry.
        menuOf(song, "Add to favourites")
        waitFor("the heart on the row of $song") {
            composeRule.onAllNodesWithContentDescription("Favourite", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        waitFor("the filled heart in the session") {
            onMain { session.mediaButtonPreferences.any { it.icon == CommandButton.ICON_HEART_FILLED } }
        }
        waitFor("$song in the Favourites list of the session") { song in childTitles(session, "favourites") }

        // Playlist: a new playlist with the song, on the screen and in the session.
        menuOf(song, "Add to playlist")
        composeRule.onNodeWithText("New playlist").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(playlist)
        composeRule.onNodeWithText("Create").performClick()
        waitFor("$playlist in the Playlists list of the session") { playlist in childTitles(session, "playlists") }
        waitFor("$song in $playlist in the session") { song in playlistTitles(session, playlist) }

        // Up next: the mini player opens the player tab, where a tap on a row plays that song.
        onMain { session.seekToDefaultPosition(0) }
        val second = onMain { session.getMediaItemAt(1).mediaMetadata.title?.toString() }
        composeRule.onNode(hasContentDescription("Open the player", substring = true)).performClick()
        val row = hasContentDescription("Number 2, $second", substring = true)
        // The player card scrolls too, so each list that scrolls is tried until one holds the row.
        waitFor("the up next row of $second") {
            val lists = composeRule.onAllNodes(hasScrollToNodeAction())
            lists.fetchNodes().indices.any { place ->
                try {
                    lists[place].performScrollToNode(row)
                    true
                } catch (_: AssertionError) {
                    false
                }
            }
        }
        composeRule.onNode(row).performClick()
        waitFor("the session to move to $second") { onMain { session.currentMediaItemIndex } == 1 }
    }

    /** The permission that reads audio files on this Android version. */
    private fun audioPermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    /** A browser of the playback service, as the car has. It notes each song that is heard. */
    private fun connect(): MediaBrowser {
        val token = SessionToken(app, ComponentName(app, PLAYBACK_SERVICE))
        val connected = MediaBrowser.Builder(app, token).buildAsync().get(CONNECT_SECONDS, TimeUnit.SECONDS)
        browser = connected
        instrumentation.runOnMainSync {
            connected.addListener(
                object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        val title = player.currentMediaItem?.mediaMetadata?.title?.toString()
                        if (player.isPlaying && title != null) played += title
                    }
                },
            )
        }
        onMain { connected.getLibraryRoot(null) }.get(CONNECT_SECONDS, TimeUnit.SECONDS)
        return connected
    }

    /** The titles of the list [parentId] of the session, read again on each call. */
    private fun childTitles(session: MediaBrowser, parentId: String): List<String> {
        val result = onMain { session.getChildren(parentId, 0, LIST_SIZE, null) }
            .get(CONNECT_SECONDS, TimeUnit.SECONDS)
        return result.value.orEmpty().mapNotNull { it.mediaMetadata.title?.toString() }
    }

    /** The song titles of the playlist named [name], read through the car list of the session. */
    private fun playlistTitles(session: MediaBrowser, name: String): List<String> {
        val result = onMain { session.getChildren("playlists", 0, LIST_SIZE, null) }
            .get(CONNECT_SECONDS, TimeUnit.SECONDS)
        val id = result.value.orEmpty().firstOrNull { it.mediaMetadata.title?.toString() == name }?.mediaId
        return id?.let { childTitles(session, it) }.orEmpty()
    }

    /**
     * Scrolls the list on the screen to the row with [text]; false while there is no such row. A
     * device may hold other music, so the row can be below the fold, where a list builds no rows.
     */
    private fun scrollTo(text: String): Boolean = try {
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text))
        true
    } catch (_: AssertionError) {
        false
    }

    /** Opens the bottom bar tab [name]. */
    private fun tab(name: String) {
        composeRule.onAllNodes(hasText(name) and hasClickAction()).onFirst().performClick()
    }

    /** Opens the menu of the song [title] in the list and picks [entry]. */
    private fun menuOf(title: String, entry: String) {
        composeRule.onNodeWithContentDescription("More options for $title").performClick()
        composeRule.onNodeWithText(entry).performClick()
    }

    private fun SemanticsNodeInteractionCollection.fetchNodes() = fetchSemanticsNodes(atLeastOneRootRequired = false)

    private fun waitFor(what: String, condition: () -> Boolean) {
        try {
            composeRule.waitUntil(WAIT_MS, condition)
        } catch (timeout: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Waited ${WAIT_MS / MS_PER_SECOND} s for $what", timeout)
        }
    }

    /** Runs [block] on the main thread, where the browser lives, and gives its result. */
    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }

    private companion object {
        // The service class is internal to its module; the session is reached by its name.
        const val PLAYBACK_SERVICE = "com.siliconcircuits.tarangini.core.playback.service.PlaybackService"
        const val CONNECT_SECONDS = 20L
        const val WAIT_MS = 30_000L
        const val MS_PER_SECOND = 1_000
        const val LIST_SIZE = 100
    }
}
