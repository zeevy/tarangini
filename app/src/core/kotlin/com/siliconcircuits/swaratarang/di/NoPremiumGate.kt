/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.di

import com.siliconcircuits.swaratarang.core.domain.premium.PremiumGate
import javax.inject.Inject

/** The premium gate of the core build: there are no premium features. */
internal class NoPremiumGate @Inject constructor() : PremiumGate {
    override val isUnlocked: Boolean = false
}
