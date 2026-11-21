/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.siliconcircuits.tarangini.core.domain.model.SleepState
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.AddToPlaylistDialog
import com.siliconcircuits.tarangini.core.ui.component.PlaylistPick
import com.siliconcircuits.tarangini.core.ui.component.formatDuration
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.feature.player.R

/** One line to pick in a dialog. */
private class Choice(val label: String, val isPicked: Boolean = false, val onPick: () -> Unit)

/**
 * The menu of the top bar: add the song to a playlist, the speed and the sleep timer. The lines of
 * the speed and the timer say what is set. A live stream has no speed to set, and an item that is
 * no song of the library cannot go into a playlist.
 */
@Composable
internal fun PlaybackOptionsMenu(state: NowPlayingUiState, actions: PlaybackOptionActions, playlists: PlaylistPick?) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    var asking by rememberSaveable { mutableStateOf<String?>(null) }
    Box {
        IconButton(onClick = { isOpen = true }) {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_more),
                contentDescription = stringResource(R.string.now_playing_options),
            )
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            if (state.songId != null && playlists != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(CoreUiR.string.song_add_to_playlist)) },
                    onClick = {
                        isOpen = false
                        asking = PLAYLIST
                    },
                )
            }
            if (!state.isLive) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.now_playing_speed, speedText(state.speed))) },
                    onClick = {
                        isOpen = false
                        asking = SPEED
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(sleepText(state.sleep)) },
                onClick = {
                    isOpen = false
                    asking = SLEEP
                },
            )
        }
    }
    when (asking) {
        PLAYLIST -> if (state.songId != null && playlists != null) {
            AddToPlaylistDialog(playlists, state.songId) { asking = null }
        }

        SPEED -> ChoiceDialog(stringResource(R.string.now_playing_speed_title), speedChoices(state.speed, actions)) {
            asking = null
        }

        SLEEP -> ChoiceDialog(stringResource(R.string.now_playing_sleep), sleepChoices(state.sleep, actions)) {
            asking = null
        }
    }
}

@Composable
private fun speedChoices(now: Float, actions: PlaybackOptionActions): List<Choice> = SPEEDS.map { speed ->
    Choice(speedText(speed), isPicked = speed == now) { actions.onSetSpeed(speed) }
}

@Composable
private fun sleepChoices(now: SleepState, actions: PlaybackOptionActions): List<Choice> {
    val times = SLEEP_MINUTES.map { minutes ->
        Choice(stringResource(R.string.now_playing_sleep_minutes, minutes)) { actions.onSleepAfter(minutes) }
    }
    val atEnd =
        Choice(stringResource(R.string.now_playing_sleep_end), now == SleepState.EndOfTrack, actions.onSleepAtEnd)
    val off = Choice(stringResource(R.string.now_playing_sleep_off), onPick = actions.onCancelSleep)
    return times + atEnd + listOfNotNull(off.takeIf { now != SleepState.Off })
}

@Composable
private fun speedText(speed: Float): String =
    stringResource(R.string.now_playing_speed_value, speed.toString().removeSuffix(".0"))

@Composable
private fun sleepText(sleep: SleepState): String = when (sleep) {
    SleepState.Off -> stringResource(R.string.now_playing_sleep)
    is SleepState.Timed -> stringResource(R.string.now_playing_sleep_in, formatDuration(sleep.leftMs))
    SleepState.EndOfTrack -> stringResource(R.string.now_playing_sleep_at_end)
}

/** A short list to pick one line from. A pick closes the dialog. */
@Composable
private fun ChoiceDialog(title: String, choices: List<Choice>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // One group, so a screen reader says which of how many lines it is on.
            Column(modifier = Modifier.selectableGroup()) {
                choices.forEach { choice ->
                    Text(
                        text = choice.label,
                        // The line that is set now stands out by its colour.
                        color = if (choice.isPicked) MaterialTheme.colorScheme.primary else Color.Unspecified,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = choice.isPicked, role = Role.RadioButton) {
                                choice.onPick()
                                onDismiss()
                            }
                            .padding(vertical = Spacing.medium),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.action_cancel)) } },
    )
}

private const val PLAYLIST = "playlist"
private const val SPEED = "speed"
private const val SLEEP = "sleep"
private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private val SLEEP_MINUTES = listOf(15, 30, 45, 60)
