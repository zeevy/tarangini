/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.di

import com.siliconcircuits.swaratarang.core.domain.premium.PremiumGate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the premium interfaces to their "not available" versions in the core build. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class CorePremiumModule {
    @Binds
    abstract fun bindPremiumGate(gate: NoPremiumGate): PremiumGate
}
