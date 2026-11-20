/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo

/**
 * Puts the song title that a station sends into the item that plays, so the screens, the
 * notification, the lock screen and the car all show it.
 *
 * Many stations send the title of the song inside the stream (ICY metadata). While a song title is
 * known, it is the title and the name of the station is the line under it. With no song title the
 * name of the station is the title.
 *
 * The name of the station is the one the item came with. An item with no name gets the name the
 * server sends, and when the server sends none, the host of the address.
 *
 * Only a stream is ever touched. A file of the phone has texts too, and they come from the library.
 * The texts of a stream come from a server nobody here knows. [cleanStreamText] makes each one
 * safe to show, and a stream never stays without a title: the player would then show the text of
 * the server as it came.
 */
// The classes that carry ICY metadata are API of Media3 that may still change.
@OptIn(UnstableApi::class)
internal class StationTitles(private val player: Player) : Player.Listener {
    override fun onMetadata(metadata: Metadata) {
        val sent = (0 until metadata.length()).map(metadata::get).filterIsInstance<IcyInfo>().firstOrNull() ?: return
        show { cleanStreamText(sent.title) }
    }

    /** The name the server sends comes with the first answer, and a new item comes with no title at all. */
    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = show { shown -> shown.song() }

    /** Writes the texts of the current item again, with the song that [songFor] gives for the texts it has now. */
    private fun show(songFor: (MediaMetadata) -> String?) {
        val item = player.currentMediaItem ?: return
        val address = item.localConfiguration?.uri?.takeIf { it.isStreamAddress() } ?: return
        val old = item.mediaMetadata
        val station =
            old.station?.toString() ?: cleanStreamText(player.mediaMetadata.station?.toString()) ?: address.host
        val song = songFor(old)
        val new = old.buildUpon().setTitle(song ?: station).setArtist(station.takeIf { song != null }).build()
        // The same title comes again every few seconds. Only a change is told to the player.
        if (new != old) {
            player.replaceMediaItem(player.currentMediaItemIndex, item.buildUpon().setMediaMetadata(new).build())
        }
    }

    /** The song title that is shown now. With no artist line the title is the name of the station. */
    private fun MediaMetadata.song(): String? = title?.toString().takeIf { artist != null }
}

/**
 * A text from a stream in a form that is safe to show, or null when nothing useful is left.
 *
 * Control characters, the marks of a wrong encoding and the characters that turn the direction of
 * the text around are taken out, and white space is made one space. A text with no letter and no
 * digit, such as " - ", counts as no text. A long text is cut: the text goes to the notification
 * and to the car.
 */
internal fun cleanStreamText(raw: String?): String? {
    val text = raw.orEmpty()
        .filterNot { it.isISOControl() && !it.isWhitespace() }
        .filterNot { it == WRONG_ENCODING_MARK || it in directionMarks }
        .map { if (it.isWhitespace()) ' ' else it }
        .joinToString("")
        .replace(spaces, " ")
        .trim()
    // A cut between the two halves of one character would leave half a character.
    val end = if (text.length > MOST_SHOWN && text[MOST_SHOWN - 1].isHighSurrogate()) MOST_SHOWN - 1 else MOST_SHOWN
    return text.take(end).trimEnd().takeIf { shown -> shown.any { it.isLetterOrDigit() } }
}

private const val MOST_SHOWN = 200
private const val WRONG_ENCODING_MARK = '\uFFFD'

// With these a title could show the texts around it backwards. The joiners that Indian scripts need stay.
private val directionMarks = "\u061C\u200E\u200F\u202A\u202B\u202C\u202D\u202E\u2066\u2067\u2068\u2069".toSet()
private val spaces = Regex(" +")
