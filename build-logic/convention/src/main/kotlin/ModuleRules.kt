/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

/**
 * Which module may depend on which. This is the table in section 3 of
 * docs/ARCHITECTURE.md. A module that is not in the table is refused, so a
 * new module cannot be added without changing the table.
 */
object ModuleRules {
    private const val APP = ":app"
    private const val DOMAIN = ":core:domain"
    private const val UI = ":core:ui"
    private const val TESTING = ":core:testing"
    private const val PREMIUM = ":premium"
    private const val FEATURE_PREFIX = ":feature:"

    private val featureMayUse = setOf(DOMAIN, UI)

    private val coreMayUse: Map<String, Set<String>> = mapOf(
        DOMAIN to emptySet(),
        ":core:data" to setOf(DOMAIN),
        ":core:playback" to setOf(DOMAIN),
        UI to setOf(DOMAIN),
        TESTING to setOf(DOMAIN),
        PREMIUM to setOf(DOMAIN, UI),
    )

    /** True for the configurations of unit tests and device tests, such as `testImplementation`. */
    fun isTestConfiguration(name: String): Boolean = name.startsWith("test") || name.startsWith("androidTest")

    /** True for the configurations of the play flavour, such as `playImplementation`. */
    fun isPlayConfiguration(name: String): Boolean =
        name.startsWith("play") || name.startsWith("testPlay") || name.startsWith("androidTestPlay")

    /**
     * Returns null when [from] may depend on [to] in the configuration named
     * [configuration], or one line that says what is wrong. Test configurations
     * may also use the shared fakes. Only the play flavour of the app may use
     * the premium module, so closed code cannot reach the core build.
     */
    fun violation(from: String, to: String, configuration: String): String? = when {
        from == to -> null

        to == TESTING -> "$from uses $TESTING outside its tests".unless(isTestConfiguration(configuration))

        to == PREMIUM ->
            "$from uses $PREMIUM outside the play build of $APP"
                .unless(from == APP && isPlayConfiguration(configuration))

        from == APP -> null

        from.startsWith(FEATURE_PREFIX) -> "$from must not depend on $to".unless(to in featureMayUse)

        else -> when (val allowed = coreMayUse[from]) {
            null -> "$from is not a module in docs/ARCHITECTURE.md"
            else -> "$from must not depend on $to".unless(to in allowed)
        }
    }

    /** This problem line, or null when the dependency is [allowed]. */
    private fun String.unless(allowed: Boolean): String? = if (allowed) null else this
}
