/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NoticeFilesTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private fun library(name: String, vararg entries: Pair<String, ByteArray>): File =
        folder.newFile(name).apply { writeBytes(zip(*entries)) }

    @Test
    fun `the notice of a jar is found in its META-INF folder`() {
        val jar =
            library("a.jar", "a/A.class" to byteArrayOf(1), "META-INF/NOTICE.txt" to " Made by A \n".toByteArray())

        assertEquals("Made by A", NoticeFiles.noticeIn(jar))
    }

    @Test
    fun `the notice of an Android library is found in the jar inside it`() {
        val classes = zip("META-INF/NOTICE.md" to "Made by B".toByteArray())
        val aar = library("b.aar", "AndroidManifest.xml" to byteArrayOf(1), "classes.jar" to classes)

        assertEquals("Made by B", NoticeFiles.noticeIn(aar))
    }

    @Test
    fun `a library with no notice, an empty one or one in another folder gives none`() {
        assertNull(NoticeFiles.noticeIn(library("c.jar", "c/C.class" to byteArrayOf(1))))
        assertNull(NoticeFiles.noticeIn(library("d.jar", "META-INF/NOTICE" to "  ".toByteArray())))
        assertNull(NoticeFiles.noticeIn(library("e.jar", "docs/NOTICE.txt" to "Other".toByteArray())))
    }

    @Test
    fun `every notice of a file is kept, each once, also from a jar in the libs folder`() {
        val inside = zip("META-INF/NOTICE" to "From the inner jar".toByteArray())
        val classes = zip("META-INF/NOTICE.txt" to "Made by B".toByteArray(), "x.jar" to inside)
        val aar = library(
            "g.aar",
            "NOTICE-extra.md" to "At the top".toByteArray(),
            "classes.jar" to classes,
            "libs/other.jar" to zip(
                "NOTICE" to "Made by B".toByteArray(),
                "META-INF/NOTICE" to "Of libs".toByteArray(),
            ),
        )

        assertEquals("At the top\n\nMade by B\n\nOf libs", NoticeFiles.noticeIn(aar))
    }

    @Test
    fun `a very long notice is cut`() {
        val jar = library("f.jar", "NOTICE" to "x".repeat(NoticeFiles.MAX_CHARS + 10).toByteArray())

        assertEquals(NoticeFiles.MAX_CHARS, NoticeFiles.noticeIn(jar)?.length)
    }

    @Test
    fun `the asset lists the libraries in a fixed order`() {
        val json = NoticeFiles.toJson(mapOf("org.b:two" to "Second \"notice\"", "org.a:one" to "First"))

        assertEquals(
            """[{"groupId":"org.a","artifactId":"one","notice":"First"},""" +
                """{"groupId":"org.b","artifactId":"two","notice":"Second \"notice\""}]""",
            json,
        )
    }
}
