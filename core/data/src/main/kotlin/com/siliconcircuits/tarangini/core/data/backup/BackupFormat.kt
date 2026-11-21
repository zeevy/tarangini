/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.backup

import com.siliconcircuits.tarangini.core.domain.model.MAX_MIN_TRACK_SECONDS
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.model.checkPlaylistName
import com.siliconcircuits.tarangini.core.domain.text.isDirectionMark
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * What a backup holds. A song is the path of its file as the library saves it: the storage, the
 * folders and the file name, such as `external_primary/Music/Ragas/yaman.mp3`.
 */
internal data class BackupContent(
    val settings: BackupSettings? = null,
    val favourites: List<String> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList(),
)

/** The settings of the library in a backup. A value the file does not hold is null. */
internal data class BackupSettings(
    val minTrackSeconds: Int? = null,
    val hiddenFolders: List<String> = emptyList(),
    val songSort: SongSort? = null,
)

/** A playlist in a backup: its name and its songs in its order. */
internal data class BackupPlaylist(val name: String, val songs: List<String>)

/** What the text of a file turned out to be. */
internal sealed interface BackupRead {
    data class Read(val content: BackupContent) : BackupRead

    /** No backup of this app. */
    data object NotABackup : BackupRead

    /** A backup of a newer version of the app, which this version cannot read safely. */
    data object Newer : BackupRead
}

/**
 * The backup file: JSON with a name and a version number.
 *
 * A file that is read is not trusted. It may be anything the person picked, or a file someone made
 * to do harm. So every list has a limit, every text is cut and loses its control characters, a
 * value outside its range is left out, and a part that is not what it should be counts as missing.
 */
internal object BackupFormat {
    /** A larger file is refused before it is read. A backup of 50,000 favourites is well below this. */
    const val MAX_BYTES = 5 * 1024 * 1024

    /** The version this app writes, and the newest it reads. */
    const val VERSION = 1

    /** What the file names itself, so another JSON file is not taken for a backup. */
    const val NAME = "tarangini-backup"

    const val MAX_FAVOURITES = 50_000
    const val MAX_PLAYLISTS = 500
    const val MAX_PLAYLIST_SONGS = 5_000

    /** All playlists together. It keeps the search for the songs of a made up file short. */
    const val MAX_SONGS_IN_PLAYLISTS = 100_000
    const val MAX_HIDDEN_FOLDERS = 1_000
    private const val MAX_PATH = 4_096

    /** The text of a backup. */
    fun write(content: BackupContent, createdAt: Long): String = JSONObject()
        .put("format", NAME)
        .put("version", VERSION)
        .put("createdAt", createdAt)
        .put("settings", content.settings?.let(::settingsJson))
        .put("favourites", JSONArray(content.favourites))
        .put(
            "playlists",
            JSONArray(content.playlists.map { JSONObject().put("name", it.name).put("songs", JSONArray(it.songs)) }),
        )
        .toString()

    /** What [text] holds. */
    fun read(text: String): BackupRead {
        val file = try {
            JSONObject(text)
        } catch (_: JSONException) {
            null
        }
        val version = file?.optInt("version", 0) ?: 0
        return when {
            file == null || file.optString("format") != NAME || version < 1 -> BackupRead.NotABackup
            version > VERSION -> BackupRead.Newer
            else -> BackupRead.Read(contentOf(file))
        }
    }

    private fun settingsJson(settings: BackupSettings) = JSONObject()
        .put("minTrackSeconds", settings.minTrackSeconds)
        .put("hiddenFolders", JSONArray(settings.hiddenFolders))
        .put("songSort", settings.songSort?.name)

    private fun contentOf(file: JSONObject) = BackupContent(
        settings = file.optJSONObject("settings")?.let(::settingsOf),
        favourites = paths(file.optJSONArray("favourites"), MAX_FAVOURITES),
        playlists = playlistsOf(file.optJSONArray("playlists")),
    )

    /** The playlists with a name that can be saved. Together they hold at most [MAX_SONGS_IN_PLAYLISTS] songs. */
    private fun playlistsOf(list: JSONArray?): List<BackupPlaylist> {
        var left = MAX_SONGS_IN_PLAYLISTS
        return objects(list, MAX_PLAYLISTS).mapNotNull { playlist ->
            checkPlaylistName(clean(playlist.optString("name")))?.let { name ->
                val songs = paths(playlist.optJSONArray("songs"), minOf(MAX_PLAYLIST_SONGS, left))
                left -= songs.size
                BackupPlaylist(name, songs)
            }
        }
    }

    private fun settingsOf(settings: JSONObject) = BackupSettings(
        minTrackSeconds = settings.optInt("minTrackSeconds", -1).takeIf { it in 0..MAX_MIN_TRACK_SECONDS },
        hiddenFolders = paths(settings.optJSONArray("hiddenFolders"), MAX_HIDDEN_FOLDERS),
        songSort = SongSort.entries.firstOrNull { it.name == settings.optString("songSort") },
    )

    /** The texts of a list that can be a path, each once, and never more than [limit]. */
    private fun paths(list: JSONArray?, limit: Int): List<String> = (0 until minOf(list?.length() ?: 0, limit))
        .mapNotNull { index -> (list?.opt(index) as? String)?.let(::cleanPath) }
        .filter { it.isNotEmpty() && it.length <= MAX_PATH }

    private fun objects(list: JSONArray?, limit: Int): List<JSONObject> =
        (0 until minOf(list?.length() ?: 0, limit)).mapNotNull { index -> list?.optJSONObject(index) }

    /** No control character, and no character that changes the direction of the text of a name. */
    private fun clean(text: String): String = text.filterNot { it.isISOControl() || it.isDirectionMark() }.trim()

    /**
     * A path loses only its control characters. Every other character can be part of a file name,
     * and a path with one character less would not be found again.
     */
    private fun cleanPath(text: String): String = text.filterNot { it.isISOControl() }
}
