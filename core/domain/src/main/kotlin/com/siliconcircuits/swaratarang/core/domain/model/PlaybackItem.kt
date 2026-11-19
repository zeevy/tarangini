/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.domain.model

/**
 * One item in the player's queue. A text is null when the player does not know it.
 *
 * @property id the id the item was loaded with
 * @property artworkUri the address of the cover picture
 */
data class PlaybackItem(
    val id: String,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artworkUri: String? = null,
)
