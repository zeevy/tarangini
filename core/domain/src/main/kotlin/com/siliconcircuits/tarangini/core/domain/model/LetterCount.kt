/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * How many rows of a list sorted by name start with one letter.
 *
 * @property letter the first letter of the name as the list sorts it (lower case), or an empty text for a name
 * that is empty
 */
data class LetterCount(val letter: String, val count: Int)
