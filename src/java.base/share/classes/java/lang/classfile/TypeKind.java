/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, Alibaba Group Holding Limited. All Rights Reserved.
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

package java.lang.classfile;

import java.lang.invoke.TypeDescriptor;
import jdk.internal.javac.PreviewFeature;

import static java.lang.classfile.Opcode.*;

/**
 * Describes the types that can be part of a field or method descriptor.
 *
 * @since 22
 */
@PreviewFeature(feature = PreviewFeature.Feature.CLASSFILE_API)
public enum TypeKind {
    /** the primitive type byte */
    ByteType("byte", "B", 8,
            ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3, ILOAD, ILOAD_W,
            ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3, ISTORE, ISTORE_W,
            IRETURN, BALOAD, BASTORE),
    /** the primitive type short */
    ShortType("short", "S", 9,
            ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3, ILOAD, ILOAD_W,
            ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3, ISTORE, ISTORE_W,
            IRETURN, SALOAD, SASTORE),
    /** the primitive type int */
    IntType("int", "I", 10,
            ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3, ILOAD, ILOAD_W,
            ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3, ISTORE, ISTORE_W,
            IRETURN, IALOAD, IASTORE),
    /** the primitive type float */
    FloatType("float", "F", 6,
            FLOAD_0, FLOAD_1, FLOAD_2, FLOAD_3, FLOAD, FLOAD_W,
            FSTORE_0, FSTORE_1, FSTORE_2, FSTORE_3, FSTORE, FSTORE_W,
            FRETURN, FALOAD, FASTORE),
    /** the primitive type long */
    LongType("long", "J", 11,
            LLOAD_0, LLOAD_1, LLOAD_2, LLOAD_3, LLOAD, LLOAD_W,
            LSTORE_0, LSTORE_1, LSTORE_2, LSTORE_3, LSTORE, LSTORE_W,
            LRETURN, LALOAD, LASTORE),
    /** the primitive type double */
    DoubleType("double", "D", 7,
            DLOAD_0, DLOAD_1, DLOAD_2, DLOAD_3, DLOAD, DLOAD_W,
            DSTORE_0, DSTORE_1, DSTORE_2, DSTORE_3, DSTORE, DSTORE_W,
            DRETURN, DALOAD, DASTORE),
    /** a reference type */
    ReferenceType("reference type", "L", -1,
            ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3, ALOAD, ALOAD_W,
            ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3, ASTORE, ASTORE_W,
            ARETURN, AALOAD, AASTORE),
    /** the primitive type char */
    CharType("char", "C", 5,
            ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3, ILOAD, ILOAD_W,
            ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3, ISTORE, ISTORE_W,
            IRETURN, CALOAD, CASTORE),
    /** the primitive type boolean */
    BooleanType("boolean", "Z", 4,
            ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3, ILOAD, ILOAD_W,
            ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3, ISTORE, ISTORE_W,
            IRETURN, BALOAD, BASTORE),
    /** void */
    VoidType("void", "V", -1,
            null, null, null, null, null, null,
            null, null, null, null, null, null,
            RETURN, null, null);

    private final String name;
    private final String descriptor;
    private final int newarrayCode;

    private final Opcode load0;
    private final Opcode load1;
    private final Opcode load2;
    private final Opcode load3;
    private final Opcode load;
    private final Opcode loadw;

    private final Opcode store0;
    private final Opcode store1;
    private final Opcode store2;
    private final Opcode store3;
    private final Opcode store;
    private final Opcode storew;

    private final Opcode returnOpcode;
    private final Opcode arrayLoadOpcode;
    private final Opcode arrayStoreOpcode;

    /** {@return the human-readable name corresponding to this type} */
    public String typeName() { return name; }

    /** {@return the field descriptor character corresponding to this type} */
    public String descriptor() { return descriptor; }

    /**
     * {@return the code used by the {@code newarray} opcode corresponding to this type}
     * @since 23
     */
    public int newarrayCode() {
        return newarrayCode;
    }

    /**
     * {@return the number of local variable slots consumed by this type}
     */
    public int slotSize() {
        return switch (this) {
            case VoidType -> 0;
            case LongType, DoubleType -> 2;
            default -> 1;
        };
    }

    /**
     * Erase this type kind to the type which will be used for xLOAD, xSTORE,
     * and xRETURN bytecodes
     * @return the erased type kind
     */
    public TypeKind asLoadable() {
        return switch (this) {
            case BooleanType, ByteType, CharType, ShortType -> TypeKind.IntType;
            default -> this;
        };
    }

    TypeKind(String name, String descriptor, int newarrayCode,
             Opcode load0, Opcode load1, Opcode load2, Opcode load3, Opcode load, Opcode loadw,
             Opcode store0, Opcode store1, Opcode store2, Opcode store3, Opcode store, Opcode storew,
             Opcode returnOpcode, Opcode arrayLoadOpcode, Opcode arrayStoreOpcode
    ) {
        this.name = name;
        this.descriptor = descriptor;
        this.newarrayCode = newarrayCode;

        this.load0 = load0;
        this.load1 = load1;
        this.load2 = load2;
        this.load3 = load3;
        this.load = load;
        this.loadw = loadw;

        this.store0 = store0;
        this.store1 = store1;
        this.store2 = store2;
        this.store3 = store3;
        this.store = store;
        this.storew = storew;

        this.returnOpcode = returnOpcode;
        this.arrayLoadOpcode = arrayLoadOpcode;
        this.arrayStoreOpcode = arrayStoreOpcode;
    }

    /**
     * {@return the type kind associated with the array type described by the
     * array code used as an operand to {@code newarray}}
     * @param newarrayCode the operand of the {@code newarray} instruction
     * @throws IllegalArgumentException if the code is invalid
     * @since 23
     */
    public static TypeKind fromNewarrayCode(int newarrayCode) {
        return switch (newarrayCode) {
            case 4 -> TypeKind.BooleanType;
            case 5 -> TypeKind.CharType;
            case 6 -> TypeKind.FloatType;
            case 7 -> TypeKind.DoubleType;
            case 8 -> TypeKind.ByteType;
            case 9 -> TypeKind.ShortType;
            case 10 -> TypeKind.IntType;
            case 11 -> TypeKind.LongType;
            default -> throw new IllegalArgumentException("Bad newarray code: " + newarrayCode);
        };
    }

    /**
     * {@return the type kind associated with the specified field descriptor}
     * @param s the field descriptor
     * @throws IllegalArgumentException only if the descriptor is not valid
     */
    public static TypeKind fromDescriptor(CharSequence s) {
        if (s.isEmpty()) { // implicit null check
            throw new IllegalArgumentException("Empty descriptor");
        }
        return switch (s.charAt(0)) {
            case '[', 'L' -> TypeKind.ReferenceType;
            case 'B' -> TypeKind.ByteType;
            case 'C' -> TypeKind.CharType;
            case 'Z' -> TypeKind.BooleanType;
            case 'S' -> TypeKind.ShortType;
            case 'I' -> TypeKind.IntType;
            case 'F' -> TypeKind.FloatType;
            case 'J' -> TypeKind.LongType;
            case 'D' -> TypeKind.DoubleType;
            case 'V' -> TypeKind.VoidType;
            default -> throw new IllegalArgumentException("Bad type: " + s);
        };
    }

    /**
     * {@return the type kind associated with the specified field descriptor}
     * @param descriptor the field descriptor
     */
    public static TypeKind from(TypeDescriptor.OfField<?> descriptor) {
        return descriptor.isPrimitive() // implicit null check
                ? fromDescriptor(descriptor.descriptorString())
                : TypeKind.ReferenceType;
    }

    /**
     * {@return a local variable load instruction}
     *
     * @param slot the local variable slot to load from
     */
    public Opcode loadOpcode(int slot) {
        if (this == VoidType) {
            throw new IllegalArgumentException("void");
        }

        return switch (slot) {
            case 0 -> load0;
            case 1 -> load1;
            case 2 -> load2;
            case 3 -> load3;
            default -> (slot < 256) ? load : loadw;
        };
    }

    /**
     * {@return a local variable store instruction}
     *
     * @param slot the local variable slot to store to
     */
    public Opcode storeOpcode(int slot) {
        if (this == VoidType) {
            throw new IllegalArgumentException("void");
        }

        return switch (slot) {
            case 0 -> store0;
            case 1 -> store1;
            case 2 -> store2;
            case 3 -> store3;
            default -> (slot < 256) ? store : storew;
        };
    }

    /**
     * {@return a return instruction}
     */
    public Opcode returnOpcode() {
        return returnOpcode;
    }

    /**
     * {@return an instruction to load from an array}
     */
    public Opcode arrayLoadOpcode() {
        if (this == VoidType) {
            throw new IllegalArgumentException("void not an allowable array type");
        }
        return arrayLoadOpcode;
    }

    /**
     * {@return an instruction to store into an array}
     */
    public Opcode arrayStoreOpcode() {
        if (this == VoidType) {
            throw new IllegalArgumentException("void not an allowable array type");
        }
        return arrayStoreOpcode;
    }

}
