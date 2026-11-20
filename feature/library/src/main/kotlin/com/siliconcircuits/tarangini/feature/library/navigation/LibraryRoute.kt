/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.navigation

import kotlinx.serialization.Serializable

/**
 * The Library tab. It is a group of screens: the lists, and the album,
 * artist and folder screens that open from them. The bottom bar keeps the
 * tab marked on all of them.
 */
@Serializable
data object LibraryRoute

/** The first screen of the tab: the four lists. */
@Serializable
internal data object LibraryListsRoute
