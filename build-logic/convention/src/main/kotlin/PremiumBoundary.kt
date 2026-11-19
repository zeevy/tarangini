/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

/**
 * The open core boundary: public code never names the package of the private
 * premium module. The premium module plugs in through interfaces instead.
 */
object PremiumBoundary {
    const val PREMIUM_PACKAGE = "${TaranginiBuild.BASE_PACKAGE}.premium"

    private val premiumName = Regex("""(?<![\w.])""" + Regex.escape(PREMIUM_PACKAGE) + """(?![\w])""")

    /** The line numbers, from 1, of the lines in [source] that name the premium package. */
    fun violations(source: String): List<Int> = source.lineSequence()
        .mapIndexedNotNull { index, line -> if (premiumName.containsMatchIn(line)) index + 1 else null }
        .toList()
}
