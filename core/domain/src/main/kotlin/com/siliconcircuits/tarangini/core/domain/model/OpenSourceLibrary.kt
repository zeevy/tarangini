/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * A library or a font that ships inside the app.
 *
 * @property version the version that ships, or empty when the part has none
 * @property licence the licence the app uses it under
 * @property notice the content of the `NOTICE` file the library ships, or null when it has none
 */
data class OpenSourceLibrary(
    val name: String,
    val version: String,
    val licence: LibraryLicence,
    val notice: String? = null,
)

/**
 * A licence of a library.
 *
 * @property id the short name the text of the licence is found by, such as `Apache-2.0`
 * @property name the name as a person reads it
 */
data class LibraryLicence(val id: String, val name: String)
