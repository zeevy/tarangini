/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

/** What a caller may do with the media session. */
internal enum class CallerAccess {
    /** Everything: the phone UI of this app. */
    FULL,

    /** Play, pause, seek and skip in the queue that is there. It cannot change what is in the queue. */
    TRANSPORT_ONLY,

    /** The caller may not connect. */
    REFUSED,
}

/**
 * Decides which app may connect to the media session, and how much it may do.
 *
 * This app gets everything. A caller the system trusts for media control
 * gets the transport controls: the system itself, Bluetooth, and apps the
 * user gave notification access. Every other app is refused.
 */
internal class CallerPolicy(private val ownPackage: String, private val ownUid: Int) {
    fun accessFor(packageName: String, uid: Int, trustedBySystem: Boolean): CallerAccess = when {
        uid == ownUid && packageName == ownPackage -> CallerAccess.FULL
        trustedBySystem -> CallerAccess.TRANSPORT_ONLY
        else -> CallerAccess.REFUSED
    }
}
