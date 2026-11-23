/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * Where a queue came from, such as the album "Morning Ragas". Now playing names it in its top bar.
 *
 * @property kind the kind of list the queue was started from
 * @property name the name of that list, cut to [MAX_NAME] characters; empty for [QueueSourceKind.FAVOURITES]
 */
data class QueueSource(val kind: QueueSourceKind, val name: String = "") {
    companion object {
        /** Names come from file tags, so a long one is cut. */
        const val MAX_NAME = 200

        /** A source with its name cut to [MAX_NAME] characters. */
        fun of(kind: QueueSourceKind, name: String) = QueueSource(kind, name.take(MAX_NAME))

        /** A source for a list with a name, or null when the name is blank and there is nothing to show. */
        fun named(kind: QueueSourceKind, name: String): QueueSource? =
            name.takeIf { it.isNotBlank() }?.let { of(kind, it) }
    }
}

/** The lists a queue can be started from. A list with no kind here (all songs, search) gives no source. */
enum class QueueSourceKind { ALBUM, ARTIST, PLAYLIST, GENRE, FOLDER, FAVOURITES }
