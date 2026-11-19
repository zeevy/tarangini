/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.feature.library.permission

/** Where the app stands with the permission to read the audio files of the phone. */
internal enum class AudioPermissionState {
    /** The user was not asked in this visit, and Android may still show its question. */
    NOT_ASKED,

    GRANTED,

    /** The user said no once. Android will show its question again. */
    DENIED,

    /** The user said no and Android no longer shows its question. Only the app settings can change it. */
    DENIED_FOR_GOOD,
}

/** This many refusals with no word from Android mean that Android no longer shows its question. */
internal const val SILENT_REFUSALS_FOR_GOOD = 2

/**
 * Works out the state from what Android tells.
 *
 * Android does not say "never asked", "closed the question without an answer" and "refused for
 * good" apart: all three look like "not granted, and no reason to explain". So the refusals that
 * came with no word from Android are counted, see [silentRefusalsAfter].
 *
 * @param granted the permission is granted
 * @param silentRefusals the count that [silentRefusalsAfter] keeps for this visit
 * @param canAskAgain Android says the app should explain itself, which it does only after a first no
 */
internal fun audioPermissionState(granted: Boolean, silentRefusals: Int, canAskAgain: Boolean): AudioPermissionState =
    when {
        granted -> AudioPermissionState.GRANTED
        canAskAgain -> AudioPermissionState.DENIED
        silentRefusals >= SILENT_REFUSALS_FOR_GOOD -> AudioPermissionState.DENIED_FOR_GOOD
        silentRefusals > 0 -> AudioPermissionState.DENIED
        else -> AudioPermissionState.NOT_ASKED
    }

/**
 * The new count of silent refusals after a request came back with a no.
 *
 * - Android will ask again: it was a first no, and nothing is silent about it.
 * - Android would ask before the request and will not now: it was the second no, which is for good.
 * - Android says nothing before and after: the user closed the question without an answer, or
 *   Android did not show it at all. One such refusal keeps the button that asks. A second one in a
 *   row means the question no longer shows, and the user is sent to the app settings.
 *
 * @param before the count before this request
 * @param couldAskAgain what Android said before the request
 * @param canAskAgain what Android says now
 */
internal fun silentRefusalsAfter(before: Int, couldAskAgain: Boolean, canAskAgain: Boolean): Int = when {
    canAskAgain -> 0
    couldAskAgain -> SILENT_REFUSALS_FOR_GOOD
    else -> before + 1
}
