/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * The address of the cover picture of a local song, made from the address
 * the song is played from, or null for anything that is not a local song.
 * The phone gives the picture inside the file there, in a small size, and
 * an error when the file has none. So the address says where to ask, not
 * that a picture exists.
 */
fun artworkUriOf(mediaUri: String): String? = if (localSong.matches(mediaUri)) "$mediaUri/albumart" else null

// Exactly the address of one audio file of the phone: no query, no fragment and nothing after the number.
private val localSong = Regex("""content://media/[^/?#]+/audio/media/\d+""")
