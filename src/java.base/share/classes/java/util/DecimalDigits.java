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

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.util.ByteArrayLittleEndian;
import jdk.internal.vm.annotation.Stable;

/**
 * Digits class for decimal digits.
 *
 * @since 21
 */
final class DecimalDigits implements Digits {
    private static final JavaLangAccess jla = SharedSecrets.getJavaLangAccess();

    /**
     * Singleton instance of DecimalDigits.
     */
    static final Digits INSTANCE = new DecimalDigits();

    /**
     * Constructor.
     */
    private DecimalDigits() {
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
            int digits = jla.digitPair(r);

            putCharMH.invokeExact(buffer, --index, digits >> 8);
            putCharMH.invokeExact(buffer, --index, digits & 0xFF);
        }

        int iq, ivalue = (int)value;
        while (ivalue <= -100) {
            iq = ivalue / 100;
            r = (iq * 100) - ivalue;
            ivalue = iq;
            int digits = jla.digitPair(r);
            putCharMH.invokeExact(buffer, --index, digits >> 8);
            putCharMH.invokeExact(buffer, --index, digits & 0xFF);
        }

        if (ivalue < 0) {
            ivalue = -ivalue;
        }

        int digits = jla.digitPair(ivalue);
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
}
