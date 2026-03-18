/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @summary String.encodedLengthUTF8() LATIN1 path should use long accumulator
 *          to avoid integer overflow, matching the UTF16 path
 * @requires os.maxMemory > 5g
 * @run main/othervm -Xmx5g EncodedLengthUTF8Overflow
 */

import java.nio.charset.StandardCharsets;

public class EncodedLengthUTF8Overflow {

    public static void main(String[] args) throws Exception {
        testSmallStringEncodedLength();
        testLargeStringOverflow();
    }

    /**
     * Verify encodedLength(UTF_8) returns correct values for small
     * LATIN1 strings with non-ASCII bytes.
     */
    static void testSmallStringEncodedLength() {
        // Each LATIN1 char 0x80-0xFF encodes to 2 UTF-8 bytes
        String latin1 = "\u00e9\u00e8\u00ea\u00eb"; // éèêë
        int len = latin1.encodedLength(StandardCharsets.UTF_8);
        if (len != 8) {
            throw new RuntimeException(
                "Expected encodedLength=8 for 4 non-ASCII LATIN1 chars, got " + len);
        }

        // Mix of ASCII and non-ASCII
        // 3 ASCII (1 byte each) + 1 non-ASCII (2 bytes) = 5
        String mixed = "abc\u00ff";
        int mixedLen = mixed.encodedLength(StandardCharsets.UTF_8);
        if (mixedLen != 5) {
            throw new RuntimeException(
                "Expected encodedLength=5 for mixed string, got " + mixedLen);
        }

        // Pure ASCII
        String ascii = "hello";
        int asciiLen = ascii.encodedLength(StandardCharsets.UTF_8);
        if (asciiLen != 5) {
            throw new RuntimeException(
                "Expected encodedLength=5 for ASCII string, got " + asciiLen);
        }

        System.out.println("PASS: small string encodedLength correctness verified");
    }

    /**
     * Test that encodedLength(UTF_8) throws OutOfMemoryError for a very
     * large LATIN1 string whose UTF-8 length exceeds Integer.MAX_VALUE,
     * instead of silently overflowing to a negative value.
     *
     * Uses encodedLength() directly which calls encodedLengthUTF8()
     * internally, avoiding the need to allocate a 2GB+ output buffer.
     */
    static void testLargeStringOverflow() {
        // We need > Integer.MAX_VALUE/2 non-ASCII bytes to overflow.
        // Each non-ASCII LATIN1 byte encodes to 2 UTF-8 bytes, so:
        //   dp = 2 * length > Integer.MAX_VALUE when length > MAX_VALUE/2
        int length = Integer.MAX_VALUE / 2 + 1; // 1,073,741,824

        System.out.println("Creating " + (length / (1024 * 1024))
            + " char LATIN1 string...");
        String bigString;
        try {
            // Create a LATIN1 string with non-ASCII chars (U+00FF encodes to 2 bytes in UTF-8)
            bigString = "\u00ff".repeat(length);
        } catch (OutOfMemoryError e) {
            System.out.println("SKIP: not enough memory for String creation");
            return;
        }

        // Use encodedLength() which directly calls encodedLengthUTF8().
        // This avoids allocating a 2GB+ output byte array, making the
        // test much more memory-efficient and reliable.
        System.out.println("Calling encodedLength(UTF_8) "
            + "(should throw OutOfMemoryError)...");
        try {
            int encodedLen = bigString.encodedLength(StandardCharsets.UTF_8);
            // If we get here with a negative or small value, the int overflowed
            if (encodedLen < length) {
                throw new RuntimeException(
                    "BUG: encodedLength returned " + encodedLen
                    + " (likely int overflow), expected > " + length);
            }
            throw new RuntimeException(
                "Expected OutOfMemoryError but got encodedLength=" + encodedLen);
        } catch (OutOfMemoryError e) {
            System.out.println("PASS: OutOfMemoryError thrown as expected: "
                + e.getMessage());
        }
    }
}
