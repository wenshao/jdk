/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.internal.util;

import java.lang.invoke.MethodHandle;

import jdk.internal.vm.annotation.Stable;

/**
 * Digits provides a fast methodology for converting integers and longs to
 * decimal ASCII strings.
 *
 * @since 21
 */
public final class DecimalDigits {

    /**
     * Each element of the array represents the packaging of two ascii characters based on little endian:<p>
     * <pre>
     *      00 -> '0' | ('0' << 8) -> 0x3030
     *      01 -> '1' | ('0' << 8) -> 0x3130
     *      02 -> '2' | ('0' << 8) -> 0x3230
     *
     *     ...
     *
     *      10 -> '0' | ('1' << 8) -> 0x3031
     *      11 -> '1' | ('1' << 8) -> 0x3131
     *      12 -> '2' | ('1' << 8) -> 0x3231
     *
     *     ...
     *
     *      97 -> '7' | ('9' << 8) -> 0x3739
     *      98 -> '8' | ('9' << 8) -> 0x3839
     *      99 -> '9' | ('9' << 8) -> 0x3939
     * </pre>
     */
    @Stable
    private static final short[] DIGITS;

    static {
        short[] digits = new short[10 * 10];

        for (int i = 0; i < 10; i++) {
            short hi = (short) (i + '0');
            for (int j = 0; j < 10; j++) {
                short lo = (short) ((j + '0') << 8);
                digits[i * 10 + j] = (short) (hi | lo);
            }
        }
        DIGITS = digits;
    }

    /**
     * Constructor.
     */
    private DecimalDigits() {
    }

    /**
     * Places characters representing the integer i into the
     * character array buf. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * @implNote This method converts positive inputs into negative
     * values, to cover the Integer.MIN_VALUE case. Converting otherwise
     * (negative to positive) will expose -Integer.MIN_VALUE that overflows
     * integer.
     *
     * @param i     value to convert
     * @param index next index, after the least significant digit
     * @param buf   target buffer, Latin1-encoded
     * @return index of the most significant digit or minus sign, if present
     */
    public static int getCharsLatin1(int i, int index, byte[] buf) {
        // Used by trusted callers.  Assumes all necessary bounds checks have been done by the caller.
        int q;
        int charPos = index;

        boolean negative = i < 0;
        if (!negative) {
            i = -i;
        }

        // Generate two digits per iteration
        while (i <= -100) {
            q = i / 100;
            charPos -= 2;
            writeDigitPair(buf, charPos, (q * 100) - i);
            i = q;
        }

        // We know there are at most two digits left at this point.
        if (i < -9) {
            charPos -= 2;
            writeDigitPair(buf, charPos, -i);
        } else {
            buf[--charPos] = (byte)('0' - i);
        }

        if (negative) {
            buf[--charPos] = (byte)'-';
        }
        return charPos;
    }


    /**
     * Places characters representing the long i into the
     * character array buf. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * @implNote This method converts positive inputs into negative
     * values, to cover the Long.MIN_VALUE case. Converting otherwise
     * (negative to positive) will expose -Long.MIN_VALUE that overflows
     * long.
     *
     * @param i     value to convert
     * @param index next index, after the least significant digit
     * @param buf   target buffer, Latin1-encoded
     * @return index of the most significant digit or minus sign, if present
     */
    public static int getCharsLatin1(long i, int index, byte[] buf) {
        // Used by trusted callers.  Assumes all necessary bounds checks have been done by the caller.
        long q;
        int charPos = index;

        boolean negative = (i < 0);
        if (!negative) {
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i <= Integer.MIN_VALUE) {
            q = i / 100;
            charPos -= 2;
            writeDigitPair(buf, charPos, (int)((q * 100) - i));
            i = q;
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)i;
        while (i2 <= -100) {
            q2 = i2 / 100;
            charPos -= 2;
            writeDigitPair(buf, charPos, (q2 * 100) - i2);
            i2 = q2;
        }

        // We know there are at most two digits left at this point.
        if (i2 < -9) {
            charPos -= 2;
            writeDigitPair(buf, charPos, -i2);
        } else {
            buf[--charPos] = (byte)('0' - i2);
        }

        if (negative) {
            buf[--charPos] = (byte)'-';
        }
        return charPos;
    }

    public static void writeDigitPair(byte[] buf, int charPos, int value) {
        short pair = digitPair(value);
        buf[charPos] = (byte)(pair);
        buf[charPos + 1] = (byte)(pair >> 8);
    }

    /**
     * Calculate the number of digits required to represent the long.
     *
     * @param value value to convert
     *
     * @return number of digits
     */
    public static int size(long value) {
        boolean negative = value < 0;
        int sign = negative ? 1 : 0;

        if (!negative) {
            value = -value;
        }

        long precision = -10;
        for (int i = 1; i < 19; i++) {
            if (value > precision)
                return i + sign;

            precision = 10 * precision;
        }

        return 19 + sign;
    }

    /**
     * For values from 0 to 99 return a short encoding a pair of ASCII-encoded digit characters in little-endian
     * @param i value to convert
     * @return a short encoding a pair of ASCII-encoded digit characters
     */
    public static short digitPair(int i) {
        return DIGITS[i];
    }
}
