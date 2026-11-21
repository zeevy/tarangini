/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

plugins {
    id("com.android.application")
    id("tarangini.quality")
    id("app.cash.licensee")
}

android {
    namespace = TaranginiBuild.namespaceFor(path)
    compileSdk = TaranginiBuild.COMPILE_SDK

    defaultConfig {
        applicationId = TaranginiBuild.BASE_PACKAGE
        minSdk = TaranginiBuild.MIN_SDK
        targetSdk = TaranginiBuild.TARGET_SDK
    }

    buildTypes {
        release {
            // R8 removes code and resources nothing uses, and shortens names. The libraries bring
            // their own keep rules. Rules of the app itself are in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = TaranginiBuild.javaVersion
        targetCompatibility = TaranginiBuild.javaVersion
    }

    lint {
        lintConfig = rootProject.file("config/lint.xml")
    }

    // Compose preview tasks need the compiled resources in unit tests.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // One Robolectric settings file for every module.
    sourceSets.getByName("test").resources.srcDir(rootProject.file("config/robolectric"))
}

// Fails the build when a library that ships in the app has a licence outside this list.
// The app is GPL-3.0, but the Play build adds closed code, so no GPL library of others is allowed.
licensee {
    // The list of libraries goes into the app as the asset app/cash/licensee/artifacts.json. The
    // open source licences screen reads it, so a new library shows there with no other step.
    bundleAndroidAsset.set(true)
    allow("Apache-2.0")
    allow("MIT")
    allow("BSD-2-Clause")
    allow("BSD-3-Clause")
    allow("MPL-2.0")
    // The list is the policy, so a licence that no library uses yet is not a problem.
    unusedAction(app.cash.licensee.UnusedAction.IGNORE)
}

// Public code must not name the premium package, whichever build is made.
tasks.named("preBuild") {
    dependsOn(":checkPremiumBoundary")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "testImplementation"(libs.findLibrary("junit").get())
    "testImplementation"(libs.findLibrary("robolectric").get())
}
