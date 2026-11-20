/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import com.siliconcircuits.tarangini.core.data.di.ApplicationScope
import com.siliconcircuits.tarangini.core.data.work.RescanSchedule
import com.siliconcircuits.tarangini.core.domain.library.LibraryScanner
import com.siliconcircuits.tarangini.core.domain.library.LibraryWatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Scans when the app starts and again after the phone said that its audio
 * files changed. The phone says so many times while files are copied, so a
 * scan waits until the phone has been quiet for a moment: many changes in a
 * row give one scan. Changes that never stop give a scan every half minute.
 *
 * It only asks for scans. [LibraryScanner] makes sure that one scan runs at
 * a time, also against the job that scans while the app is closed.
 */
@Singleton
internal class DefaultLibraryWatcher @Inject constructor(
    private val scanner: LibraryScanner,
    private val source: AudioFileSource,
    private val schedule: RescanSchedule,
    @param:ApplicationScope private val scope: CoroutineScope,
) : LibraryWatcher {
    override fun start() {
        // A change that comes while a scan runs is kept, more of them count as one.
        val changed = Channel<Unit>(Channel.CONFLATED)
        scope.launch { source.changes().collect { changed.send(Unit) } }
        scope.launch {
            schedule.waitForChange()
            scanner.scanQuietly()
            while (true) {
                changed.receive()
                var waits = 0
                do {
                    delay(QUIET)
                    waits++
                } while (changed.tryReceive().isSuccess && waits < MOST_WAITS)
                scanner.scanQuietly()
            }
        }
    }

    private companion object {
        val QUIET = 2.seconds

        /** Changes that never stop must not hold a scan back for ever: after this many quiet times it runs. */
        const val MOST_WAITS = 15
    }
}
