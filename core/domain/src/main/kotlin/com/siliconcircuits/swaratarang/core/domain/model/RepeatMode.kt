/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.domain.model

/** What the player does when an item or the queue ends. */
enum class RepeatMode {
    /** Stop after the last item. */
    OFF,

    /** Play the current item again and again. */
    ONE,

    /** Start the queue again after the last item. */
    ALL,
}
