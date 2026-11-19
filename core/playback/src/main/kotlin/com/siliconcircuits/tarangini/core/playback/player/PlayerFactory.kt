/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Makes the player of the playback service. Nothing else in the app makes a player.
 *
 * The player asks for audio focus by itself. It pauses for a call and goes on after it, lets the
 * system lower its volume for a short prompt, and pauses for good when another app starts to play.
 * It also pauses when headphones are unplugged or a Bluetooth device goes away, so that the sound
 * never moves to the speaker by surprise.
 *
 * Album tracks play with no gap when the files say how much silence their encoder added.
 * [GaplessRenderersFactory] makes that right for the MP3 decoder of Android.
 */
// The player is built with our own renderers factory, which is API that may still change.
@OptIn(UnstableApi::class)
internal fun createPlayer(context: Context): ExoPlayer {
    val music = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()
    val player = ExoPlayer.Builder(context, GaplessRenderersFactory(context))
        .setAudioAttributes(music, true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(wakeModeFor(null))
        .build()
    player.addListener(WakeModeSwitch(player))
    return player
}
