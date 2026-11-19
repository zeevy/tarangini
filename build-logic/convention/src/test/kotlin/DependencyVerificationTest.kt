/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Guards the settings of the checksum file. Gradle itself checks the checksums in every build. */
class DependencyVerificationTest {
    private val root = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "gradle/verification-metadata.xml").isFile }
    private val document = DocumentBuilderFactory.newInstance()
        .apply { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        .newDocumentBuilder()
        .parse(File(root, "gradle/verification-metadata.xml"))

    private fun elements(tag: String): List<Element> =
        document.getElementsByTagName(tag).let { nodes -> List(nodes.length) { nodes.item(it) as Element } }

    @Test
    fun `library files and their metadata are checked`() {
        assertEquals(listOf("true"), elements("verify-metadata").map { it.textContent })
    }

    @Test
    fun `only source and javadoc jars are trusted without a checksum`() {
        val trusted = elements("trust").map { trust ->
            (0 until trust.attributes.length).associate {
                trust.attributes.item(it).nodeName to
                    trust.attributes.item(it).nodeValue
            }
        }

        assertEquals(
            setOf(
                mapOf("file" to ".*-javadoc[.]jar", "regex" to "true"),
                mapOf("file" to ".*-sources[.]jar", "regex" to "true"),
            ),
            trusted.toSet(),
        )
        assertTrue(elements("trusted-keys").isEmpty() && elements("ignored-keys").isEmpty())
    }

    @Test
    fun `every listed file has a SHA-256`() {
        val artifacts = elements("artifact")

        assertTrue(artifacts.isNotEmpty())
        assertEquals(
            emptyList<String>(),
            artifacts.filter { it.getElementsByTagName("sha256").length == 0 }.map { it.getAttribute("name") },
        )
    }

    @Test
    fun `the build tool with one file per system is listed for Linux and macOS`() {
        val names = elements("artifact").map { it.getAttribute("name") }.filter { it.startsWith("aapt2-") }

        assertTrue(names.any { it.endsWith("-linux.jar") })
        assertTrue(names.any { it.endsWith("-osx.jar") })
    }
}
