/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.station

import com.siliconcircuits.tarangini.core.domain.station.StreamTest
import com.siliconcircuits.tarangini.core.domain.station.StreamTester
import kotlinx.coroutines.CompletableDeferred

/**
 * [StreamTester] with the answer a test sets.
 *
 * @property answer what every try gives
 * @property gate when set, a try waits until the test completes it
 */
class FakeStreamTester(
    var answer: StreamTest = StreamTest.Found("audio/mpeg"),
    var gate: CompletableDeferred<Unit>? = null,
) : StreamTester {
    val tested = mutableListOf<String>()

    override suspend fun test(address: String): StreamTest {
        tested += address
        gate?.await()
        return answer
    }
}
