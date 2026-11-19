/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/** The MPL-2.0 notice that every source file of the public repo starts with. */
object LicenceHeader {
    const val NOTICE =
        "This Source Code Form is subject to the terms of the Mozilla Public " +
            "License, v. 2.0. If a copy of the MPL was not distributed with this " +
            "file, You can obtain one at https://mozilla.org/MPL/2.0/."

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
