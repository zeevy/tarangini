/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local

import java.text.Normalizer
import java.util.Locale

/**
 * The form of a name that lists are sorted by: no difference between upper
 * and lower case or between two ways to write the same letter, and no spaces
 * at the ends. It is stored next to the name, so sorting uses an index and
 * gives the same order on every phone.
 */
internal fun sortKey(name: String): String =
    Normalizer.normalize(name.trim(), Normalizer.Form.NFKC).lowercase(Locale.ROOT)
