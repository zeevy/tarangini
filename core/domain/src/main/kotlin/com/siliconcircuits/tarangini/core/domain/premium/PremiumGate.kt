/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
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
