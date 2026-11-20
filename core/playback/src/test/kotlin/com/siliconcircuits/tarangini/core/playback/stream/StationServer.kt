/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest

/**
 * A local server with one station of each kind, and playlist files that are wrong in one way each.
 * The body of a stream is its own name, so a test sees which stream was opened.
 */
internal fun stationServer(): MockWebServer = MockWebServer().apply {
    dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = answerFor(request.url.encodedPath)
    }
    start()
}

private const val BIGGER_THAN_THE_LIMIT = 70_000

private fun answerFor(path: String): MockResponse =
    answers[path]?.invoke() ?: MockResponse.Builder().code(NOT_FOUND).build()

private val answers: Map<String, () -> MockResponse> = mapOf(
    "/icecast.mp3" to { stream("icecast", "audio/mpeg").addHeader("icy-name", "Test station").build() },
    // An old Shoutcast server answers with "ICY" where "HTTP/1.0" belongs.
    "/shoutcast" to { stream("shoutcast", "audio/mpeg").status("ICY 200 OK").build() },
    "/radio.aac" to { stream("aac", "audio/aac").build() },
    "/live.m3u8" to { stream("#EXTM3U", "application/vnd.apple.mpegurl").build() },
    "/station.pls" to {
        text("[playlist]\nNumberOfEntries=1\nFile1=icecast.mp3\nTitle1=Test\nLength1=-1\nVersion=2\n")
    },
    "/station.m3u" to { text("#EXTM3U\n#EXTINF:-1,Test station\nradio.aac\n") },
    "/two-steps.m3u" to { text("station.pls\n") },
    "/self.pls" to { text("File1=self.pls\n") },
    "/ring-a.m3u" to { text("ring-b.m3u\n") },
    "/ring-b.m3u" to { text("ring-a.m3u\n") },
    "/deep-1.m3u" to { text("deep-2.m3u\n") },
    "/deep-2.m3u" to { text("deep-3.m3u\n") },
    "/deep-3.m3u" to { text("deep-4.m3u\n") },
    "/deep-4.m3u" to { text("icecast.mp3\n") },
    "/big.pls" to { text("#".repeat(BIGGER_THAN_THE_LIMIT) + "\nFile1=icecast.mp3\n") },
    "/moved.pls" to { redirect("/station.pls") },
    "/moves-for-ever.pls" to { redirect("/moves-for-ever.pls") },
    "/moved-to-a-file.pls" to { redirect("file:///data/local/tmp/a.mp3") },
    "/files-only.m3u" to { text("file:///sdcard/Music/a.mp3\ncontent://media/external/audio/media/1\n") },
    "/file-first.m3u" to { text("file:///sdcard/Music/a.mp3\nicecast.mp3\n") },
    "/empty.pls" to { text("[playlist]\nNumberOfEntries=0\n") },
)

private const val NOT_FOUND = 404
private const val FOUND = 302

private fun stream(body: String, type: String) = MockResponse.Builder().addHeader("Content-Type", type).body(body)

private fun text(body: String) = MockResponse.Builder().addHeader("Content-Type", "audio/x-scpls").body(body).build()

private fun redirect(to: String) = MockResponse.Builder().code(FOUND).addHeader("Location", to).build()
