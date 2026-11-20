/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import android.net.Uri
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** The only kinds of address a stream may have. A file, the content of an app and everything else is not a stream. */
internal val STREAM_SCHEMES = setOf("http", "https")

/**
 * True for a complete `http` or `https` address with a host, and for nothing else.
 *
 * An address with a user name or password in it is refused: the address is the id of the item, and
 * the id is shown to every app that may see what plays. An address with a backslash is refused,
 * because Android and the HTTP client do not agree on which host it names.
 */
internal fun Uri.isStreamAddress(): Boolean = scheme?.lowercase() in STREAM_SCHEMES &&
    !host.isNullOrEmpty() &&
    encodedUserInfo.isNullOrEmpty() &&
    BACKSLASH !in toString() &&
    toString().toHttpUrlOrNull() != null

private const val BACKSLASH = '\\'
