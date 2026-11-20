/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.permission

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.siliconcircuits.tarangini.feature.library.R

/**
 * Shows [content] when the app may read audio files, and [AudioPermissionScreen] when it may not.
 *
 * The permission is read again each time the screen comes back to the front, so a change made in
 * the app settings shows at once.
 *
 */
@Composable
internal fun AudioPermissionGate(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.library_title),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val permission = remember { audioPermissionFor(Build.VERSION.SDK_INT) }

    fun readGranted() = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    fun readCanAskAgain() = activity?.shouldShowRequestPermissionRationale(permission) == true

    var granted by remember { mutableStateOf(readGranted()) }
    var canAskAgain by remember { mutableStateOf(readCanAskAgain()) }
    var silentRefusals by rememberSaveable { mutableIntStateOf(0) }

    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        val couldAskAgain = canAskAgain
        granted = allowed
        canAskAgain = readCanAskAgain()
        silentRefusals = if (allowed) 0 else silentRefusalsAfter(silentRefusals, couldAskAgain, canAskAgain)
    }
    LifecycleResumeEffect(permission) {
        granted = readGranted()
        canAskAgain = readCanAskAgain()
        onPauseOrDispose { }
    }

    Box(modifier = modifier) {
        when (val state = audioPermissionState(granted, silentRefusals, canAskAgain)) {
            AudioPermissionState.GRANTED -> content()

            else -> AudioPermissionScreen(
                state = state,
                title = title,
                onAllow = { request.launch(permission) },
                onOpenSettings = { context.openAppSettings() },
            )
        }
    }
}

/** Opens the page of this app in the system settings, where the user can change its permissions. */
private fun Context.openAppSettings() {
    try {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    } catch (_: ActivityNotFoundException) {
        // A phone with no such page has no place to send the user to. The screen stays as it is.
    }
}
