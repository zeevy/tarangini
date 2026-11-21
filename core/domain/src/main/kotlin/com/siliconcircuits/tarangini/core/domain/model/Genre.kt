/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * A genre of the library, from the genre tags of the files.
 *
 * @property name the name as the tags write it; empty for the songs with no genre tag
 * @property songCount how many songs of the genre can be played
 * @property key the form the genre is known by; "Rock" and "rock" have the same. It stays after a rescan
 */
data class Genre(val id: Long, val name: String, val songCount: Int, val key: String = name)
