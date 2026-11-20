/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.annotation.ExperimentalCoilApi
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import java.io.File
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// The test library of Coil and the call that swaps the loader are marked as not final. They are used only here.
@OptIn(ExperimentalCoilApi::class, DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h200dp-xhdpi")
class AlbumArtScreenshotTest {
    @Before
    fun useAFakeLoader() {
        // One address gives a plain picture at once. No other address gives one, as for a song with no picture.
        val engine = FakeImageLoaderEngine.Builder().intercept(
            WITH_PICTURE,
            ColorImage(Color(PICTURE).toArgb()),
        ).build()
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(ApplicationProvider.getApplicationContext()).components { add(engine) }.build(),
        )
    }

    @After
    fun forgetTheLoader() = SingletonImageLoader.reset()

    @Test
    fun `a picture, no picture and no address, light`() = capture("album_art_light", darkTheme = false)

    @Test
    fun `a picture, no picture and no address, dark`() = capture("album_art_dark", darkTheme = true)

    private fun capture(name: String, darkTheme: Boolean) {
        val picture = File("src/test/screenshots/$name.png")
        // Without this, a test with no saved picture passes: there is nothing to compare with.
        assertTrue("No saved picture $picture. Run the tests with -PrecordScreenshots.", recording || picture.exists())
        captureRoboImage(picture.path) {
            TaranginiTheme(darkTheme = darkTheme, dynamicColor = false) {
                Surface {
                    Row(Modifier.padding(Spacing.medium), Arrangement.spacedBy(Spacing.medium)) {
                        AlbumArt(WITH_PICTURE)
                        AlbumArt(NO_PICTURE)
                        AlbumArt(null)
                        AlbumArt(WITH_PICTURE, size = LARGE)
                    }
                }
            }
        }
    }
}

private const val WITH_PICTURE = "content://media/external/audio/media/1/albumart"
private const val NO_PICTURE = "content://media/external/audio/media/2/albumart"
private const val PICTURE = 0xFFC83C14
private val LARGE = 144.dp
private val recording = System.getProperty("roborazzi.test.record").toBoolean()
