/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.station

/** What is wrong with a station name a person typed. */
enum class NameFault { EMPTY, TOO_LONG }

/** What is wrong with a stream address a person typed. */
enum class AddressFault {
    EMPTY,
    TOO_LONG,

    /** A space, a line break, another control character or a backslash. */
    ODD_CHARACTERS,

    /** Anything but `http://` or `https://` at the start. */
    NOT_HTTP,

    /** No server name, or a server name or port that cannot be right. */
    NO_HOST,

    /** A user name or a password in the address. It would be stored and shown as plain text. */
    HAS_LOGIN,

    /** Another station of the person has the same address. */
    ALREADY_SAVED,
}

/** The longest name of a station. */
const val MAX_STATION_NAME = 100

/** The longest stream address. Real ones are far shorter. */
const val MAX_STREAM_ADDRESS = 2_000

/**
 * A station name as it is stored: no space at the ends, one space between words, and none of the
 * characters that cannot be seen or that turn the direction of the text around. The name is shown
 * in lists, in the notification and in the car.
 */
fun cleanStationName(name: String): String = name
    .filterNot { (it.isISOControl() && !it.isWhitespace()) || it in unseen }
    .replace(spaces, " ")
    .trim()

/** Says what is wrong with a station name, or null when it can be saved. */
fun checkStationName(name: String): NameFault? {
    val clean = cleanStationName(name)
    return when {
        clean.none { it.isLetterOrDigit() } -> NameFault.EMPTY
        clean.length > MAX_STATION_NAME -> NameFault.TOO_LONG
        else -> null
    }
}

/**
 * Says what is wrong with a stream address, or null when it can be saved.
 *
 * Only a complete `http` or `https` address passes. This is the check for the form. The player
 * checks every address again before it opens it.
 */
fun checkStreamAddress(address: String): AddressFault? {
    val text = address.trim()
    val scheme = text.substringBefore("://", "").lowercase()
    val authority = text.substringAfter("://", "").takeWhile { it !in "/?#" }
    return when {
        text.isEmpty() -> AddressFault.EMPTY
        text.length > MAX_STREAM_ADDRESS -> AddressFault.TOO_LONG
        text.any { it.isWhitespace() || it.isISOControl() || it == '\\' } -> AddressFault.ODD_CHARACTERS
        scheme != "http" && scheme != "https" -> AddressFault.NOT_HTTP
        '@' in authority -> AddressFault.HAS_LOGIN
        !authority.isHostWithPort() -> AddressFault.NO_HOST
        else -> null
    }
}

/** True for `host`, `host:port` and `[ipv6]:port` with a port from 1 to 65535. */
private fun String.isHostWithPort(): Boolean {
    val host = if (startsWith('[')) substringBefore(']', "") else substringBefore(':')
    val rest = if (startsWith('[')) substringAfter(']', "") else substring(host.length)
    val port = rest.removePrefix(":")
    val portFits = rest.isEmpty() || (rest.startsWith(':') && port.length in PORT_DIGITS && port.toIntOrNull() in PORTS)
    return host.length > (if (startsWith('[')) 1 else 0) && portFits
}

private val PORT_DIGITS = 1..5
private val PORTS = 1..65_535
private val spaces = Regex("\\s+")

// Zero width space, byte order mark and the marks that turn text around. The joiners that Indian scripts need stay.
private val unseen = "\u200B\uFEFF\u061C\u200E\u200F\u202A\u202B\u202C\u202D\u202E\u2066\u2067\u2068\u2069".toSet()
