/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.library

import com.siliconcircuits.tarangini.core.domain.model.LetterCount
import com.siliconcircuits.tarangini.core.domain.repository.LibraryLetters

/** The first letters of what a test put into [library], for plain names, in the order the fake library lists them. */
class FakeLibraryLetters(private val library: FakeLibraryRepository) : LibraryLetters {
    override suspend fun ofSongs(): List<LetterCount> = letters(library.songs.map { it.title })

    override suspend fun ofAlbums(): List<LetterCount> = letters(library.albums.map { it.title })

    override suspend fun ofArtists(): List<LetterCount> = letters(library.artists.map { it.name })

    private fun letters(names: List<String>): List<LetterCount> = names
        .groupingBy { it.lowercase().take(1) }
        .eachCount()
        .toSortedMap()
        .map { (letter, count) -> LetterCount(letter, count) }
}
