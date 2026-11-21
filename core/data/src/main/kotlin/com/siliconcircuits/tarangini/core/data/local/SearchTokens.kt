/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local

/**
 * The vowel signs and the other marks of the Indian scripts (Unicode classes Mn and Mc of the
 * blocks from Devanagari to Malayalam).
 *
 * The search table takes a mark as the end of a word unless it is told otherwise. Then a word
 * such as "గోవిందా" is stored as its consonants, and a search for "గా" also finds it.
 */
private const val INDIC_MARKS =
    // Devanagari
    "\u0900\u0901\u0902\u0903\u093A\u093B\u093C\u093E\u093F\u0940\u0941\u0942\u0943\u0944\u0945\u0946" +
        "\u0947\u0948\u0949\u094A\u094B\u094C\u094D\u094E\u094F\u0951\u0952\u0953\u0954\u0955\u0956\u0957" +
        "\u0962\u0963" +
        // Bengali
        "\u0981\u0982\u0983\u09BC\u09BE\u09BF\u09C0\u09C1\u09C2\u09C3\u09C4\u09C7\u09C8\u09CB\u09CC\u09CD" +
        "\u09D7\u09E2\u09E3\u09FE" +
        // Gurmukhi
        "\u0A01\u0A02\u0A03\u0A3C\u0A3E\u0A3F\u0A40\u0A41\u0A42\u0A47\u0A48\u0A4B\u0A4C\u0A4D\u0A51\u0A70" +
        "\u0A71\u0A75" +
        // Gujarati
        "\u0A81\u0A82\u0A83\u0ABC\u0ABE\u0ABF\u0AC0\u0AC1\u0AC2\u0AC3\u0AC4\u0AC5\u0AC7\u0AC8\u0AC9\u0ACB" +
        "\u0ACC\u0ACD\u0AE2\u0AE3\u0AFA\u0AFB\u0AFC\u0AFD\u0AFE\u0AFF" +
        // Odia
        "\u0B01\u0B02\u0B03\u0B3C\u0B3E\u0B3F\u0B40\u0B41\u0B42\u0B43\u0B44\u0B47\u0B48\u0B4B\u0B4C\u0B4D" +
        "\u0B55\u0B56\u0B57\u0B62\u0B63" +
        // Tamil
        "\u0B82\u0BBE\u0BBF\u0BC0\u0BC1\u0BC2\u0BC6\u0BC7\u0BC8\u0BCA\u0BCB\u0BCC\u0BCD\u0BD7" +
        // Telugu
        "\u0C00\u0C01\u0C02\u0C03\u0C04\u0C3C\u0C3E\u0C3F\u0C40\u0C41\u0C42\u0C43\u0C44\u0C46\u0C47\u0C48" +
        "\u0C4A\u0C4B\u0C4C\u0C4D\u0C55\u0C56\u0C62\u0C63" +
        // Kannada
        "\u0C81\u0C82\u0C83\u0CBC\u0CBE\u0CBF\u0CC0\u0CC1\u0CC2\u0CC3\u0CC4\u0CC6\u0CC7\u0CC8\u0CCA\u0CCB" +
        "\u0CCC\u0CCD\u0CD5\u0CD6\u0CE2\u0CE3\u0CF3" +
        // Malayalam
        "\u0D00\u0D01\u0D02\u0D03\u0D3B\u0D3C\u0D3E\u0D3F\u0D40\u0D41\u0D42\u0D43\u0D44\u0D46\u0D47\u0D48" +
        "\u0D4A\u0D4B\u0D4C\u0D4D\u0D57\u0D62\u0D63"

/**
 * The option of the search table that makes each mark of [INDIC_MARKS] part of a word.
 *
 * It is part of the definition of the table. A change to it needs a higher database version and
 * a migration that makes the table again.
 */
internal const val SEARCH_TOKEN_CHARS = "tokenchars=$INDIC_MARKS"
