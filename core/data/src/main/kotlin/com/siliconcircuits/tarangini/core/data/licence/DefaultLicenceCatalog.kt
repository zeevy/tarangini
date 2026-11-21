/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.licence

import android.content.Context
import com.siliconcircuits.tarangini.core.data.di.IoDispatcher
import com.siliconcircuits.tarangini.core.domain.model.LibraryLicence
import com.siliconcircuits.tarangini.core.domain.model.OpenSourceLibrary
import com.siliconcircuits.tarangini.core.domain.repository.LicenceCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Reads the list the licence check of the build puts into the app, the `NOTICE` files the build
 * collects from the libraries, and the licence texts that ship as assets. The font is not a
 * library of the build, so it is added here.
 */
internal class DefaultLicenceCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LicenceCatalog {
    override suspend fun libraries(): List<OpenSourceLibrary> = withContext(io) {
        // A build with no list still shows the font and the notice of the app.
        val notices = parseNotices(asset(NOTICES).orEmpty())
        (parseArtifacts(asset(ARTIFACTS).orEmpty(), notices) + FONT).sortedBy { it.name.lowercase() }
    }

    // The id becomes a part of a file name, so only the plain form of a licence id is read.
    override suspend fun text(licenceId: String): String? =
        if (LICENCE_ID.matches(licenceId)) withContext(io) { asset("$TEXTS/$licenceId.txt") } else null

    private fun asset(path: String): String? = try {
        context.assets.open(path).bufferedReader().use { it.readText() }
    } catch (_: IOException) {
        null
    }

    private companion object {
        const val ARTIFACTS = "app/cash/licensee/artifacts.json"
        const val NOTICES = "licences/notices.json"
        const val TEXTS = "licences"
        val LICENCE_ID = Regex("[A-Za-z0-9][A-Za-z0-9.+-]{0,39}")
        val FONT = OpenSourceLibrary("Manrope", "", LibraryLicence("Manrope-OFL", "SIL Open Font License 1.1"))
    }
}

/**
 * The libraries in the list of the licence check. A library with more than one licence is listed
 * under its first one. An entry that cannot be read is left out, and a list that is no list gives nothing.
 */
internal fun parseArtifacts(json: String, notices: Map<String, String> = emptyMap()): List<OpenSourceLibrary> = try {
    val artifacts = JSONArray(json)
    (0 until artifacts.length()).mapNotNull { index -> artifacts.optJSONObject(index)?.toLibrary(notices) }
} catch (_: JSONException) {
    emptyList()
}

/**
 * The `NOTICE` files the build collected, by the `group:name` of the library. The text comes from
 * a file of a library, so it loses its control characters and is cut. A list that is no list gives nothing.
 */
internal fun parseNotices(json: String): Map<String, String> = try {
    val notices = JSONArray(json)
    (0 until notices.length()).mapNotNull { index ->
        val entry = notices.optJSONObject(index)
        val text = entry?.optString("notice").orEmpty()
            .filter { it == '\n' || !it.isISOControl() }
            .trim()
            .take(MAX_NOTICE)
        if (entry == null || text.isEmpty()) null else entry.coordinates() to text
    }.toMap()
} catch (_: JSONException) {
    emptyMap()
}

private const val MAX_NOTICE = 64 * 1024

private fun JSONObject.coordinates() = optString("groupId") + ":" + optString("artifactId")

private fun JSONObject.toLibrary(notices: Map<String, String>): OpenSourceLibrary? {
    val licence = optJSONArray("spdxLicenses")?.optJSONObject(0)
    val id = licence?.optString("identifier").orEmpty()
    val coordinates = coordinates()
    return OpenSourceLibrary(
        name = optString("name").ifEmpty { coordinates },
        version = optString("version"),
        licence = LibraryLicence(id, licence?.optString("name").orEmpty().ifEmpty { id }),
        notice = notices[coordinates],
    ).takeIf { id.isNotEmpty() }
}
