/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenceHeaderTest {
    private val notice = listOf(
        "This Source Code Form is subject to the terms of the Mozilla Public",
        "License, v. 2.0. If a copy of the MPL was not distributed with this",
        "file, You can obtain one at https://mozilla.org/MPL/2.0/.",
    )

    @Test
    fun `a Kotlin file with the block comment header passes`() {
        val source = "/*\n" + notice.joinToString("\n") { " * $it" } + "\n */\n\npackage a\n"

        assertTrue(LicenceHeader.isPresent(source))
    }

    @Test
    fun `an XML file with the header after the declaration passes`() {
        val source = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!--\n" +
            notice.joinToString("\n") { "  $it" } + "\n-->\n<resources />\n"

        assertTrue(LicenceHeader.isPresent(source))
    }

    @Test
    fun `a header written as line comments passes`() {
        val source = notice.joinToString("\n") { "// $it" } + "\n\npackage a\n"

        assertTrue(LicenceHeader.isPresent(source))
    }

    @Test
    fun `a file with no header fails`() {
        assertFalse(LicenceHeader.isPresent("package a\n\nclass Player\n"))
    }

    @Test
    fun `an empty file fails`() {
        assertFalse(LicenceHeader.isPresent(""))
    }

    @Test
    fun `a header with a changed licence version fails`() {
        val source = "/*\n" + notice.joinToString("\n") { " * $it" }.replace("v. 2.0", "v. 1.1") + "\n */\n"

        assertFalse(LicenceHeader.isPresent(source))
    }

    @Test
    fun `a header below the code fails`() {
        val code = (1..20).joinToString("\n") { "val value$it = $it" }
        val source = "package a\n\n$code\n\n/*\n" + notice.joinToString("\n") { " * $it" } + "\n */\n"

        assertFalse(LicenceHeader.isPresent(source))
    }
}
