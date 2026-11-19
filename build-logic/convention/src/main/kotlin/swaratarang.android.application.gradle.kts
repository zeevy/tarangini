/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("com.android.application")
    id("swaratarang.quality")
    id("app.cash.licensee")
}

android {
    namespace = SwaraTarangBuild.namespaceFor(path)
    compileSdk = SwaraTarangBuild.COMPILE_SDK

    defaultConfig {
        applicationId = SwaraTarangBuild.BASE_PACKAGE
        minSdk = SwaraTarangBuild.MIN_SDK
        targetSdk = SwaraTarangBuild.TARGET_SDK
    }

    compileOptions {
        sourceCompatibility = SwaraTarangBuild.javaVersion
        targetCompatibility = SwaraTarangBuild.javaVersion
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
licensee {
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
