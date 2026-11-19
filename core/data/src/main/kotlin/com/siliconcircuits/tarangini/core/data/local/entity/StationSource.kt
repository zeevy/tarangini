/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.data.local.entity

/** Where a saved station came from. The name is stored in the database, so a name never changes. */
internal enum class StationSource {
    RADIO_BROWSER,
    USER,
}
