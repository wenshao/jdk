/*
 * Copyright (c) 2001, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025, Alibaba Group Holding Limited. All Rights Reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;

public sealed class StreamEncoder extends Writer permits StreamEncoder.UTF8Impl {
    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

    private static final int INITIAL_BYTE_BUFFER_CAPACITY = 512;
    private static final int MAX_BYTE_BUFFER_CAPACITY = 8192;
    private static final byte LATIN1 = 0, UTF16  = 1;

    private volatile boolean closed;

    private void ensureOpen() throws IOException {
        if (closed)
            throw new IOException("Stream closed");
    }

    // Factories for java.io.OutputStreamWriter
    public static StreamEncoder forOutputStreamWriter(OutputStream out,
                                                      Object lock,
                                                      String charsetName)
        throws UnsupportedEncodingException
    {
        try {
            return forOutputStreamWriter(out, lock, Charset.forName(charsetName));
        } catch (IllegalCharsetNameException | UnsupportedCharsetException x) {
            throw new UnsupportedEncodingException (charsetName);
        }
    }

    public static StreamEncoder forOutputStreamWriter(OutputStream out,
                                                      Object lock,
                                                      Charset cs)
    {
        if (cs == UTF_8.INSTANCE) {
            return new UTF8Impl(out, lock);
        }
        return new StreamEncoder(out, lock, cs);
    }

    public static StreamEncoder forOutputStreamWriter(OutputStream out,
                                                      Object lock,
                                                      CharsetEncoder enc)
    {
        return new StreamEncoder(out, lock, enc);
    }

    public static StreamEncoder forOutputStreamWriter(OutputStream out,
                                                      CharsetEncoder enc)
    {
        return new StreamEncoder(out, enc);
    }

    // -- Public methods corresponding to those in OutputStreamWriter --

    // All synchronization and state/argument checking is done in these public
    // methods; the concrete stream-encoder subclasses defined below need not
    // do any such checking.

    public final String getEncoding() {
        if (isOpen())
            return encodingName();
        return null;
    }

    public Charset getCharset() {
        return cs;
    }

    public final void flushBuffer() throws IOException {
        synchronized (lock) {
            if (isOpen())
                implFlushBuffer();
            else
                throw new IOException("Stream closed");
        }
    }

    public final void write(int c) throws IOException {
        char[] cbuf = new char[1];
        cbuf[0] = (char) c;
        write(cbuf, 0, 1);
    }

    public final void write(char[] cbuf, int off, int len) throws IOException {
        synchronized (lock) {
            ensureOpen();
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }
            implWrite(cbuf, off, len);
        }
    }

    public void write(String str, int off, int len) throws IOException {
        /* Check the len before creating a char buffer */
        if (len < 0)
            throw new IndexOutOfBoundsException();
        char[] cbuf = new char[len];
        str.getChars(off, off + len, cbuf, 0);
        write(cbuf, 0, len);
    }

    public final void write(CharBuffer cb) throws IOException {
        int position = cb.position();
        try {
            synchronized (lock) {
                ensureOpen();
                implWrite(cb);
            }
        } finally {
            cb.position(position);
        }
    }

    public void write(StringBuilder sb) throws IOException {
        write(CharBuffer.wrap(sb));
    }

    public void write(byte coder, byte[] src, int off, int len) throws IOException {
        if (encoder instanceof ArrayEncoder ae) {
            write(coder, src, off, len, ae);
            return;
        }
        write(CharBuffer.wrap(new ArrayCharBuffer(coder, src, off, len)));
    }

    public record ArrayCharBuffer(byte coder, byte[] src, int off, int len) implements CharSequence {
        @Override
        public int length() {
            return len;
        }

        @Override
        public char charAt(int index) {
            return coder == LATIN1 ? (char) src[off + index] : JLA.uncheckedGetUTF16Char(src, off + index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new ArrayCharBuffer(coder, src, start, end - start);
        }
    }

    private void write(byte coder, byte[] src, int off, int len, ArrayEncoder encoder) throws IOException {
        int maxBytes = len * (coder == 0 ? 2 : 3); // maxBytesPerChar is 3
        if (maxBytes >= maxBufferCapacity) {
            byte[] bytes = new byte[maxBytes];
            if (coder == LATIN1) {
                encoder.encodeFromLatin1(src, off, len, bytes, 0);
            } else {
                encoder.encodeFromUTF16(src, off, len, bytes, 0);
            }
                /* If the request length exceeds the max size of the output buffer,
                   flush the buffer and then write the data directly.  In this
                   way buffered streams will cascade harmlessly. */
            implFlushBuffer();
            out.write(bytes, 0, maxBytes);
            return;
        }

        var bb = this.bb;
        int boff = bb.arrayOffset();
        int cap = bb.capacity();
        int newCap = bb.position() + boff + maxBytes;
        if (newCap >= maxBufferCapacity) {
            implFlushBuffer();
        }

        if (newCap > cap) {
            implFlushBuffer();
            this.bb = bb = ByteBuffer.allocate(newCap);
        }

        byte[] cb = bb.array();
        int pos = bb.position();

        if (coder == LATIN1) {
            pos = encoder.encodeFromLatin1(src, off, len, cb, pos);
        } else {
            pos = encoder.encodeFromUTF16(src, off, len, cb, pos);
        }

        bb.position(pos - boff);
    }

    public final void flush() throws IOException {
        synchronized (lock) {
            ensureOpen();
            implFlush();
        }
    }

    public final void close() throws IOException {
        synchronized (lock) {
            if (closed)
                return;
            try {
                implClose();
            } finally {
                closed = true;
            }
        }
    }

    private boolean isOpen() {
        return !closed;
    }


    // -- Charset-based stream encoder impl --

    private final Charset cs;
    protected final CharsetEncoder encoder;
    protected ByteBuffer bb;
    protected final int maxBufferCapacity;

    protected final OutputStream out;

    // Leftover first char in a surrogate pair
    protected boolean haveLeftoverChar = false;
    private char leftoverChar;
    private CharBuffer lcb = null;

    StreamEncoder(OutputStream out, Object lock, Charset cs) {
        this(out, lock,
            cs.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    private StreamEncoder(OutputStream out, Object lock, CharsetEncoder enc) {
        super(lock);
        this.out = out;
        this.cs = enc.charset();
        this.encoder = enc;

        this.bb = ByteBuffer.allocate(INITIAL_BYTE_BUFFER_CAPACITY);
        this.maxBufferCapacity = MAX_BYTE_BUFFER_CAPACITY;
    }

    private StreamEncoder(OutputStream out, CharsetEncoder enc) {
        super();
        this.out = out;
        this.cs = enc.charset();
        this.encoder = enc;

        this.bb = ByteBuffer.allocate(INITIAL_BYTE_BUFFER_CAPACITY);
        this.maxBufferCapacity = MAX_BYTE_BUFFER_CAPACITY;
    }

    private void writeBytes() throws IOException {
        bb.flip();
        int lim = bb.limit();
        int pos = bb.position();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        if (rem > 0) {
            out.write(bb.array(), bb.arrayOffset() + pos, rem);
        }
        bb.clear();
    }

    private void flushLeftoverChar(CharBuffer cb, boolean endOfInput)
        throws IOException
    {
        if (!haveLeftoverChar && !endOfInput)
            return;
        if (lcb == null)
            lcb = CharBuffer.allocate(2);
        else
            lcb.clear();
        if (haveLeftoverChar)
            lcb.put(leftoverChar);
        if ((cb != null) && cb.hasRemaining())
            lcb.put(cb.get());
        lcb.flip();
        while (lcb.hasRemaining() || endOfInput) {
            CoderResult cr = encoder.encode(lcb, bb, endOfInput);
            if (cr.isUnderflow()) {
                if (lcb.hasRemaining()) {
                    leftoverChar = lcb.get();
                    if (cb != null && cb.hasRemaining()) {
                        lcb.clear();
                        lcb.put(leftoverChar).put(cb.get()).flip();
                        continue;
                    }
                    return;
                }
                break;
            }
            if (cr.isOverflow()) {
                assert bb.position() > 0;
                writeBytes();
                continue;
            }
            cr.throwException();
        }
        haveLeftoverChar = false;
    }

    final void implWrite(char[] cbuf, int off, int len)
        throws IOException
    {
        CharBuffer cb = CharBuffer.wrap(cbuf, off, len);
        implWrite(cb);
    }

    final void implWrite(CharBuffer cb)
        throws IOException
    {
        if (haveLeftoverChar) {
            flushLeftoverChar(cb, false);
        }

        growByteBufferIfNeeded(cb.remaining());

        while (cb.hasRemaining()) {
            CoderResult cr = encoder.encode(cb, bb, false);
            if (cr.isUnderflow()) {
                assert (cb.remaining() <= 1) : cb.remaining();
                if (cb.remaining() == 1) {
                    haveLeftoverChar = true;
                    leftoverChar = cb.get();
                }
                break;
            }
            if (cr.isOverflow()) {
                assert bb.position() > 0;
                writeBytes();
                continue;
            }
            cr.throwException();
        }
    }

    public final void growByteBufferIfEmptyNeeded(int len) {
        if (bb.position() != 0) {
            return;
        }
        int cap = bb.capacity();
        if (cap < maxBufferCapacity) {
            int maxBytes = len;
            int newCap = Math.min(maxBytes, maxBufferCapacity);
            if (newCap > cap) {
                bb = ByteBuffer.allocate(newCap);
            }
        }
    }

    /**
     * Grows bb to a capacity to allow len characters be encoded.
     */
    public final void growByteBufferIfNeeded(int len) throws IOException {
        int cap = bb.capacity();
        if (cap < maxBufferCapacity) {
            int maxBytes = len * Math.round(encoder.maxBytesPerChar());
            int newCap = Math.min(maxBytes, maxBufferCapacity);
            if (newCap > cap) {
                implFlushBuffer();
                bb = ByteBuffer.allocate(newCap);
            }
        }
    }

    final void implFlushBuffer() throws IOException {
        if (bb.position() > 0) {
            writeBytes();
        }
    }

    final void implFlush() throws IOException {
        implFlushBuffer();
        out.flush();
    }

    final void implClose() throws IOException {
        try (out) {
            flushLeftoverChar(null, true);
            for (;;) {
                CoderResult cr = encoder.flush(bb);
                if (cr.isUnderflow())
                    break;
                if (cr.isOverflow()) {
                    assert bb.position() > 0;
                    writeBytes();
                    continue;
                }
                cr.throwException();
            }

            if (bb.position() > 0)
                writeBytes();
            out.flush();
        } catch (IOException x) {
            encoder.reset();
            throw x;
        }
    }

    final String encodingName() {
        return ((cs instanceof HistoricallyNamedCharset)
            ? ((HistoricallyNamedCharset)cs).historicalName()
            : cs.name());
    }

    private final static class UTF8Impl extends StreamEncoder {
        UTF8Impl(OutputStream out, Object lock) {
            super(out, lock, UTF_8.INSTANCE);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            /* Check the len before creating a char buffer */
            if (len < 0)
                throw new IndexOutOfBoundsException();
            if (haveLeftoverChar) {
                super.write(str, off, len);
                return;
            }

            int utf8Size = len * 3;
            if (utf8Size >= maxBufferCapacity) {
                byte[] utf8 = new byte[utf8Size];
                utf8Size = JLA.encodeUTF8(str, off, off + len, utf8, 0);
                /* If the request length exceeds the max size of the output buffer,
                   flush the buffer and then write the data directly.  In this
                   way buffered streams will cascade harmlessly. */
                implFlushBuffer();
                out.write(utf8, 0, utf8Size);
                return;
            }

            var bb = this.bb;
            int boff = bb.arrayOffset();
            int cap = bb.capacity();
            int newCap = bb.position() + boff + utf8Size;
            if (newCap >= maxBufferCapacity) {
                implFlushBuffer();
            }

            if (newCap > cap) {
                implFlushBuffer();
                this.bb = bb = ByteBuffer.allocate(newCap);
            }

            byte[] cb = bb.array();
            int pos = bb.position();

            pos = JLA.encodeUTF8(str, off, off + len, cb, pos + boff);
            bb.position(pos - boff);
        }
    }
}
