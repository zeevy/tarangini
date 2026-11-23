/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

// The module that records the baseline profile of the app and measures its start. It runs on a
// device or an emulator and drives the app from outside, as a person would.
plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
    id("tarangini.quality")
}

android {
    namespace = TaranginiBuild.namespaceFor(path)
    compileSdk = TaranginiBuild.COMPILE_SDK
    compileSdkMinor = TaranginiBuild.COMPILE_SDK_MINOR

    defaultConfig {
        minSdk = TaranginiBuild.MIN_SDK
        targetSdk = TaranginiBuild.TARGET_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The profile and the numbers are for the free core build.
        missingDimensionStrategy("distribution", "core")
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = TaranginiBuild.javaVersion
        targetCompatibility = TaranginiBuild.javaVersion
    }

    lint {
        lintConfig = rootProject.file("config/lint.xml")
    }
}

// The profile is recorded on the device that is plugged in, or on a running emulator.
baselineProfile {
    useConnectedDevices = true
}
