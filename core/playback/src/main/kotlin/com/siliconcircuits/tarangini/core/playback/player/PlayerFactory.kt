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
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.siliconcircuits.tarangini.core.playback.stream.StationTitles
import com.siliconcircuits.tarangini.core.playback.stream.StreamRetryPolicy
import com.siliconcircuits.tarangini.core.playback.stream.streamDataSourceFactory
import okhttp3.OkHttpClient

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
 *
 * Radio streams (Icecast, Shoutcast, HLS and playlist files) play through the same player. They
 * are read with [streamDataSourceFactory], and [StationTitles] shows the song title a station sends.
 * [StreamRetryPolicy] says how long a stream is tried again when the network drops.
 *
 * @param httpClient gives the shared HTTP client when the first stream is opened
 * @param audioProcessors run on the sound before it is played, such as the equalizer
 * @param onSongStarts called on the playback thread when the first sound of a song goes to the processors
 * @param crossfadeUs the length of the fade into the next song, read on the playback thread; 0 for none
 */
// The player is built with our own renderers factory, which is API that may still change.
@OptIn(UnstableApi::class)
internal fun createPlayer(
    context: Context,
    sound: LastSound = LastSound(),
    audioProcessors: List<AudioProcessor> = emptyList(),
    onSongStarts: SongStarts = SongStarts { _, _ -> },
    crossfadeUs: () -> Long = { 0L },
    httpClient: () -> OkHttpClient,
): ExoPlayer {
    val music = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()
    val retry = StreamRetryPolicy()
    val player = ExoPlayer.Builder(
        context,
        GaplessRenderersFactory(context, sound, audioProcessors, onSongStarts, crossfadeUs),
    )
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(streamDataSourceFactory(context, httpClient))
                .setLoadErrorHandlingPolicy(retry),
        )
        .setAudioAttributes(music, true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(wakeModeFor(null))
        .build()
    player.addListener(WakeModeSwitch(player))
    player.addListener(SkipMissingFile(player))
    player.addListener(StationTitles(player))
    player.addListener(retry)
    return player
}
