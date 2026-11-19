/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
