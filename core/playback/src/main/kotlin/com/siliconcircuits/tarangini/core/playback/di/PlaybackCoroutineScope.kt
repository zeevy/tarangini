/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.playback.di

import javax.inject.Qualifier

/** The scope of the playback connection. It lives as long as the app and runs on the main thread. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class PlaybackCoroutineScope
