/*
 * Copyright (c) 2000, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs;

import java.nio.*;
import java.nio.charset.*;

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;

/**
 * Base class for different flavors of UTF-16 encoders
 */
public abstract class UnicodeEncoder extends CharsetEncoder implements ArrayEncoder {
    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

    protected static final char BYTE_ORDER_MARK = '\uFEFF';
    protected static final char REVERSED_MARK = '\uFFFE';

    protected static final int BIG = 0;
    protected static final int LITTLE = 1;

    private int byteOrder;      /* Byte order in use */
    private boolean usesMark;   /* Write an initial BOM */
    private boolean needsMark;

    protected UnicodeEncoder(Charset cs, int bo, boolean m) {
        super(cs, 2.0f,
              // Four bytes max if you need a BOM
              m ? 4.0f : 2.0f,
              // Replacement depends upon byte order
              ((bo == BIG)
               ? new byte[] { (byte)0xff, (byte)0xfd }
               : new byte[] { (byte)0xfd, (byte)0xff }));
        usesMark = needsMark = m;
        byteOrder = bo;
    }

    private void put(char c, ByteBuffer dst) {
        if (byteOrder == BIG) {
            dst.put((byte)(c >> 8));
            dst.put((byte)(c & 0xff));
        } else {
            dst.put((byte)(c & 0xff));
            dst.put((byte)(c >> 8));
        }
    }

    private final Surrogate.Parser sgp = new Surrogate.Parser();

    protected CoderResult encodeLoop(CharBuffer src, ByteBuffer dst) {
        int mark = src.position();

        if (needsMark && src.hasRemaining()) {
            if (dst.remaining() < 2)
                return CoderResult.OVERFLOW;
            put(BYTE_ORDER_MARK, dst);
            needsMark = false;
        }
        try {
            while (src.hasRemaining()) {
                char c = src.get();
                if (!Character.isSurrogate(c)) {
                    if (dst.remaining() < 2)
                        return CoderResult.OVERFLOW;
                    mark++;
                    put(c, dst);
                    continue;
                }
                int d = sgp.parse(c, src);
                if (d < 0)
                    return sgp.error();
                if (dst.remaining() < 4)
                    return CoderResult.OVERFLOW;
                mark += 2;
                put(Character.highSurrogate(d), dst);
                put(Character.lowSurrogate(d), dst);
            }
            return CoderResult.UNDERFLOW;
        } finally {
            src.position(mark);
        }
    }

    protected void implReset() {
        needsMark = usesMark;
    }

    public boolean canEncode(char c) {
        return ! Character.isSurrogate(c);
    }

    private static void putChar(byte[] ba, int off, char c, boolean big) {
        if (big) {
            ba[off    ] = (byte)(c >> 8);
            ba[off + 1] = (byte)(c & 0xff);
        } else {
            ba[off    ] = (byte)(c & 0xff);
            ba[off + 1] = (byte)(c >> 8);
        }
    }

    @Override
    public int encode(char[] sa, int sp, int len, byte[] da, int dp) {
        boolean big = byteOrder == BIG;
        int sl = sp + len;
        int dl = da.length;
        if (needsMark && sp < sl) {
            if (dl - dp < 2)
                return dp;
            putChar(da, dp, BYTE_ORDER_MARK, big);
            dp += 2;
            needsMark = false;
        }

        while (sp < sl && dl - dp >= 2) {
            putChar(da, dp, sa[sp++], big);
            dp += 2;
        }
        return dp;
    }

    @Override
    public int encodeFromLatin1(byte[] sa, int sp, int len, byte[] da, int dp) {
        boolean big = byteOrder == BIG;
        int sl = sp + len;
        int dl = da.length;
        if (needsMark && sp < sl) {
            if (dl - dp < 2)
                return dp;
            putChar(da, dp, BYTE_ORDER_MARK, big);
            dp += 2;
            needsMark = false;
        }

        while (sp < sl && dl - dp >= 2) {
            putChar(da, dp, (char) (sa[sp++] & 0xff), big);
            dp += 2;
        }
        return dp;
    }

    @Override
    public int encodeFromUTF16(byte[] sa, int sp, int len, byte[] da, int dp) {
        boolean big = byteOrder == BIG;
        int sl = sp + len;
        int dl = da.length;
        if (needsMark && sp < sl) {
            if (dl - dp < 2)
                return dp;
            putChar(da, dp, BYTE_ORDER_MARK, big);
            dp += 2;
            needsMark = false;
        }

        while (sp < sl && dl - dp >= 2) {
            putChar(da, dp, JLA.uncheckedGetUTF16Char(sa, sp++), big);
            dp += 2;
        }
        return dp;
    }

    @Override
    public boolean isASCIICompatible() {
        return false;
    }
}
