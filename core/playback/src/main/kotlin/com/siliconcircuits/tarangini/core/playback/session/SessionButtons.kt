/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.siliconcircuits.tarangini.core.playback.R

/** What a press of the shuffle or the repeat button does next. The name of a button says this. */
internal enum class ModeStep { SHUFFLE_ON, SHUFFLE_OFF, REPEAT_ALL, REPEAT_ONE, REPEAT_OFF }

/**
 * The buttons next to the player buttons of the car, the notification and the lock screen: the
 * heart, shuffle and repeat. Shuffle and repeat show the state of the player and a press changes
 * it, so they are the same switches as on the now playing screen of the phone. While nothing is
 * loaded there is no button.
 *
 * @param label the name of a mode button: what a press does
 * @param show takes the buttons to show, after every change
 */
// The buttons of a session are API of Media3 that may still change.
@OptIn(UnstableApi::class)
internal class SessionButtons(
    private val player: Player,
    private val label: (ModeStep) -> String,
    private val show: (List<CommandButton>) -> Unit,
) : Player.Listener {
    private var heart: List<CommandButton> = emptyList()
    private var shown: List<CommandButton>? = null

    /** Takes the heart button, or an empty list when the item that plays has none. */
    fun heart(buttons: List<CommandButton>) {
        heart = buttons
        refresh()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = refresh()

    override fun onRepeatModeChanged(repeatMode: Int) = refresh()

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refresh()

    /**
     * A press of a mode button. False when [action] is not one of them. A press while nothing is
     * loaded changes nothing: it comes from a button that is no longer shown.
     */
    fun press(action: String): Boolean = when {
        action != SHUFFLE.customAction && action != REPEAT.customAction -> false

        player.currentMediaItem == null -> true

        action == SHUFFLE.customAction -> {
            player.shuffleModeEnabled = !player.shuffleModeEnabled
            true
        }

        else -> {
            player.repeatMode = repeatAfter(player.repeatMode)
            true
        }
    }

    /** Hands on the buttons only when they changed: every call makes the notification again. */
    private fun refresh() {
        val now = if (player.currentMediaItem == null) emptyList() else heart + modes()
        if (now != shown) {
            shown = now
            show(now)
        }
    }

    private fun modes(): List<CommandButton> {
        val shuffle = if (player.shuffleModeEnabled) {
            button(CommandButton.ICON_SHUFFLE_ON, ModeStep.SHUFFLE_OFF, SHUFFLE)
        } else {
            button(CommandButton.ICON_SHUFFLE_OFF, ModeStep.SHUFFLE_ON, SHUFFLE)
        }
        val repeat = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> button(CommandButton.ICON_REPEAT_ALL, ModeStep.REPEAT_ONE, REPEAT)
            Player.REPEAT_MODE_ONE -> button(CommandButton.ICON_REPEAT_ONE, ModeStep.REPEAT_OFF, REPEAT)
            else -> button(CommandButton.ICON_REPEAT_OFF, ModeStep.REPEAT_ALL, REPEAT)
        }
        return listOf(shuffle, repeat)
    }

    private fun button(icon: Int, next: ModeStep, command: SessionCommand): CommandButton =
        CommandButton.Builder(icon).setDisplayName(label(next)).setSessionCommand(command).build()

    companion object {
        /** Switches shuffle on or off. It carries no value. */
        val SHUFFLE = SessionCommand("com.siliconcircuits.tarangini.TOGGLE_SHUFFLE", Bundle.EMPTY)

        /** Goes to the next repeat mode: off, all, one, off. It carries no value. */
        val REPEAT = SessionCommand("com.siliconcircuits.tarangini.NEXT_REPEAT", Bundle.EMPTY)

        /** Every command of a button of this app. */
        val COMMANDS = listOf(FavouriteButton.COMMAND, SHUFFLE, REPEAT)
    }
}

/** The repeat mode a press of the repeat button leads to. */
private fun repeatAfter(mode: Int): Int = when (mode) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

/** The names of the mode buttons from the string resources of the app. */
internal fun modeLabels(context: Context): (ModeStep) -> String = { step ->
    context.getString(
        when (step) {
            ModeStep.SHUFFLE_ON -> R.string.car_shuffle_on
            ModeStep.SHUFFLE_OFF -> R.string.car_shuffle_off
            ModeStep.REPEAT_ALL -> R.string.car_repeat_all
            ModeStep.REPEAT_ONE -> R.string.car_repeat_one
            ModeStep.REPEAT_OFF -> R.string.car_repeat_off
        },
    )
}
