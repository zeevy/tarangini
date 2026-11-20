/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.list

import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs [block] and gives its result, or what went wrong. A read of the
 * library can fail, for example when the storage is full, and that must not
 * end the screen. A cancelled coroutine stays cancelled.
 */
internal suspend fun <T> attempt(block: suspend () -> T): Result<T> =
    runCatching { block() }.onFailure { if (it is CancellationException) throw it }
