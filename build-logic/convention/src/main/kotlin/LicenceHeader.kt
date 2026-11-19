/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

/** The GPL-3.0 notice that every source file of the public repo starts with. */
object LicenceHeader {
    const val NOTICE =
        "Copyright (C) Silicon Circuits and the Tarangini contributors. This file " +
            "is part of Tarangini, free software under the GNU General Public License, " +
            "version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only"

    /** The notice must end within this many lines, so it stays at the top of the file. */
    private const val TOP_LINES = 12

    private val commentMarks = Regex("""^\s*(/\*+|\*+/|\*|//|<!--|-->|#)?\s*""")

    /** True when [source] has the notice as a comment at the top, in any comment style. */
    fun isPresent(source: String): Boolean {
        val top = source.lineSequence()
            .take(TOP_LINES)
            .map { line -> line.replace(commentMarks, "").trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        return NOTICE in top
    }
}
