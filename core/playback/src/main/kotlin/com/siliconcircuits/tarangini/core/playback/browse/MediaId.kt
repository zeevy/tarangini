/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

/** The three lists of the library that are sorted by name and can be long. */
internal enum class LibraryList(val word: String) {
    SONGS("songs"),
    ALBUMS("albums"),
    ARTISTS("artists"),
}

/**
 * The id of one node of the list the car shows. Every id the car gets is
 * made by [encode], and every id that comes back is read by [parse]. The
 * car is another app, so an id that comes back is checked like any other
 * input: only the forms below are read, and a number is only digits.
 */
internal sealed interface MediaId {
    /** The top of the list. Its children are the tabs. */
    data object Root : MediaId

    /** The Library tab. */
    data object Library : MediaId

    /** All songs, all albums or all artists. */
    data class Section(val list: LibraryList) : MediaId

    /** The top folders. */
    data object Folders : MediaId

    /** The Favourites tab: the favourite songs, the one marked last first. */
    data object Favourites : MediaId

    /** The Recent tab: the songs heard last, the newest first. */
    data object Recent : MediaId

    /** The Most played tab: the songs heard most often, the one with the most plays first. */
    data object MostPlayed : MediaId

    /**
     * The rows of a long list that start with one letter.
     *
     * @property letter the code point of the letter, or [BEFORE_LETTERS] for the names that sort before the
     * first letter (digits, signs, an empty name)
     */
    data class Letter(val list: LibraryList, val letter: Int) : MediaId

    /** The playlists, under the Library tab. */
    data object Playlists : MediaId

    /** One playlist: its songs in its order. */
    data class Playlist(val id: Long) : MediaId

    /** The genres, under the Library tab. */
    data object Genres : MediaId

    /** One genre: its songs by title. */
    data class Genre(val id: Long) : MediaId

    /** One album: its songs. */
    data class Album(val id: Long) : MediaId

    /** One artist: the songs of the artist. */
    data class Artist(val id: Long) : MediaId

    /** One folder: the folders and the songs directly inside it. */
    data class Folder(val id: Long) : MediaId

    /**
     * One song in the list it was shown in. The list is part of the id, so
     * a tap on the song can play that list from this song on.
     */
    data class SongIn(val songId: Long, val list: MediaId) : MediaId

    /** True for a node whose children are songs, so a song can name it as its list. */
    val holdsSongs: Boolean
        get() = when (this) {
            is Album, is Artist, is Folder, is Playlist, is Genre, Favourites, Recent, MostPlayed -> true
            is Section -> list == LibraryList.SONGS
            is Letter -> list == LibraryList.SONGS
            Root, Library, Folders, Playlists, Genres, is SongIn -> false
        }

    /** The text form of the id, for example `album.12` or `song.345@album.12`. */
    fun encode(): String = when (this) {
        Root, Library, Folders, Favourites, Recent, MostPlayed, Playlists, Genres -> FIXED.getValue(this)
        is Playlist -> "$PLAYLIST$PART$id"
        is Genre -> "$GENRE$PART$id"
        is Section -> list.word
        is Letter -> "${list.word}$PART$letter"
        is Album -> "$ALBUM$PART$id"
        is Artist -> "$ARTIST$PART$id"
        is Folder -> "$FOLDER$PART$id"
        is SongIn -> "$SONG$PART$songId$IN${list.encode()}"
    }

    companion object {
        /** The value of [Letter.letter] for the names that sort before the first letter. */
        const val BEFORE_LETTERS = 0

        private const val ROOT = "root"
        private const val LIBRARY = "library"
        private const val FOLDERS = "folders"
        private const val FAVOURITES = "favourites"
        private const val RECENT = "recent"
        private const val MOST_PLAYED = "mostplayed"
        private const val PLAYLISTS = "playlists"
        private const val PLAYLIST = "playlist"
        private const val GENRES = "genres"
        private const val GENRE = "genre"
        private const val ALBUM = "album"
        private const val ARTIST = "artist"
        private const val FOLDER = "folder"
        private const val SONG = "song"
        private const val PART = "."
        private const val IN = "@"

        // The nodes that are a word alone. Made at the first use: the nodes do not exist
        // yet while this object is made.
        private val FIXED: Map<MediaId, String> by lazy {
            mapOf(
                Root to ROOT,
                Library to LIBRARY,
                Folders to FOLDERS,
                Favourites to FAVOURITES,
                Recent to RECENT,
                MostPlayed to MOST_PLAYED,
                Playlists to PLAYLISTS,
                Genres to GENRES,
            )
        }

        // The longest id this app makes has about 50 characters.
        private const val MAX_LENGTH = 64

        // A row id is a positive Long, which has at most 19 digits. 18 digits always fit.
        private const val MAX_DIGITS = 18

        /** The id this text stands for, or null when this app did not make it. */
        fun parse(text: String): MediaId? {
            // The length is checked before anything else, so a very long text costs nothing.
            val halves = if (text.length > MAX_LENGTH) emptyList() else text.split(IN)
            return when {
                halves.size == 1 -> node(text)
                halves.size == 2 -> song(halves[0], node(halves[1]))
                else -> null
            }
        }

        private fun song(text: String, list: MediaId?): MediaId? {
            val parts = text.split(PART)
            val id = parts.getOrNull(1)?.number()
            val isSong = parts.size == 2 && parts[0] == SONG
            return if (isSong && id != null && list?.holdsSongs == true) SongIn(id, list) else null
        }

        private fun node(text: String): MediaId? {
            val parts = text.split(PART)
            return when (parts.size) {
                1 -> fixed(text)
                2 -> parts[1].number()?.let { number -> numbered(parts[0], number) }
                else -> null
            }
        }

        private fun fixed(word: String): MediaId? = FIXED.entries.firstOrNull { it.value == word }?.key
            ?: LibraryList.entries.firstOrNull { it.word == word }?.let(::Section)

        private fun numbered(word: String, number: Long): MediaId? = when (word) {
            ALBUM -> Album(number)

            ARTIST -> Artist(number)

            FOLDER -> Folder(number)

            PLAYLIST -> Playlist(number)

            GENRE -> Genre(number)

            else -> LibraryList.entries.firstOrNull { it.word == word }
                ?.takeIf { number <= Character.MAX_CODE_POINT }
                ?.let { list -> Letter(list, number.toInt()) }
        }

        /** The number this text is, when it is only digits with no sign, no space and no leading zero. */
        private fun String.number(): Long? =
            takeIf { length in 1..MAX_DIGITS && all { it in '0'..'9' } && (length == 1 || first() != '0') }?.toLong()
    }
}
