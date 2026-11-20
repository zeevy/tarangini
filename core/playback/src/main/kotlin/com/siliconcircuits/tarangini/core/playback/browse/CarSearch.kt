/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

import android.provider.MediaStore
import androidx.media3.common.MediaItem
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.domain.repository.SearchRepository
import java.util.Locale

/** What the person who spoke asked for first, when the voice assistant says so. */
internal enum class SpokenFocus { ANY, SONG, ARTIST, ALBUM }

/**
 * A spoken "play ..." as the voice assistant hands it over.
 *
 * @property text what to look for, cut to [MAX_LENGTH] characters. It is empty for "play some music"
 */
internal data class SpokenRequest(val text: String, val focus: SpokenFocus = SpokenFocus.ANY) {
    /** The words of the text: runs of letters and digits, in lower case. Signs between them do not count. */
    val words: List<String> get() = text.words()

    companion object {
        /** A spoken request is a few words. A longer text is cut, so nothing long reaches the search. */
        const val MAX_LENGTH = 200
    }
}

/**
 * The spoken request an item stands for, or null when the item is not one.
 * The voice assistant sends an item with a search text and no address. It
 * can add what it understood: that the person named a song, an artist or an
 * album, and that name. The text comes from another app, so it is only ever used
 * as text to search for.
 */
internal fun MediaItem.spokenRequest(): SpokenRequest? {
    val query = requestMetadata.searchQuery?.toString()
    val hasAddress = localConfiguration != null || requestMetadata.mediaUri != null
    if (query == null || hasAddress) return null
    val extras = requestMetadata.extras
    val focus = when (extras?.getString(MediaStore.EXTRA_MEDIA_FOCUS)) {
        MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> SpokenFocus.SONG
        MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> SpokenFocus.ARTIST
        MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> SpokenFocus.ALBUM
        else -> SpokenFocus.ANY
    }
    val named = when (focus) {
        SpokenFocus.SONG -> extras?.getString(MediaStore.EXTRA_MEDIA_TITLE)
        SpokenFocus.ARTIST -> extras?.getString(MediaStore.EXTRA_MEDIA_ARTIST)
        SpokenFocus.ALBUM -> extras?.getString(MediaStore.EXTRA_MEDIA_ALBUM)
        SpokenFocus.ANY -> null
    }
    return SpokenRequest((named?.takeIf { it.isNotBlank() } ?: query).take(SpokenRequest.MAX_LENGTH), focus)
}

/**
 * Search for the car: the results its search screen shows, and what a
 * spoken "play ..." starts. Both use the search of the phone, so they find
 * the same things.
 */
internal class CarSearch(
    private val search: SearchRepository,
    private val library: LibraryRepository,
    private val items: BrowseItems,
) {
    /**
     * What the search screen of the car shows for [text]: songs, then
     * artists, then albums. A tap on a song plays its album from that song
     * on; an artist or an album opens its songs.
     */
    suspend fun results(text: String): List<MediaItem> {
        val found = find(SpokenRequest(text.take(SpokenRequest.MAX_LENGTH)))
        return found.songs.map { song -> items.song(song, MediaId.Album(song.albumId)) } +
            found.artists.map(items::artist) +
            found.albums.map(items::album)
    }

    /**
     * What a spoken request plays, or null when nothing fits.
     *
     * A song whose title has every word comes first: it plays, and the
     * other songs found come after it. Then an artist: the songs of the
     * artist. Then an album: its songs. When the assistant says the person
     * named an artist or an album, that kind is tried first. Last come the
     * songs found by another text of theirs, such as the name of an album.
     */
    suspend fun queue(request: SpokenRequest): PickedQueue? {
        val found = find(request)
        val kinds = when (request.focus) {
            SpokenFocus.ANY, SpokenFocus.SONG -> listOf(Kind.SONG, Kind.ARTIST, Kind.ALBUM)
            SpokenFocus.ARTIST -> listOf(Kind.ARTIST, Kind.SONG, Kind.ALBUM)
            SpokenFocus.ALBUM -> listOf(Kind.ALBUM, Kind.SONG, Kind.ARTIST)
        }
        val songs = kinds.firstNotNullOfOrNull { kind -> songsOf(kind, found, request.words).ifEmpty { null } }
            ?: found.songs
        return if (songs.isEmpty()) null else PickedQueue(songs, 0)
    }

    private suspend fun find(request: SpokenRequest): SearchResults =
        if (request.words.isEmpty()) SearchResults() else search.search(request.text)

    private suspend fun songsOf(kind: Kind, found: SearchResults, words: List<String>): List<Song> = when (kind) {
        Kind.SONG -> found.songs.firstOrNull { song -> song.title.hasWords(words) }
            ?.let { song -> listOf(song) + (found.songs - song) }
            .orEmpty()

        Kind.ARTIST -> found.artists.firstOrNull()
            ?.let { artist -> library.songsOfArtist(artist.id, Page(Page.MAX_LIMIT)) }
            .orEmpty()

        Kind.ALBUM -> found.albums.firstOrNull()
            ?.let { album -> library.songsOfAlbum(album.id, Page(Page.MAX_LIMIT)) }
            .orEmpty()
    }

    private enum class Kind { SONG, ARTIST, ALBUM }
}

// Marks belong to a word: the vowel signs of Telugu and Hindi are marks, not letters.
private val notLetterOrDigit = Regex("[^\\p{L}\\p{N}\\p{M}]+")

private fun String.words(): List<String> = lowercase(Locale.ROOT).split(notLetterOrDigit).filter { it.isNotEmpty() }

/** True when every one of [words] starts a word of this name. */
private fun String.hasWords(words: List<String>): Boolean {
    val own = words()
    return words.all { word -> own.any { it.startsWith(word) } }
}
