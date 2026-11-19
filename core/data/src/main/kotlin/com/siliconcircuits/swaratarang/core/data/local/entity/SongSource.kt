/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local.entity

/** Where a song file lives. The name is stored in the database, so a name never changes. */
internal enum class SongSource {
    /** A file on the phone, found through MediaStore. */
    LOCAL,
}
