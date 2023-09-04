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

package java.util;

import java.lang.invoke.MethodHandle;

import jdk.internal.util.ByteArrayLittleEndian;
import jdk.internal.vm.annotation.Stable;

/**
 * Digits class for decimal digits.
 *
 * @since 21
 */
final class DecimalDigits implements Digits {
    @Stable
    private static final short[] DIGITS;

    /**
     * Singleton instance of DecimalDigits.
     */
    static final Digits INSTANCE = new DecimalDigits();

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

    static short digit(int index) {
        return DIGITS[index];
    }

    @Override
    public int digits(long value, byte[] buffer, int index,
                      MethodHandle putCharMH) throws Throwable {
        boolean negative = value < 0;
        if (!negative) {
            value = -value;
        }

        long q;
        int r;
        while (value <= Integer.MIN_VALUE) {
            q = value / 100;
            r = (int)((q * 100) - value);
            value = q;
            int digits = DIGITS[r];

            putCharMH.invokeExact(buffer, --index, digits >> 8);
            putCharMH.invokeExact(buffer, --index, digits & 0xFF);
        }

        int iq, ivalue = (int)value;
        while (ivalue <= -100) {
            iq = ivalue / 100;
            r = (iq * 100) - ivalue;
            ivalue = iq;
            int digits = DIGITS[r];
            putCharMH.invokeExact(buffer, --index, digits >> 8);
            putCharMH.invokeExact(buffer, --index, digits & 0xFF);
        }

        if (ivalue < 0) {
            ivalue = -ivalue;
        }

        int digits = DIGITS[ivalue];
        putCharMH.invokeExact(buffer, --index, digits >> 8);

        if (9 < ivalue) {
            putCharMH.invokeExact(buffer, --index, digits & 0xFF);
        }

        if (negative) {
            putCharMH.invokeExact(buffer, --index, (int)'-');
        }

        return index;
    }

    @Override
    public int size(long value) {
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
    static int getChars(int i, int index, byte[] buf) {
        int q, r;
        int charPos = index;

        boolean negative = i < 0;
        if (!negative) {
            i = -i;
        }

        // Generate two digits per iteration
        while (i <= -100) {
            q = i / 100;
            r = (q * 100) - i;
            i = q;
            charPos -= 2;
            ByteArrayLittleEndian.setShort(buf, charPos, DIGITS[r]);
        }

        // We know there are at most two digits left at this point.
        if (i < -9) {
            charPos -= 2;
            ByteArrayLittleEndian.setShort(buf, charPos, DIGITS[-i]);
        } else {
            buf[--charPos] = (byte)('0' - i);
        }

        if (negative) {
            buf[--charPos] = (byte)'-';
        }
        return charPos;
    }

    /**
     * Returns the string representation size for a given int value.
     *
     * @param x int value
     * @return string size
     *
     * @implNote There are other ways to compute this: e.g. binary search,
     * but values are biased heavily towards zero, and therefore linear search
     * wins. The iteration results are also routinely inlined in the generated
     * code after loop unrolling.
     */
    static int stringSize(int x) {
        int d = 1;
        if (x >= 0) {
            d = 0;
            x = -x;
        }
        int p = -10;
        for (int i = 1; i < 10; i++) {
            if (x > p)
                return i + d;
            p = 10 * p;
        }
        return 10 + d;
    }
}
