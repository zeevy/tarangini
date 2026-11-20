/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.station

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationInputTest {
    @Test
    fun `complete http and https addresses pass`() {
        listOf(
            "http://radio.example/stream",
            "https://radio.example:8443/live.m3u8?token=a#x",
            "HTTPS://Radio.Example",
            "  https://radio.example/a.pls  ",
            "http://192.168.1.10:8000/",
            "http://[2001:db8::1]:8000/stream",
            "https://రేడియో.example/stream",
        ).forEach { assertNull(it, checkStreamAddress(it)) }
    }

    @Test
    fun `every other scheme is refused`() {
        listOf(
            "file:///sdcard/Music/a.mp3",
            "content://media/external/audio/media/1",
            "ftp://radio.example/stream",
            "rtsp://radio.example/stream",
            "javascript:alert(1)",
            "data:audio/mpeg;base64,AAAA",
            "intent://radio.example#Intent;end",
            "radio.example/stream",
            "http:/radio.example",
            "http//radio.example",
            "://radio.example",
            "xhttp://radio.example",
        ).forEach { assertEquals(it, AddressFault.NOT_HTTP, checkStreamAddress(it)) }
    }

    @Test
    fun `spaces, line breaks, control characters and backslashes are refused`() {
        listOf(
            "http://radio.example/my stream",
            "http://radio.example/a\nHost: other.example",
            "http://radio.example/\u0000",
            "http://radio.example/a\u00A0b",
            "http://radio.example\\@other.example/",
        ).forEach { assertEquals(it, AddressFault.ODD_CHARACTERS, checkStreamAddress(it)) }
    }

    @Test
    fun `an address with no host or with a login is refused`() {
        assertEquals(AddressFault.NO_HOST, checkStreamAddress("http://"))
        assertEquals(AddressFault.NO_HOST, checkStreamAddress("https:///stream"))
        assertEquals(AddressFault.NO_HOST, checkStreamAddress("http://:8000/stream"))
        listOf(
            "http://host:80:80/",
            "http://host:abc/",
            "http://host:99999/",
            "http://host:0/",
            "http://host:/",
            "http://[::1/",
            "http://[]/",
        )
            .forEach { assertEquals(it, AddressFault.NO_HOST, checkStreamAddress(it)) }
        assertEquals(AddressFault.HAS_LOGIN, checkStreamAddress("http://me:secret@radio.example/"))
        assertEquals(AddressFault.HAS_LOGIN, checkStreamAddress("http://radio.example@other.example/"))
    }

    @Test
    fun `nothing and a very long address are refused`() {
        assertEquals(AddressFault.EMPTY, checkStreamAddress(""))
        assertEquals(AddressFault.EMPTY, checkStreamAddress("   "))
        assertNull(checkStreamAddress("http://radio.example/" + "a".repeat(MAX_STREAM_ADDRESS - 21)))
        assertEquals(
            AddressFault.TOO_LONG,
            checkStreamAddress("http://radio.example/" + "a".repeat(MAX_STREAM_ADDRESS)),
        )
        assertEquals(AddressFault.TOO_LONG, checkStreamAddress("a".repeat(1_000_000)))
    }

    @Test
    fun `a name needs something to read and has a longest length`() {
        assertNull(checkStationName("Radio Mirchi 98.3"))
        assertNull(checkStationName("రేడియో"))
        assertNull(checkStationName("a".repeat(MAX_STATION_NAME)))
        assertEquals(NameFault.EMPTY, checkStationName(""))
        assertEquals(NameFault.EMPTY, checkStationName("  \n "))
        assertEquals(NameFault.EMPTY, checkStationName("\u200B\u200B"))
        assertEquals(NameFault.EMPTY, checkStationName(" - "))
        assertEquals(NameFault.TOO_LONG, checkStationName("a".repeat(MAX_STATION_NAME + 1)))
    }

    @Test
    fun `a name is stored with single spaces and none at the ends`() {
        assertEquals("Radio City", cleanStationName("  Radio \t City\n"))
        assertEquals("Radio oidaR", cleanStationName("Radio \u202EoidaR\u202C\u200B"))
        assertEquals("క్\u200Cష", cleanStationName("క్\u200Cష"))
    }
}
