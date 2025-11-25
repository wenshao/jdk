/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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
package java.time.format;

import jdk.internal.vm.annotation.Stable;

import java.time.temporal.ChronoField;
import java.util.*;

/**
 * A specialized Map implementation for ChronoField keys and Long values.
 * This implementation uses a bit set and array for efficient storage and retrieval,
 * optimized specifically for ChronoField keys which have sequential ordinals.
 */
final class ChronoFieldMap implements Map<ChronoField, Long> {
    /** Array of all ChronoField values, used for quick key lookup by ordinal. */
    @Stable
    private static final ChronoField[] FIELDS = ChronoField.values();
    /** The maximum possible size based on the number of ChronoField values. */
    private static final int MAX_SIZE = FIELDS.length;
    /** Array to store the values corresponding to each ChronoField by its ordinal. */
    private final long[] values = new long[MAX_SIZE];
    /** Bit set to track which ChronoField keys have been set. */
    private int bitSet;

    static {
        assert MAX_SIZE <= Integer.SIZE :
                "Too many ChronoField values. MAX_SIZE=" + MAX_SIZE + " exceeds Integer.SIZE=" + Integer.SIZE;
    }

    @Override
    public int size() {
        return Integer.bitCount(bitSet);
    }

    @Override
    public boolean isEmpty() {
        return bitSet == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return (bitSet & (1 << ((ChronoField) key).ordinal())) != 0;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value instanceof Long) {
            long v = (Long) value;
            for (long l : values) {
                if (l == v) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Long get(Object key) {
        int ordinal = ((ChronoField) key).ordinal();
        if ((bitSet & (1 << ordinal)) != 0) {
            return values[ordinal];
        }
        return null;
    }

    @Override
    public Long put(ChronoField key, Long value) {
        int ordinal = key.ordinal();
        long oldValue = values[ordinal];
        values[ordinal] = value;
        int mask = 1 << ordinal;
        if ((bitSet & mask) == 0) {
            bitSet |= mask;
            return null;
        } else {
            return oldValue;
        }
    }

    @Override
    public Long remove(Object key) {
        int ordinal = ((ChronoField) key).ordinal();
        long oldValue = values[ordinal];
        int mask = 1 << ordinal;
        if ((bitSet & mask) != 0) {
            bitSet &= ~(1 << ordinal);
            return oldValue;
        } else {
            return null;
        }
    }

    @Override
    public void putAll(Map<? extends ChronoField, ? extends Long> m) {
        for (Entry<? extends ChronoField, ? extends Long> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        bitSet = 0;
        Arrays.fill(values, 0);
    }

    /**
     * Abstract iterator implementation for iterating over the entries in the ChronoFieldMap.
     * Provides common functionality for key, value, and entry iterators.
     */
    private abstract class ChronoFieldMapIterator<T> implements Iterator<T> {
        /** Current index in the iteration. */
        int index = 0;
        /** Index of the last returned element, used for remove operation. */
        int lastReturnedIndex = -1;

        @Override
        public boolean hasNext() {
            while(this.index < MAX_SIZE && (bitSet & (1 << this.index)) == 0) {
                ++this.index;
            }

            return this.index != MAX_SIZE;
        }

        @Override
        public void remove() {
            if (this.lastReturnedIndex < 0) {
                throw new IllegalStateException();
            }
            bitSet &= ~(1 << this.lastReturnedIndex);

            this.lastReturnedIndex = -1;
        }
    }

    @Override
    public Set<ChronoField> keySet() {
        return new AbstractSet<>() {
            @Override
            public int size() {
                return ChronoFieldMap.this.size();
            }

            @Override
            public Iterator<ChronoField> iterator() {
                return new KeyIterator();
            }
        };
    }

    @Override
    public Collection<Long> values() {
        return new AbstractCollection<>() {
            @Override
            public Iterator<Long> iterator() {
                return new ValueIterator();
            }

            @Override
            public int size() {
                return ChronoFieldMap.this.size();
            }
        };
    }

    @Override
    public Set<Entry<ChronoField, Long>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public int size() {
                return ChronoFieldMap.this.size();
            }
            @Override
            public Iterator<Entry<ChronoField, Long>> iterator() {
                return new EntryIterator();
            }
        };
    }

    private final class ValueIterator extends ChronoFieldMapIterator<Long> {
        @Override
        public Long next() {
            if (!hasNext())
                throw new NoSuchElementException();
            lastReturnedIndex = index++;
            return values[lastReturnedIndex];
        }
    }

    private final class KeyIterator extends ChronoFieldMapIterator<ChronoField> {
        @Override
        public ChronoField next() {
            if (!hasNext())
                throw new NoSuchElementException();
            lastReturnedIndex = index++;
            return FIELDS[lastReturnedIndex];
        }
    }

    private final class EntryIterator extends ChronoFieldMapIterator<Entry<ChronoField, Long>> {
        @Override
        public Entry<ChronoField, Long> next() {
            if (!hasNext())
                throw new NoSuchElementException();
            lastReturnedIndex = index++;
            return new AbstractMap.SimpleEntry<>(FIELDS[lastReturnedIndex], values[lastReturnedIndex]);
        }
    }
}
