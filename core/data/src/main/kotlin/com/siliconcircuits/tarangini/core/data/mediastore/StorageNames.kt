/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import android.content.Context
import android.os.storage.StorageManager
import android.provider.MediaStore
import com.siliconcircuits.tarangini.core.data.R
import com.siliconcircuits.tarangini.core.data.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Gives a storage of the phone a name a person knows. */
internal fun interface StorageNames {
    /**
     * The name to show for a storage.
     *
     * @param volume the name MediaStore gives the storage: `external_primary` for the built-in
     *   one, a code such as `1a2b-3c4d` for a memory card
     */
    suspend fun nameOf(volume: String): String
}

/**
 * The names from Android. The built-in storage is "Phone storage". A memory card or a USB drive
 * gets the name Android shows for it, or "Memory card" when Android has none or the storage is
 * not in the phone right now.
 */
internal class DefaultStorageNames @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : StorageNames {
    // The list of storages is asked from the system, which can take a moment.
    override suspend fun nameOf(volume: String): String = withContext(io) {
        if (volume.equals(MediaStore.VOLUME_EXTERNAL_PRIMARY, ignoreCase = true)) {
            context.getString(R.string.storage_phone)
        } else {
            describedByAndroid(volume) ?: context.getString(R.string.storage_card)
        }
    }

    private fun describedByAndroid(volume: String): String? = context.getSystemService(StorageManager::class.java)
        ?.storageVolumes
        ?.firstOrNull { it.mediaStoreVolumeName.equals(volume, ignoreCase = true) }
        ?.getDescription(context)
        ?.takeIf { it.isNotBlank() }
}
