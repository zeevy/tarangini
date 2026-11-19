/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
    private const val FEATURE_PREFIX = ":feature:"

    private val featureMayUse = setOf(DOMAIN, UI)

    private val coreMayUse: Map<String, Set<String>> = mapOf(
        DOMAIN to emptySet(),
        ":core:data" to setOf(DOMAIN),
        ":core:playback" to setOf(DOMAIN),
        UI to setOf(DOMAIN),
        TESTING to setOf(DOMAIN),
        ":premium" to setOf(DOMAIN, UI),
    )

    /** True for the configurations of unit tests and device tests, such as `testImplementation`. */
    fun isTestConfiguration(name: String): Boolean = name.startsWith("test") || name.startsWith("androidTest")

    /**
     * Returns null when [from] may depend on [to], or one line that says what
     * is wrong. Test configurations may also use the shared fakes.
     */
    fun violation(from: String, to: String, isTestConfiguration: Boolean): String? {
        if (from == to) return null
        if (to == TESTING) {
            return if (isTestConfiguration) null else "$from uses $TESTING outside its tests"
        }
        val allowed = when {
            from == APP -> return null
            from.startsWith(FEATURE_PREFIX) -> featureMayUse
            else -> coreMayUse[from] ?: return "$from is not a module in docs/ARCHITECTURE.md"
        }
        return if (to in allowed) null else "$from must not depend on $to"
    }
}
