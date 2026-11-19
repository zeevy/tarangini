/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * The open core boundary: public code never names the package of the private
 * premium module. The premium module plugs in through interfaces instead.
 */
object PremiumBoundary {
    const val PREMIUM_PACKAGE = "${SwaraTarangBuild.BASE_PACKAGE}.premium"

    private val premiumName = Regex("""(?<![\w.])""" + Regex.escape(PREMIUM_PACKAGE) + """(?![\w])""")

    /** The line numbers, from 1, of the lines in [source] that name the premium package. */
    fun violations(source: String): List<Int> = source.lineSequence()
        .mapIndexedNotNull { index, line -> if (premiumName.containsMatchIn(line)) index + 1 else null }
        .toList()
}
