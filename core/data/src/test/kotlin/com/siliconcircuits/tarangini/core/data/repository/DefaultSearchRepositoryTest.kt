/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.addSong
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultSearchRepositoryTest {
    private val database = testDatabase()
    private val search = DefaultSearchRepository(database.libraryDao())
    private var nextFileId = 1

    @After
    fun closeDatabase() = database.close()

    private suspend fun song(title: String, artist: String = "Artist", album: String = "Album"): Long =
        database.addSong(title, artist, album, "${nextFileId++}")

    private fun SearchResults.names() = Triple(songs.map { it.title }, albums.map { it.title }, artists.map { it.name })

    @Test
    fun `results come in three groups, songs, albums and artists`() = runTest {
        song("Morning Raga", artist = "Ravi Shankar", album = "Live in Delhi")
        song("Delhi Nights", artist = "Asha", album = "City Songs")
        song("Evening", artist = "Delhi Brothers", album = "First")

        val found = search.search("delhi")

        assertEquals(
            Triple(
                listOf("Delhi Nights", "Evening", "Morning Raga"),
                listOf("Live in Delhi"),
                listOf("Delhi Brothers"),
            ),
            found.names(),
        )
        assertEquals(1, found.albums.single().songCount)
        assertEquals("Ravi Shankar", found.albums.single().artist)
        assertEquals(1, found.artists.single().songCount)
    }

    @Test
    fun `a title that starts with the text comes first, then a word in the title, then artist and album`() = runTest {
        song("A song", artist = "Raga Group")
        song("The Raga of Dawn")
        song("Raga Bhairavi")
        song("Night Raga")
        song("Raga: Yaman")

        assertEquals(
            listOf("Raga Bhairavi", "Raga: Yaman", "Night Raga", "The Raga of Dawn", "A song"),
            search.search("raga").songs.map { it.title },
        )
    }

    @Test
    fun `albums and artists that start with the text come first`() = runTest {
        song("one", artist = "The Rain Band", album = "After the Rain")
        song("two", artist = "Rain Tree", album = "Rain Songs")

        val found = search.search("rain")

        assertEquals(listOf("Rain Songs", "After the Rain"), found.albums.map { it.title })
        assertEquals(listOf("Rain Tree", "The Rain Band"), found.artists.map { it.name })
    }

    @Test
    fun `every word must match, and the words of a song may be in different places`() = runTest {
        song("Morning Raga", artist = "Ravi Shankar", album = "Live in Delhi")
        song("Morning Walk", artist = "Other")

        val found = search.search("morn ravi")

        assertEquals(Triple(listOf("Morning Raga"), emptyList<String>(), emptyList<String>()), found.names())
        assertEquals(listOf("Ravi Shankar"), search.search("SHANK  rav").artists.map { it.name })
    }

    @Test
    fun `the start of a word of an Indian script does not find a word with another vowel`() = runTest {
        // Per script: a word, the start of it, and a start with the same consonant and another vowel.
        val words = listOf(
            Triple("గోవిందా", "గో", "గా"),
            Triple("गोविंदा", "गो", "गा"),
            Triple("கோவிந்தா", "கோ", "கா"),
            Triple("ಗೋವಿಂದಾ", "ಗೋ", "ಗಾ"),
            Triple("ഗോവിന്ദാ", "ഗോ", "ഗാ"),
            Triple("গোবিন্দ", "গো", "গা"),
        )
        words.forEach { (word, _, _) -> song(word) }

        words.forEach { (word, start, otherVowel) ->
            assertEquals(listOf(word), search.search(start).songs.map { it.title })
            assertEquals(word, emptyList<String>(), search.search(otherVowel).songs.map { it.title })
            // The bare consonant is the start of the word as it is written.
            assertEquals(listOf(word), search.search(start.take(1)).songs.map { it.title })
        }
    }

    @Test
    fun `Telugu and Hindi text finds songs with such tags`() = runTest {
        song("ఎందరో మహానుభావులు", artist = "త్యాగరాజు", album = "పంచరత్న కృతులు")
        song("रघुपति राघव", artist = "विष्णु दिगंबर", album = "भजन")

        assertEquals(listOf("ఎందరో మహానుభావులు"), search.search("మహాను").songs.map { it.title })
        assertEquals(listOf("త్యాగరాజు"), search.search("త్యాగ").artists.map { it.name })
        assertEquals(listOf("పంచరత్న కృతులు"), search.search("కృతు").albums.map { it.title })
        assertEquals(listOf("रघुपति राघव"), search.search("राघव").songs.map { it.title })
    }

    @Test
    fun `a text of signs only, or of nothing, finds nothing and does not fail`() = runTest {
        song("Plain")

        listOf("", "   ", "!!!", "\"", "* - ()", "'; --", "%_").forEach { text ->
            assertTrue(text, search.search(text).isEmpty)
        }
    }

    @Test
    fun `signs and search operators in the text are only text`() = runTest {
        song("Rock - Roll", artist = "AC/DC", album = "OR NOT")

        assertEquals(listOf("Rock - Roll"), search.search("rock & roll").songs.map { it.title })
        assertEquals(listOf("AC/DC"), search.search("ac/dc").artists.map { it.name })
        assertEquals(listOf("OR NOT"), search.search("OR NOT").albums.map { it.title })
        assertEquals(listOf("Rock - Roll"), search.search("\u0301 rock, roll!").songs.map { it.title })
        assertTrue(search.search("rock OR zzz").isEmpty)
        assertTrue(search.search("title:zzz").isEmpty)
    }

    @Test
    fun `a song whose file is gone is not found, and neither are its album and artist`() = runTest {
        val gone = song("Gone", artist = "Lost", album = "Missing")
        database.libraryDao().markMissing(listOf(gone), at = 1)

        assertTrue(search.search("gone").isEmpty)
        assertTrue(search.search("lost").isEmpty)
        assertTrue(search.search("missing").isEmpty)
    }

    @Test
    fun `each group has at most as many rows as were asked for`() = runTest {
        repeat(5) { song("Raga $it", artist = "Raga artist $it", album = "Raga album $it") }

        val found = search.search("raga", limit = 3)

        assertEquals(Triple(3, 3, 3), Triple(found.songs.size, found.albums.size, found.artists.size))
        listOf(0, 101).forEach { limit ->
            assertTrue(runCatching { search.search("raga", limit) }.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun `only the start of a very long text is used`() = runTest {
        song("Rock - Roll")

        assertEquals(listOf("Rock - Roll"), search.search("rock" + " ".repeat(100_000) + "zzz").songs.map { it.title })
    }
}
