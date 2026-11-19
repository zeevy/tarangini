/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer

/**
 * Makes the player of the playback service. Nothing else in the app makes a player.
 *
 * The player asks for audio focus by itself. It pauses for a call and goes on after it, lets the
 * system lower its volume for a short prompt, and pauses for good when another app starts to play.
 * It also pauses when headphones are unplugged or a Bluetooth device goes away, so that the sound
 * never moves to the speaker by surprise.
 */
internal fun createPlayer(context: Context): ExoPlayer {
    val music = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()
    val player = ExoPlayer.Builder(context)
        .setAudioAttributes(music, true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(wakeModeFor(null))
        .build()
    player.addListener(WakeModeSwitch(player))
    return player
}
