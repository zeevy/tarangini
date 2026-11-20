/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.memory.MemoryCache

/**
 * Sets the picture loader every cover picture of the app is loaded with.
 * The application calls it when it starts. It may be called again, as the
 * tests do that make the application many times in one process: the new
 * loader then takes the place of the old one.
 */
// The safe call of Coil throws when a picture was loaded before it, which a second start in a test would hit.
@OptIn(DelicateCoilApi::class)
fun installArtworkLoader() {
    SingletonImageLoader.setUnsafe(::artworkLoader)
}

/**
 * The loader keeps decoded pictures in memory, in at most a part of the
 * memory of the app, so a list that scrolls back shows them at once. It
 * keeps nothing on disk: every picture comes from the phone itself, which
 * already stores the small pictures it makes.
 */
private fun artworkLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
    .memoryCache { MemoryCache.Builder().maxSizePercent(context, MEMORY_PART).build() }
    .diskCache(null)
    .build()

private const val MEMORY_PART = 0.15
