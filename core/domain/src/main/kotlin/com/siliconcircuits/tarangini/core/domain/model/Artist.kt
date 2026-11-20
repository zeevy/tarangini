/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * One artist.
 *
 * @property name the artist name, or an empty text for the songs that name no artist
 * @property songCount how many of the artist's songs can be played now
 */
data class Artist(val id: Long, val name: String, val songCount: Int)
