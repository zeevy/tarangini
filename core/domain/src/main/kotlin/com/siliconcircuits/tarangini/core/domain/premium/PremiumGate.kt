/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.domain.premium

/**
 * Says whether the premium features can be used. The core build always
 * answers no. The play build takes the real answer from the premium module.
 *
 * A core feature must never ask this gate.
 */
interface PremiumGate {
    val isUnlocked: Boolean
}
