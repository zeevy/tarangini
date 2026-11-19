/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

plugins {
    id("tarangini.kotlin.library")
}

dependencies {
    // `PlaybackController` gives its state as a `StateFlow`, so every user of the domain needs the type.
    api(libs.kotlinx.coroutines.core)
}
