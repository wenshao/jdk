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
 * @bug 9999903
 * @summary String.encodedLengthUTF8() LATIN1 path should use long accumulator
 *          to avoid integer overflow, matching the UTF16 path
 * @requires os.maxMemory > 3g
 * @run main/othervm -Xmx3g EncodedLengthUTF8Overflow
 */

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class EncodedLengthUTF8Overflow {

    public static void main(String[] args) throws Exception {
        testSmallStringCorrectness();
        testLargeStringOverflow();
    }

    /**
     * Verify small LATIN1 strings with non-ASCII bytes encode correctly.
     */
    static void testSmallStringCorrectness() {
        // Each of these LATIN1 chars (0x80-0xFF) encodes to 2 UTF-8 bytes
        String s = "\u00e9\u00e8\u00ea\u00eb"; // éèêë
        byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        if (utf8.length != 8) {
            throw new RuntimeException(
                "Expected 8 UTF-8 bytes for 4 LATIN1 non-ASCII chars, got "
                + utf8.length);
        }

        // Mix of ASCII and non-ASCII
        String mixed = "abc\u00ff";
        byte[] mixedUtf8 = mixed.getBytes(StandardCharsets.UTF_8);
        if (mixedUtf8.length != 5) { // 3 ASCII (1 byte each) + 1 non-ASCII (2 bytes)
            throw new RuntimeException(
                "Expected 5 UTF-8 bytes for mixed string, got "
                + mixedUtf8.length);
        }

        System.out.println("PASS: small string correctness verified");
    }

    /**
     * Test that a very large LATIN1 string with all non-ASCII bytes
     * throws OutOfMemoryError instead of silently overflowing.
     */
    static void testLargeStringOverflow() {
        // We need > Integer.MAX_VALUE/2 non-ASCII bytes to overflow.
        // Each non-ASCII LATIN1 byte → 2 UTF-8 bytes, so:
        //   dp = 2 * length > Integer.MAX_VALUE when length > MAX_VALUE/2
        int length = Integer.MAX_VALUE / 2 + 1; // 1,073,741,824

        System.out.println("Allocating " + (length / (1024*1024))
            + " MB byte array...");
        byte[] bigArray;
        try {
            bigArray = new byte[length];
        } catch (OutOfMemoryError e) {
            System.out.println("SKIP: not enough memory to allocate test array");
            return;
        }

        // Fill with 0xFF (non-ASCII in LATIN1, signed byte = -1)
        Arrays.fill(bigArray, (byte) 0xFF);

        System.out.println("Creating LATIN1 string...");
        String bigString;
        try {
            bigString = new String(bigArray, StandardCharsets.ISO_8859_1);
        } catch (OutOfMemoryError e) {
            System.out.println("SKIP: not enough memory for String creation");
            return;
        }

        System.out.println("Encoding to UTF-8 (should throw OutOfMemoryError)...");
        try {
            byte[] utf8 = bigString.getBytes(StandardCharsets.UTF_8);
            // If we get here, either the JVM has >2GB arrays (unlikely)
            // or the overflow wasn't caught
            throw new RuntimeException(
                "Expected OutOfMemoryError but got " + utf8.length + " bytes");
        } catch (OutOfMemoryError e) {
            System.out.println("PASS: OutOfMemoryError thrown as expected: "
                + e.getMessage());
        } catch (NegativeArraySizeException e) {
            // This is the bug: int overflow → negative dp → negative array size
            throw new RuntimeException(
                "BUG: int overflow in encodedLengthUTF8 caused "
                + "NegativeArraySizeException", e);
        }
    }
}
