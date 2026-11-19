/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.di

import com.siliconcircuits.tarangini.core.domain.premium.PremiumGate
import javax.inject.Inject

/** The premium gate of the core build: there are no premium features. */
internal class NoPremiumGate @Inject constructor() : PremiumGate {
    override val isUnlocked: Boolean = false
}
