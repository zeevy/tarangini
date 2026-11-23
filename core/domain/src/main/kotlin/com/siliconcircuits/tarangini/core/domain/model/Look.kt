/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/** The colours of the app on the phone. The car is not changed by it: Android Auto draws its own screens. */
enum class Look {
    /** Light by day and dark at night, as the dark mode of the phone says. */
    FOLLOW_PHONE,

    /** Always the light look. */
    LIGHT,

    /** Always the dark look. */
    DARK,
}
