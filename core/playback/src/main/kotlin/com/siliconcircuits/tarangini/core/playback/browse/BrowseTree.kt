/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

import androidx.media3.common.MediaItem
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import com.siliconcircuits.tarangini.core.domain.repository.LibraryLetters
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.domain.repository.PlayHistory

/** The songs a tap in the car plays, and which of them was tapped. */
internal data class PickedQueue(val songs: List<Song>, val index: Int)

/**
 * The list the car shows, read from the same library as the phone screens.
 *
 * The car has no paging and shows only a limited number of rows, so no
 * node gives a long list. A list by name with more than [WHOLE_LIST] rows
 * is shown as one node per first letter. A node never gives more than
 * [Page.MAX_LIMIT] rows; what is after them is found by voice or search.
 */
internal class BrowseTree(
    private val library: LibraryRepository,
    private val letters: LibraryLetters,
    private val items: BrowseItems,
    private val favourites: FavouriteRepository,
    private val history: PlayHistory,
) {
    /** The top node. The car asks for it first, before any list. */
    val root: MediaItem = items.list(MediaId.Root)

    /**
     * What the car shows under [parent], or null when [parent] is a song, which has nothing under it.
     *
     * @param tabLimit how many tabs the car shows; the tabs after that number are left out
     */
    suspend fun children(parent: MediaId, tabLimit: Int): List<MediaItem>? = when (parent) {
        MediaId.Root -> items.tabs().take(tabLimit)
        MediaId.Library -> items.sections()
        is MediaId.Section -> section(parent)
        is MediaId.Letter -> rows(parent.list, parent, group(parent))
        MediaId.Folders -> library.folders(null).map(items::folder)
        is MediaId.Folder -> library.folders(parent.id).map(items::folder) + songItems(parent)
        is MediaId.Album, is MediaId.Artist, MediaId.Favourites, MediaId.Recent -> songItems(parent)
        is MediaId.SongIn -> null
    }

    /** The item with this id, or null for a song the library does not have. */
    suspend fun item(id: MediaId): MediaItem? = when (id) {
        MediaId.Root -> root
        is MediaId.SongIn -> library.song(id.songId)?.let { song -> items.song(song, id.list) }
        else -> (items.tabs() + items.sections()).firstOrNull { it.mediaId == id.encode() } ?: items.list(id)
    }

    /**
     * What a tap on a song plays: the list the song was shown in, from that
     * song on. When the song is no longer in that list, because the library
     * changed after the car read it, the song plays alone. Null when the
     * library does not have the song at all.
     */
    suspend fun queue(picked: MediaId.SongIn): PickedQueue? {
        val songs = songsOf(picked.list)
        val index = songs.indexOfFirst { it.id == picked.songId }
        return if (index >= 0) {
            PickedQueue(songs, index)
        } else {
            library.song(picked.songId)?.let { song -> PickedQueue(listOf(song), 0) }
        }
    }

    /** A short list comes whole. A long one comes as its letters. */
    private suspend fun section(section: MediaId.Section): List<MediaItem> {
        val groups = groups(section.list)
        val total = groups.sumOf { it.count }
        return if (total <= WHOLE_LIST) {
            rows(section.list, section, LetterGroup(MediaId.BEFORE_LETTERS, offset = 0, count = total))
        } else {
            groups.map { group -> items.list(MediaId.Letter(section.list, group.letter), group.label()) }
        }
    }

    /** The rows of one part of a list by name. [parent] is the node the car shows them under. */
    private suspend fun rows(list: LibraryList, parent: MediaId, part: LetterGroup?): List<MediaItem> {
        val page = part?.page() ?: return emptyList()
        return when (list) {
            LibraryList.SONGS -> library.songs(SongSort.TITLE, page).map { song -> items.song(song, parent) }
            LibraryList.ALBUMS -> library.albums(page).map(items::album)
            LibraryList.ARTISTS -> library.artists(page).map(items::artist)
        }
    }

    private suspend fun songItems(list: MediaId): List<MediaItem> = songsOf(list).map { song -> items.song(song, list) }

    /** The songs under a node whose children are songs, in the order the car shows them. */
    private suspend fun songsOf(list: MediaId): List<Song> = when (list) {
        is MediaId.Section -> library.songs(SongSort.TITLE, Page(Page.MAX_LIMIT))
        is MediaId.Letter -> group(list)?.page()?.let { page -> library.songs(SongSort.TITLE, page) }.orEmpty()
        is MediaId.Album -> library.songsOfAlbum(list.id, Page(Page.MAX_LIMIT))
        is MediaId.Artist -> library.songsOfArtist(list.id, Page(Page.MAX_LIMIT))
        is MediaId.Folder -> library.songsInFolder(list.id, Page(Page.MAX_LIMIT))
        MediaId.Favourites -> favourites.songs(Page(Page.MAX_LIMIT))
        MediaId.Recent -> history.recentSongs()
        MediaId.Root, MediaId.Library, MediaId.Folders, is MediaId.SongIn -> emptyList()
    }

    private suspend fun group(letter: MediaId.Letter): LetterGroup? =
        groups(letter.list).firstOrNull { it.letter == letter.letter }

    private suspend fun groups(list: LibraryList): List<LetterGroup> = when (list) {
        LibraryList.SONGS -> letters.ofSongs()
        LibraryList.ALBUMS -> letters.ofAlbums()
        LibraryList.ARTISTS -> letters.ofArtists()
    }.toGroups()

    private companion object {
        /** A list by name with more rows than this is shown by letter. A driver cannot scroll through more. */
        const val WHOLE_LIST = 100
    }
}
