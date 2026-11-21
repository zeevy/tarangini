/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing

import java.io.File
import org.junit.Assert.assertTrue

/**
 * The saved picture [name] of a screenshot test, in `src/test/screenshots` of the module under
 * test. Without a saved picture a compare would pass with nothing to compare with, so the test
 * fails here unless the run records new pictures.
 */
fun savedPicture(name: String): File {
    val picture = File("src/test/screenshots/$name.png")
    assertTrue("No saved picture $picture. Run the tests with -PrecordScreenshots.", isRecording() || picture.exists())
    return picture
}

/** True when the run saves new pictures (`-PrecordScreenshots`). */
fun isRecording(): Boolean = System.getProperty("roborazzi.test.record").toBoolean()
