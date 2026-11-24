/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.di

import com.siliconcircuits.tarangini.core.domain.premium.CrashReporter
import com.siliconcircuits.tarangini.core.domain.premium.CrossfadeLength
import com.siliconcircuits.tarangini.core.domain.premium.EqualizerBands
import com.siliconcircuits.tarangini.core.domain.premium.PremiumGate
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

    @Binds
    abstract fun bindCrashReporter(reporter: NoCrashReporter): CrashReporter

    @Binds
    abstract fun bindEqualizerBands(bands: FiveEqualizerBands): EqualizerBands

    @Binds
    abstract fun bindCrossfadeLength(length: NoCrossfade): CrossfadeLength
}
