/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.domain.model

/** Why playback stopped, in words a screen can turn into a message. */
enum class PlaybackError {
    /** The file is gone or the app may not read it. */
    FILE_NOT_AVAILABLE,

    /** The network or the server did not answer. */
    NETWORK,

    /** The file or stream is in a form the player cannot play. */
    CANNOT_PLAY,

    /** Anything else. */
    UNKNOWN,
}
