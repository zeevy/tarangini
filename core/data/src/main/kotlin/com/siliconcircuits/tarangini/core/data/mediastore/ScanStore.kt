/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import com.siliconcircuits.tarangini.core.data.local.dao.GenreDao
import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import javax.inject.Inject

/** The tables a scan writes. */
internal class ScanStore @Inject constructor(val library: LibraryDao, val genres: GenreDao)
