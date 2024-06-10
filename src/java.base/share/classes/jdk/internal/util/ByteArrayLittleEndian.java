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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * Utility methods for packing/unpacking primitive values in/out of byte arrays
 * using {@linkplain ByteOrder#LITTLE_ENDIAN little endian order}.
 * <p>
 * All methods in this class will throw an {@linkplain NullPointerException} if {@code null} is
 * passed in as a method parameter for a byte array.
 */
public final class ByteArrayLittleEndian {

    private ByteArrayLittleEndian() {
    }

    /*
     * Methods for unpacking primitive values from byte arrays starting at
     * a given offset.
     */

    /**
     * {@return a {@code boolean} from the provided {@code array} at the given {@code offset}}.
     *
     * @param array  to read a value from.
     * @param offset where extraction in the array should begin
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 1]
     * @see #setBoolean(byte[], int, boolean)
     */
    public static boolean getBoolean(byte[] array, int offset) {
        return array[offset] != 0;
    }

    /**
     * {@return a {@code char} from the provided {@code array} at the given {@code offset}
     * using little endian order}.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to get a value from.
     * @param offset where extraction in the array should begin
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 2]
     * @see #setChar(byte[], int, char)
     */
    public static char getChar(byte[] array, int offset) {
        return (char) (((array[offset    ] & 0xff)     )
                     | ((array[offset + 1] & 0xff) << 8));
    }

    /**
     * {@return a {@code short} from the provided {@code array} at the given {@code offset}
     * using little endian order}.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to get a value from.
     * @param offset where extraction in the array should begin
     * @return a {@code short} from the array
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 2]
     * @see #setShort(byte[], int, short)
     */
    public static short getShort(byte[] array, int offset) {
        return (short) (((array[offset    ] & 0xff)     )
                      | ((array[offset + 1] & 0xff) << 8));
    }

    /**
     * {@return an {@code unsigned short} from the provided {@code array} at the given {@code offset}
     * using little endian order}.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to get a value from.
     * @param offset where extraction in the array should begin
     * @return an {@code int} representing an unsigned short from the array
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 2]
     * @see #setUnsignedShort(byte[], int, int)
     */
    public static int getUnsignedShort(byte[] array, int offset) {
        return Short.toUnsignedInt(getShort(array, offset));
    }

    /**
     * {@return an {@code int} from the provided {@code array} at the given {@code offset}
     * using little endian order}.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to get a value from.
     * @param offset where extraction in the array should begin
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 4]
     * @see #setInt(byte[], int, int)
     */
    public static int getInt(byte[] array, int offset) {
        return ((array[offset    ] & 0xff)      )
             | ((array[offset + 1] & 0xff) <<  8)
             | ((array[offset + 2] & 0xff) << 16)
             | ((array[offset + 3] & 0xff) << 24);
    }

    /**
     * {@return a {@code float} from the provided {@code array} at the given {@code offset}
     * using little endian order}.
     * <p>
     * Variants of {@linkplain Float#NaN } values are canonized to a single NaN value.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to get a value from.
     * @param offset where extraction in the array should begin
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 4]
     * @see #setFloat(byte[], int, float)
     */
    public static float getFloat(byte[] array, int offset) {
        // Using Float.intBitsToFloat collapses NaN values to a single
        // "canonical" NaN value
        return Float.intBitsToFloat(getInt(array, offset));
    }

    /**
     * {@return a {@code long} from the provided {@code array} at the given {@code offset}
     * using little endian order}.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to get a value from.
     * @param offset where extraction in the array should begin
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 8]
     * @see #setLong(byte[], int, long)
     */
    public static long getLong(byte[] array, int offset) {
        return (((long) array[offset    ] & 0xff)      )
             | (((long) array[offset + 1] & 0xff) <<  8)
             | (((long) array[offset + 2] & 0xff) << 16)
             | (((long) array[offset + 3] & 0xff) << 24)
             | (((long) array[offset + 4] & 0xff) << 32)
             | (((long) array[offset + 5] & 0xff) << 40)
             | (((long) array[offset + 6] & 0xff) << 48)
             | (((long) array[offset + 7] & 0xff) << 56);
    }

    /**
     * {@return a {@code double} from the provided {@code array} at the given {@code offset}
     * using little endian order}.
     * <p>
     * Variants of {@linkplain Double#NaN } values are canonized to a single NaN value.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to get a value from.
     * @param offset where extraction in the array should begin
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 8]
     * @see #setDouble(byte[], int, double)
     */
    public static double getDouble(byte[] array, int offset) {
        // Using Double.longBitsToDouble collapses NaN values to a single
        // "canonical" NaN value
        return Double.longBitsToDouble(getLong(array, offset));
    }

    /*
     * Methods for packing primitive values into byte arrays starting at a given
     * offset.
     */

    /**
     * Sets (writes) the provided {@code value} into
     * the provided {@code array} beginning at the given {@code offset}.
     *
     * @param array  to set (write) a value into
     * @param offset where setting (writing) in the array should begin
     * @param value  value to set in the array
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length]
     * @see #getBoolean(byte[], int)
     */
    public static void setBoolean(byte[] array, int offset, boolean value) {
        array[offset] = (byte) (value ? 1 : 0);
    }

    /**
     * Sets (writes) the provided {@code value} using little endian order into
     * the provided {@code array} beginning at the given {@code offset}.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to set (write) a value into
     * @param offset where setting (writing) in the array should begin
     * @param value  value to set in the array
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 2]
     * @see #getChar(byte[], int)
     */
    public static void setChar(byte[] array, int offset, char value) {
        array[offset]     = (byte)  value;
        array[offset + 1] = (byte) (value >> 8);
    }

    /**
     * Sets (writes) the provided {@code value} using little endian order into
     * the provided {@code array} beginning at the given {@code offset}.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to set (write) a value into
     * @param offset where setting (writing) in the array should begin
     * @param value  value to set in the array
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 2]
     * @see #getShort(byte[], int)
     */
    public static void setShort(byte[] array, int offset, short value) {
        array[offset]     = (byte)  value;
        array[offset + 1] = (byte) (value >> 8);
    }

    /**
     * Sets (writes) the provided {@code value} using little endian order into
     * the provided {@code array} beginning at the given {@code offset}.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to set (write) a value into
     * @param offset where setting (writing) in the array should begin
     * @param value  value to set in the array
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 2]
     * @see #getUnsignedShort(byte[], int)
     */
    public static void setUnsignedShort(byte[] array, int offset, int value) {
        array[offset]     = (byte) (value & 0xff);
        array[offset + 1] = (byte) (value >> 8);
    }

    /**
     * Sets (writes) the provided {@code value} using little endian order into
     * the provided {@code array} beginning at the given {@code offset}.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to set (write) a value into
     * @param offset where setting (writing) in the array should begin
     * @param value  value to set in the array
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 4]
     * @see #getInt(byte[], int)
     */
    public static void setInt(byte[] array, int offset, int value) {
        array[offset]     = (byte)  value;
        array[offset + 1] = (byte) (value >> 8);
        array[offset + 2] = (byte) (value >> 16);
        array[offset + 3] = (byte) (value >> 24);
    }

    /**
     * Sets (writes) the provided {@code value} using little endian order into
     * the provided {@code array} beginning at the given {@code offset}.
     * <p>
     * Variants of {@linkplain Float#NaN } values are canonized to a single NaN value.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to set (write) a value into
     * @param offset where setting (writing) in the array should begin
     * @param value  value to set in the array
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 2]
     * @see #getFloat(byte[], int)
     */
    public static void setFloat(byte[] array, int offset, float value) {
        // Using Float.floatToIntBits collapses NaN values to a single
        // "canonical" NaN value
        setInt(array, offset, Float.floatToIntBits(value));
    }

    /**
     * Sets (writes) the provided {@code value} using little endian order into
     * the provided {@code array} beginning at the given {@code offset}.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to set (write) a value into
     * @param offset where setting (writing) in the array should begin
     * @param value  value to set in the array
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 4]
     * @see #getLong(byte[], int)
     */
    public static void setLong(byte[] array, int offset, long value) {
        array[offset]     = (byte)  value;
        array[offset + 1] = (byte) (value >> 8);
        array[offset + 2] = (byte) (value >> 16);
        array[offset + 3] = (byte) (value >> 24);
        array[offset + 4] = (byte) (value >> 32);
        array[offset + 5] = (byte) (value >> 40);
        array[offset + 6] = (byte) (value >> 48);
        array[offset + 7] = (byte) (value >> 56);
    }

    /**
     * Sets (writes) the provided {@code value} using little endian order into
     * the provided {@code array} beginning at the given {@code offset}.
     * <p>
     * Variants of {@linkplain Double#NaN } values are canonized to a single NaN value.
     * <p>
     * There are no access alignment requirements.
     *
     * @param array  to set (write) a value into
     * @param offset where setting (writing) in the array should begin
     * @param value  value to set in the array
     * @throws IndexOutOfBoundsException if the provided {@code offset} is outside
     *                                   the range [0, array.length - 2]
     * @see #getDouble(byte[], int)
     */
    public static void setDouble(byte[] array, int offset, double value) {
        // Using Double.doubleToLongBits collapses NaN values to a single
        // "canonical" NaN value
        setLong(array, offset, Double.doubleToLongBits(value));
    }
}
