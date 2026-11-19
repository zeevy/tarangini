/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local.dao

import androidx.room3.Embedded

/** A song of the saved queue with the queue position it was saved at. */
internal data class QueueSongRow(val position: Int, @Embedded val row: SongRow)
