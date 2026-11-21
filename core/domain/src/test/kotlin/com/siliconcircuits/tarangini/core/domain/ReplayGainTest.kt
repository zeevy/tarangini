/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain

import com.siliconcircuits.tarangini.core.domain.audio.ReplayGain
import com.siliconcircuits.tarangini.core.domain.audio.VolumeLevelling
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplayGainTest {
    private val tagged = ReplayGain(trackGainDb = -6f, trackPeak = 0.9f, albumGainDb = -4f, albumPeak = 1f)

    @Test
    fun `off plays every song as it is`() {
        assertEquals(0f, tagged.levelDb(VolumeLevelling.OFF), 0f)
    }

    @Test
    fun `track and album take their own gain`() {
        assertEquals(-6f, tagged.levelDb(VolumeLevelling.TRACK), 0f)
        assertEquals(-4f, tagged.levelDb(VolumeLevelling.ALBUM), 0f)
    }

    @Test
    fun `with no tag of the asked kind the other kind is used, and with none the song stays`() {
        assertEquals(-6f, ReplayGain(trackGainDb = -6f).levelDb(VolumeLevelling.ALBUM), 0f)
        assertEquals(-4f, ReplayGain(albumGainDb = -4f).levelDb(VolumeLevelling.TRACK), 0f)
        assertEquals(0f, ReplayGain().levelDb(VolumeLevelling.TRACK), 0f)
    }

    @Test
    fun `a lift stops where the loudest sample reaches full scale`() {
        // A peak of 0.5 leaves 6.02 dB of room.
        assertEquals(6.02f, ReplayGain(trackGainDb = 9f, trackPeak = 0.5f).levelDb(VolumeLevelling.TRACK), 0.01f)
        assertEquals(3f, ReplayGain(trackGainDb = 3f, trackPeak = 0.5f).levelDb(VolumeLevelling.TRACK), 0f)
    }

    @Test
    fun `with no peak, or a peak at full scale, a song is never lifted`() {
        assertEquals(0f, ReplayGain(trackGainDb = 5f).levelDb(VolumeLevelling.TRACK), 0f)
        assertEquals(0f, ReplayGain(trackGainDb = 5f, trackPeak = 1.2f).levelDb(VolumeLevelling.TRACK), 0f)
    }

    @Test
    fun `a gain out of range is held at the limit`() {
        assertEquals(-24f, ReplayGain(trackGainDb = -80f).levelDb(VolumeLevelling.TRACK), 0f)
        assertEquals(12f, ReplayGain(trackGainDb = 40f, trackPeak = 0.01f).levelDb(VolumeLevelling.TRACK), 0f)
    }

    @Test
    fun `tag texts are read with and without a unit and a sign`() {
        assertEquals(-6.54f, ReplayGain.gainOf("-6.54 dB")!!, 0f)
        assertEquals(2.1f, ReplayGain.gainOf(" +2.10 dB")!!, 0f)
        assertEquals(0.988525f, ReplayGain.peakOf("0.988525")!!, 0f)
        assertEquals(0.5f, ReplayGain.peakOf(".5")!!, 0f)
        assertEquals(-6.5f, ReplayGain.gainOf("-6,5 dB")!!, 0f)
    }

    @Test
    fun `a text with no number, or a peak of 0, is no tag`() {
        assertNull(ReplayGain.gainOf("loud"))
        assertNull(ReplayGain.gainOf(""))
        assertNull(ReplayGain.gainOf(null))
        assertNull(ReplayGain.gainOf("NaN dB"))
        assertNull(ReplayGain.peakOf("0"))
        assertNull(ReplayGain.peakOf("-0.5"))
    }
}
