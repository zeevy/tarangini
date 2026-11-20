/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.dao.LetterDao
import com.siliconcircuits.tarangini.core.data.local.dao.LetterRow
import com.siliconcircuits.tarangini.core.domain.model.LetterCount
import com.siliconcircuits.tarangini.core.domain.repository.LibraryLetters
import javax.inject.Inject

/** The first letters from the database of the app, one grouped query per list. */
internal class DefaultLibraryLetters @Inject constructor(private val letters: LetterDao) : LibraryLetters {
    override suspend fun ofSongs(): List<LetterCount> = letters.ofSongs().map(LetterRow::toLetterCount)

    override suspend fun ofAlbums(): List<LetterCount> = letters.ofAlbums().map(LetterRow::toLetterCount)

    override suspend fun ofArtists(): List<LetterCount> = letters.ofArtists().map(LetterRow::toLetterCount)
}

private fun LetterRow.toLetterCount() = LetterCount(letter, total)
