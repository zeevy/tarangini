/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.widget

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import androidx.glance.appwidget.testing.unit.hasSendBroadcastAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasClickAction
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.media3.session.MediaButtonReceiver
import androidx.media3.session.MediaLibraryService
import com.siliconcircuits.tarangini.core.domain.playback.NowPlayingCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WidgetContentTest {
    private val context = RuntimeEnvironment.getApplication()
    private val service = ComponentName(context.packageName, "com.example.PlaybackService")
    private val playPause = WidgetActions.mediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        .setComponent(ComponentName(context, MediaButtonReceiver::class.java))
    private val song = NowPlayingCard("Bhairavi", "Ravi", "content://media/external/audio/media/1", showsPause = true)

    /** The app has its playback service, as the merged manifest of the app has it. */
    private fun withPlaybackService() {
        shadowOf(context.packageManager).apply {
            addServiceIfNotPresent(service)
            addIntentFilterForService(service, IntentFilter(MediaLibraryService.SERVICE_INTERFACE))
        }
    }

    @Test
    fun `the wide widget shows the song and both buttons`() = runGlanceAppWidgetUnitTest {
        withPlaybackService()
        setContext(context)
        setAppWidgetSize(NowPlayingWidget.WIDE)
        provideComposable { WidgetContent(song, cover = null, actions = WidgetActions.of(context)) }

        onNode(hasTextEqualTo("Bhairavi")).assertExists()
        onNode(hasTextEqualTo("Ravi")).assertExists()
        onNode(hasContentDescriptionEqualTo("Pause")).assert(hasSendBroadcastAction(playPause))
        // The key event in the intent has its own time, so the intent itself cannot be compared here.
        onNode(hasContentDescriptionEqualTo("Next")).assert(hasClickAction())
        assertNotNull(WidgetActions.of(context).next)
    }

    @Test
    fun `the small widget has the cover and the buttons, and no texts`() = runGlanceAppWidgetUnitTest {
        withPlaybackService()
        setContext(context)
        setAppWidgetSize(NowPlayingWidget.SMALL)
        val paused = song.copy(showsPause = false)
        provideComposable { WidgetContent(paused, cover = null, actions = WidgetActions.of(context)) }

        onNode(hasTextEqualTo("Bhairavi")).assertDoesNotExist()
        onNode(hasContentDescriptionEqualTo("Open Tarangini")).assertExists()
        onNode(hasContentDescriptionEqualTo("Play")).assert(hasClickAction())
        onNode(hasContentDescriptionEqualTo("Next")).assert(hasClickAction())
    }

    @Test
    fun `with nothing loaded play still plays, and there is no next`() = runGlanceAppWidgetUnitTest {
        withPlaybackService()
        setContext(context)
        setAppWidgetSize(NowPlayingWidget.WIDE)
        provideComposable { WidgetContent(card = null, cover = null, actions = WidgetActions.of(context)) }

        onNode(hasTextEqualTo("Nothing is playing")).assertExists()
        onNode(hasContentDescriptionEqualTo("Play")).assert(hasSendBroadcastAction(playPause))
        onNode(hasContentDescriptionEqualTo("Next")).assertDoesNotExist()
    }

    @Test
    fun `without next the widget has only play`() = runGlanceAppWidgetUnitTest {
        withPlaybackService()
        setContext(context)
        setAppWidgetSize(NowPlayingWidget.WIDE)
        provideComposable { WidgetContent(song, cover = null, actions = WidgetActions.of(context).withoutNext()) }

        onNode(hasContentDescriptionEqualTo("Pause")).assertExists()
        onNode(hasContentDescriptionEqualTo("Next")).assertDoesNotExist()
    }

    @Test
    fun `a song with no texts shows the words for unknown`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(NowPlayingWidget.WIDE)
        val unknown = NowPlayingCard(title = null, artist = null, songUri = null, showsPause = false)
        provideComposable { WidgetContent(unknown, cover = null, actions = WidgetActions.of(context)) }

        onNode(hasTextEqualTo("Unknown title")).assertExists()
    }

    @Test
    fun `without a playback service there is no next, and the two keys stay apart`() {
        assertNull(WidgetActions.of(context).next)

        val press = WidgetActions.mediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        val next = WidgetActions.mediaButton(KeyEvent.KEYCODE_MEDIA_NEXT)
        // Android takes two intents as one when they are equal by this test, and the second button would
        // then send the key of the first.
        assertNotEquals(true, press.filterEquals(next))
        assertEquals(
            KeyEvent.KEYCODE_MEDIA_NEXT,
            next.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)?.keyCode,
        )
        val pressed = press.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        assertEquals(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, pressed?.keyCode)
        assertEquals(KeyEvent.ACTION_DOWN, pressed?.action)
    }
}
