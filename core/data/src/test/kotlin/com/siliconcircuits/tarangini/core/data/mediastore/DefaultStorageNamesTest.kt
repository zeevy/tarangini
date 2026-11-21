/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DefaultStorageNamesTest {
    private val names = DefaultStorageNames(RuntimeEnvironment.getApplication())

    @Test
    fun `the built-in storage is the phone storage`() {
        assertEquals("Phone storage", names.nameOf("external_primary"))
    }

    @Test
    fun `a storage Android does not know is a memory card`() {
        assertEquals("Memory card", names.nameOf("1a2b-3c4d"))
    }
}
