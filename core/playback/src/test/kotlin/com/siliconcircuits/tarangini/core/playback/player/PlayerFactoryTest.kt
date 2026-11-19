/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

// The playback thread of the player is read through API that Media3 may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class PlayerFactoryTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val audio = shadowOf(context.getSystemService(AudioManager::class.java))
    private val player: ExoPlayer = createPlayer(context)

    @After
    fun releasePlayer() {
        player.release()
    }

    private fun settle() {
        repeat(SETTLE_ROUNDS) {
            shadowOf(player.playbackLooper).idle()
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `the audio of the player goes through the renderer that sets the silence for the decoder`() {
        val renderers = List(player.rendererCount) { player.getRenderer(it) }

        assertEquals(1, renderers.count { it is GaplessAudioRenderer })
        assertEquals(1, renderers.count { it.trackType == C.TRACK_TYPE_AUDIO })
    }

    @Test
    fun `the player says it plays music as media`() {
        assertEquals(C.USAGE_MEDIA, player.audioAttributes.usage)
        assertEquals(C.AUDIO_CONTENT_TYPE_MUSIC, player.audioAttributes.contentType)
    }

    @Test
    fun `the player asks for audio focus when it starts to play, and not before`() {
        player.setMediaItem(MediaItem.fromUri("content://media/external/audio/media/7"))
        player.prepare()
        settle()
        assertNull(audio.lastAudioFocusRequest)

        player.play()
        settle()

        val request = requireNotNull(audio.lastAudioFocusRequest)
        assertEquals(AudioManager.AUDIOFOCUS_GAIN, request.durationHint)
    }

    @Test
    fun `unplugging the headphones pauses the player`() {
        player.setMediaItem(MediaItem.fromUri("content://media/external/audio/media/7"))
        player.prepare()
        player.play()
        settle()
        assertTrue(player.playWhenReady)

        context.sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        settle()

        assertFalse(player.playWhenReady)
    }

    @Test
    fun `a short loss of audio focus holds the player back and it goes on when the focus returns`() {
        val focus = playAndTakeFocusListener()

        focus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        settle()
        assertTrue(player.playWhenReady)
        assertEquals(Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS, player.playbackSuppressionReason)

        focus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        settle()
        assertTrue(player.playWhenReady)
        assertEquals(Player.PLAYBACK_SUPPRESSION_REASON_NONE, player.playbackSuppressionReason)
    }

    @Test
    fun `losing audio focus to another player pauses for good`() {
        val focus = playAndTakeFocusListener()

        focus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        settle()

        assertFalse(player.playWhenReady)
    }

    private fun playAndTakeFocusListener(): AudioManager.OnAudioFocusChangeListener {
        player.setMediaItem(MediaItem.fromUri("content://media/external/audio/media/7"))
        player.prepare()
        player.play()
        settle()
        return requireNotNull(audio.lastAudioFocusRequest).listener
    }

    private companion object {
        const val SETTLE_ROUNDS = 5
    }
}
