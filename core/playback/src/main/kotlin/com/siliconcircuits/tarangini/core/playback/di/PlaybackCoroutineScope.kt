/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.di

import javax.inject.Qualifier

/** The scope of the playback connection. It lives as long as the app and runs on the main thread. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class PlaybackCoroutineScope
