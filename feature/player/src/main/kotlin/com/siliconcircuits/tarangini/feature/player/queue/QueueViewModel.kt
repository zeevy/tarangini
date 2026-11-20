/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The queue as the screen shows it.
 *
 * @property items the items in the order of the queue
 * @property currentIndex the place of the item that plays, or null when the queue is empty
 */
internal data class QueueUiState(val items: List<PlaybackItem> = emptyList(), val currentIndex: Int? = null)

/** The queue screen: what plays next, in an order the person can change. */
@HiltViewModel
internal class QueueViewModel @Inject constructor(playback: PlaybackController, private val queue: PlaybackQueue) :
    ViewModel() {
    val state: StateFlow<QueueUiState> = playback.state
        .map { QueueUiState(it.queue, it.currentIndex) }
        // The player reports its place several times a second with the same list. Comparing a list
        // of many thousand items each time would cost more than drawing it.
        .distinctUntilChanged { old, new -> old.items === new.items && old.currentIndex == new.currentIndex }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_AFTER_MS),
            QueueUiState(playback.state.value.queue, playback.state.value.currentIndex),
        )

    fun playAt(index: Int) = queue.playAt(index)

    fun move(from: Int, to: Int) = queue.move(from, to)

    fun remove(index: Int) = queue.remove(index)

    private companion object {
        const val STOP_AFTER_MS = 5_000L
    }
}
