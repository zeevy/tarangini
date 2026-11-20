/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

/**
 * The ids of the lists a caller has read, the newest last. When the
 * library changes, the car is told about these and no others. Only the
 * last [MAX] are kept: a car has a few lists open, not more, and a caller
 * must not be able to fill the memory with ids.
 */
internal class OpenedLists {
    private val ids = LinkedHashSet<String>()

    /** Notes that the list with this id was read. */
    fun add(listId: String) {
        // The newest goes to the end, also when it was read before.
        ids.remove(listId)
        ids.add(listId)
        if (ids.size > MAX) ids.remove(ids.first())
    }

    /** The ids, the newest last. */
    fun all(): List<String> = ids.toList()

    private companion object {
        const val MAX = 50
    }
}
