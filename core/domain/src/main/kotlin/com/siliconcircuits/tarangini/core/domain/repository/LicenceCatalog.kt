/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.OpenSourceLibrary

/** The libraries that ship inside the app and the texts of their licences. The list is made by the build. */
interface LicenceCatalog {
    /** Every library and font in the app, by name. */
    suspend fun libraries(): List<OpenSourceLibrary>

    /** The full text of a licence, or null when the app has no text for this id. */
    suspend fun text(licenceId: String): String?
}
