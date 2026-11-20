/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

/** The one HTTP client of the app. Streams use it now. Anything else that goes to the network must use it too. */
@Module
@InstallIn(SingletonComponent::class)
internal object HttpClientModule {
    private const val CONNECT_SECONDS = 10L
    private const val READ_SECONDS = 20L

    @Provides
    @Singleton
    fun httpClient(@ApplicationContext context: Context): OkHttpClient {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_SECONDS, TimeUnit.SECONDS)
            // A server sees which app asks. Some stations refuse a caller with no name.
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", "Tarangini/$version").build())
            }
            .build()
    }
}
