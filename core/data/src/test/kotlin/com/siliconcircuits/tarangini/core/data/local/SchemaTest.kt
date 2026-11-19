/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local

import androidx.room3.migration.Migration
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Builds a database version from its committed schema file and brings it to
 * the newest version. A new version adds its migration to [migrations] and
 * one test here.
 */
@RunWith(RobolectricTestRunner::class)
class SchemaTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val migrations = emptyList<Migration>()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = instrumentation.targetContext.getDatabasePath("schema-test.db"),
        driver = AndroidSQLiteDriver(),
        databaseClass = TaranginiDatabase::class,
    )

    @Test
    fun `version 1 is built from its committed schema and matches the entities`() = runTest {
        helper.createDatabase(version = 1).close()

        helper.runMigrationsAndValidate(version = 1, migrations = migrations).close()
    }
}
