/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.session

/**
 * Decides which app may connect to the media session.
 *
 * This app may. A caller the system trusts for media control may: the system
 * itself, Bluetooth, and apps the user gave notification access. Every other
 * app is refused.
 */
internal class CallerPolicy(private val ownPackage: String, private val ownUid: Int) {
    fun allows(packageName: String, uid: Int, trustedBySystem: Boolean): Boolean =
        trustedBySystem || (uid == ownUid && packageName == ownPackage)
}
