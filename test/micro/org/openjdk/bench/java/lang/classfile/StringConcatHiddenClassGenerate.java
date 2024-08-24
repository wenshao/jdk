/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang.classfile;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.classfile.Annotation;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.TypeKind;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.classfile.ClassFile.*;
import static java.lang.constant.ConstantDescs.*;
import static java.lang.invoke.MethodType.methodType;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 6, time = 1)
@Fork(jvmArgsAppend = "--enable-preview", value = 1)
@State(Scope.Thread)
public class StringConcatHiddenClassGenerate {
    static final int FORCE_INLINE_THRESHOLD = 16;
    static final String CLASS_NAME   = "java.lang.String$$StringConcat";
    static final String METHOD_NAME  = "concat";

    static final ClassDesc CD_CONCAT             = ClassDesc.ofDescriptor("Ljava/lang/String$$StringConcat;");
    static final ClassDesc CD_StringConcatHelper = ClassDesc.ofDescriptor("Ljava/lang/StringConcatHelper;");
    static final ClassDesc CD_StringConcatBase   = ClassDesc.ofDescriptor("Ljava/lang/StringConcatHelper$StringConcatBase;");
    static final ClassDesc CD_Array_byte         = ClassDesc.ofDescriptor("[B");
    static final ClassDesc CD_Array_String       = ClassDesc.ofDescriptor("[Ljava/lang/String;");

    static final MethodTypeDesc MTD_byte_char       = MethodTypeDesc.of(CD_byte, CD_char);
    static final MethodTypeDesc MTD_byte            = MethodTypeDesc.of(CD_byte);
    static final MethodTypeDesc MTD_int             = MethodTypeDesc.of(CD_int);
    static final MethodTypeDesc MTD_int_int_boolean = MethodTypeDesc.of(CD_int, CD_int, CD_boolean);
    static final MethodTypeDesc MTD_int_int_char    = MethodTypeDesc.of(CD_int, CD_int, CD_char);
    static final MethodTypeDesc MTD_int_int_int     = MethodTypeDesc.of(CD_int, CD_int, CD_int);
    static final MethodTypeDesc MTD_int_int_long    = MethodTypeDesc.of(CD_int, CD_int, CD_long);
    static final MethodTypeDesc MTD_int_int_String  = MethodTypeDesc.of(CD_int, CD_int, CD_String);
    static final MethodTypeDesc MTD_String_float    = MethodTypeDesc.of(CD_String, CD_float);
    static final MethodTypeDesc MTD_String_double   = MethodTypeDesc.of(CD_String, CD_double);
    static final MethodTypeDesc MTD_String_Object   = MethodTypeDesc.of(CD_String, CD_Object);

    static final MethodTypeDesc MTD_INIT             = MethodTypeDesc.of(CD_void, CD_Array_String);
    static final MethodTypeDesc MTD_NEW_ARRAY_SUFFIX = MethodTypeDesc.of(CD_Array_byte, CD_String, CD_int, CD_byte);
    static final MethodTypeDesc MTD_STRING_INIT      = MethodTypeDesc.of(CD_void, CD_Array_byte, CD_byte);

    static final MethodTypeDesc PREPEND_int     = MethodTypeDesc.of(CD_int, CD_int, CD_byte, CD_Array_byte, CD_int, CD_String);
    static final MethodTypeDesc PREPEND_long    = MethodTypeDesc.of(CD_int, CD_int, CD_byte, CD_Array_byte, CD_long, CD_String);
    static final MethodTypeDesc PREPEND_boolean = MethodTypeDesc.of(CD_int, CD_int, CD_byte, CD_Array_byte, CD_boolean, CD_String);
    static final MethodTypeDesc PREPEND_char    = MethodTypeDesc.of(CD_int, CD_int, CD_byte, CD_Array_byte, CD_char, CD_String);
    static final MethodTypeDesc PREPEND_String  = MethodTypeDesc.of(CD_int, CD_int, CD_byte, CD_Array_byte, CD_String, CD_String);

    static final RuntimeVisibleAnnotationsAttribute FORCE_INLINE = RuntimeVisibleAnnotationsAttribute.of(Annotation.of(ClassDesc.ofDescriptor("Ljdk/internal/vm/annotation/ForceInline;")));

    static final MethodType CONSTRUCTOR_METHOD_TYPE        = MethodType.methodType(void.class, String[].class);
    static final Consumer<CodeBuilder> CONSTRUCTOR_BUILDER = new Consumer<CodeBuilder>() {
        @Override
        public void accept(CodeBuilder cb) {
            /*
             * super(constants);
             */
            int thisSlot      = cb.receiverSlot(),
                    constantsSlot = cb.parameterSlot(0);
            cb.aload(thisSlot)
                    .aload(constantsSlot)
                    .invokespecial(CD_StringConcatBase, INIT_NAME, MTD_INIT, false)
                    .return_();
        }
    };

    static MethodType methodType = MethodType.methodType(String.class, Object.class, float.class, int.class);
    static String[] constants = new String[] {"", "", "", ""};

    @Benchmark
    public void generateBytes() throws Exception {
        generateBytes(methodType, constants);
    }

    /**
     * Construct the MethodType of the prepend method, The parameters only support 5 types:
     * int/long/char/boolean/String. Not int/long/char/boolean type, use String type<p>
     *
     * The following is an example of the generated target code:
     * <blockquote><pre>
     *  int prepend(int length, byte coder, byte[] buff,  String[] constants
     *      int arg0, long arg1, boolean arg2, char arg3, String arg5)
     * </pre></blockquote>
     */
    private static MethodTypeDesc prependArgs(MethodType concatArgs) {
        int parameterCount = concatArgs.parameterCount();
        var paramTypes = new ClassDesc[parameterCount + 4];
        paramTypes[0] = CD_int;          // length
        paramTypes[1] = CD_byte;         // coder
        paramTypes[2] = CD_Array_byte;   // buff
        paramTypes[3] = CD_Array_String; // constants

        for (int i = 0; i < parameterCount; i++) {
            var cl = concatArgs.parameterType(i);
            paramTypes[i + 4] = needStringOf(cl) ? CD_String : ClassDesc.of(cl.getName());
        }
        return MethodTypeDesc.of(CD_int, paramTypes);
    }

    /**
     * Construct the MethodType of the coder method,
     * The first parameter is the initialized coder, Only parameter types that can be UTF16 are added.
     */
    private static MethodTypeDesc coderArgs(MethodType concatArgs) {
        int parameterCount = concatArgs.parameterCount();
        List<ClassDesc> paramTypes = new ArrayList<>();
        paramTypes.add(CD_int); // init coder
        for (int i = 0; i < parameterCount; i++) {
            var cl = concatArgs.parameterType(i);
            if (maybeUTF16(cl)) {
                paramTypes.add(cl == char.class ? CD_char : CD_String);
            }
        }
        return MethodTypeDesc.of(CD_int, paramTypes);
    }

    /**
     * Construct the MethodType of the length method,
     * The first parameter is the initialized length
     */
    private static MethodTypeDesc lengthArgs(MethodType concatArgs) {
        int parameterCount = concatArgs.parameterCount();
        var paramTypes = new ClassDesc[parameterCount + 1];
        paramTypes[0] = CD_int; // init long
        for (int i = 0; i < parameterCount; i++) {
            var cl = concatArgs.parameterType(i);
            paramTypes[i + 1] = needStringOf(cl) ? CD_String : ClassDesc.of(cl.getName());
        }
        return MethodTypeDesc.of(CD_int, paramTypes);
    }

    private static byte[] generateBytes(MethodType concatArgs, String[] constants) throws Exception {
        final boolean forceInline = true;

        MethodTypeDesc lengthArgs  = lengthArgs(concatArgs),
                coderArgs   = parameterMaybeUTF16(concatArgs) ? coderArgs(concatArgs) : null,
                prependArgs = prependArgs(concatArgs);

        return ClassFile.of().build(CD_CONCAT,
                new Consumer<ClassBuilder>() {
                    final boolean forceInline = concatArgs.parameterCount() < FORCE_INLINE_THRESHOLD;

                    @Override
                    public void accept(ClassBuilder clb) {
                        clb.withSuperclass(CD_StringConcatBase)
                                .withFlags(ACC_FINAL | ACC_SUPER | ACC_SYNTHETIC)
                                .withMethodBody(INIT_NAME, MTD_INIT, 0, CONSTRUCTOR_BUILDER)
                                .withMethod("length",
                                        lengthArgs,
                                        ACC_STATIC | ACC_PRIVATE,
                                        new Consumer<MethodBuilder>() {
                                            public void accept(MethodBuilder mb) {
                                                if (forceInline) {
                                                    mb.with(FORCE_INLINE);
                                                }
                                                mb.withCode(generateLengthMethod(lengthArgs));
                                            }
                                        })
                                .withMethod("prepend",
                                        prependArgs,
                                        ACC_STATIC | ACC_PRIVATE,
                                        new Consumer<MethodBuilder>() {
                                            public void accept(MethodBuilder mb) {
                                                if (forceInline) {
                                                    mb.with(FORCE_INLINE);
                                                }
                                                mb.withCode(generatePrependMethod(prependArgs));
                                            }
                                        })
                                .withMethod(METHOD_NAME,
                                        methodTypeDesc(concatArgs),
                                        ACC_FINAL,
                                        new Consumer<MethodBuilder>() {
                                            public void accept(MethodBuilder mb) {
                                                if (forceInline) {
                                                    mb.with(FORCE_INLINE);
                                                }
                                                mb.withCode(generateConcatMethod(
                                                        CD_CONCAT,
                                                        concatArgs,
                                                        lengthArgs,
                                                        coderArgs,
                                                        prependArgs));
                                            }
                                        });

                        if (coderArgs != null) {
                            clb.withMethod("coder",
                                    coderArgs,
                                    ACC_STATIC | ACC_PRIVATE,
                                    new Consumer<MethodBuilder>() {
                                        public void accept(MethodBuilder mb) {
                                            if (forceInline) {
                                                mb.with(FORCE_INLINE);
                                            }
                                            mb.withCode(generateCoderMethod(coderArgs));
                                        }
                                    });
                        }
                    }});
    }

    private static Consumer<CodeBuilder> generateConcatMethod(
            ClassDesc      concatClass,
            MethodType     concatArgs,
            MethodTypeDesc lengthArgs,
            MethodTypeDesc coderArgs,
            MethodTypeDesc prependArgs
    ) {
        return new Consumer<CodeBuilder>() {
            @Override
            public void accept(CodeBuilder cb) {
                // Compute parameter variable slots
                int paramCount = concatArgs.parameterCount(),
                        thisSlot = cb.receiverSlot(),
                        lengthSlot = cb.allocateLocal(TypeKind.IntType),
                        coderSlot = cb.allocateLocal(TypeKind.ByteType),
                        bufSlot = cb.allocateLocal(TypeKind.ReferenceType),
                        constantsSlot = cb.allocateLocal(TypeKind.ReferenceType),
                        suffixSlot = cb.allocateLocal(TypeKind.ReferenceType);

                /*
                 * Types other than int/long/char/boolean require local variables to store the result of stringOf.
                 *
                 * stringSlots stores the slots of parameters relative to local variables
                 *
                 * str0 = stringOf(arg0);
                 * str1 = stringOf(arg1);
                 * ...
                 * strN = toString(argN);
                 */
                int[] stringSlots = new int[paramCount];
                for (int i = 0; i < paramCount; i++) {
                    var cl = concatArgs.parameterType(i);
                    if (needStringOf(cl)) {
                        MethodTypeDesc methodTypeDesc;
                        if (cl == float.class) {
                            methodTypeDesc = MTD_String_float;
                        } else if (cl == double.class) {
                            methodTypeDesc = MTD_String_double;
                        } else {
                            methodTypeDesc = MTD_String_Object;
                        }
                        stringSlots[i] = cb.allocateLocal(TypeKind.ReferenceType);
                        cb.loadLocal(TypeKind.from(cl), cb.parameterSlot(i))
                          .invokestatic(CD_StringConcatHelper, "stringOf", methodTypeDesc)
                          .astore(stringSlots[i]);
                    }
                }

                /*
                 * coder = coder(this.coder, arg0, arg1, ... argN);
                 */
                cb.aload(thisSlot)
                        .getfield(concatClass, "coder", CD_byte);
                if (coderArgs != null) {
                    for (int i = 0; i < paramCount; i++) {
                        var cl = concatArgs.parameterType(i);
                        if (maybeUTF16(cl)) {
                            if (cl == char.class) {
                                cb.loadLocal(TypeKind.CharType, cb.parameterSlot(i));
                            } else {
                                cb.aload(stringSlots[i]);
                            }
                        }
                    }
                    cb.invokestatic(concatClass, "coder", coderArgs);
                }
                cb.istore(coderSlot);

                /*
                 * length = length(this.length, arg0, arg1, ..., argN);
                 */
                cb.aload(thisSlot)
                        .getfield(concatClass, "length", CD_int);
                for (int i = 0; i < paramCount; i++) {
                    var cl = concatArgs.parameterType(i);
                    int paramSlot = cb.parameterSlot(i);
                    if (needStringOf(cl)) {
                        paramSlot = stringSlots[i];
                        cl = String.class;
                    }
                    cb.loadLocal(TypeKind.from(cl), paramSlot);
                }
                cb.invokestatic(concatClass, "length", lengthArgs);

                /*
                 * String[] constants = this.constants;
                 * suffix  = constants[paranCount];
                 * length -= suffix.length();
                 */
                cb.aload(thisSlot)
                  .getfield(concatClass, "constants", CD_Array_String)
                  .dup()
                  .astore(constantsSlot)
                  .ldc(paramCount)
                  .aaload()
                  .dup()
                  .astore(suffixSlot)
                  .invokevirtual(CD_String, "length", MTD_int)
                  .isub()
                  .istore(lengthSlot);

                /*
                 * Allocate buffer :
                 *
                 *  buf = newArrayWithSuffix(suffix, length, coder)
                 */
                cb.aload(suffixSlot)
                  .iload(lengthSlot)
                  .iload(coderSlot)
                  .invokestatic(CD_StringConcatHelper, "newArrayWithSuffix", MTD_NEW_ARRAY_SUFFIX)
                  .astore(bufSlot);

                /*
                 * prepend(length, coder, buf, constants, ar0, ar1, ..., argN);
                 */
                cb.iload(lengthSlot)
                        .iload(coderSlot)
                        .aload(bufSlot)
                        .aload(constantsSlot);
                for (int i = 0; i < paramCount; i++) {
                    var cl = concatArgs.parameterType(i);
                    int paramSlot = cb.parameterSlot(i);
                    var kind = TypeKind.from(cl);
                    if (needStringOf(cl)) {
                        paramSlot = stringSlots[i];
                        kind = TypeKind.ReferenceType;
                    }
                    cb.loadLocal(kind, paramSlot);
                }
                cb.invokestatic(concatClass, "prepend", prependArgs);

                // return new String(buf, coder);
                cb.new_(CD_String)
                  .dup()
                  .aload(bufSlot)
                  .iload(coderSlot)
                  .invokespecial(CD_String, INIT_NAME, MTD_STRING_INIT)
                  .areturn();
            }
        };
    }

    /**
     * Generate length method. <p>
     *
     * The following is an example of the generated target code:
     *
     * <blockquote><pre>
     * import static java.lang.StringConcatHelper.stringSize;
     *
     * static int length(int length, int arg0, long arg1, boolean arg2, char arg3,
     *                  String arg4, String arg5, String arg6, String arg7) {
     *     return stringSize(stringSize(stringSize(length, arg0), arg1), ..., arg7);
     * }
     * </pre></blockquote>
     */
    private static Consumer<CodeBuilder> generateLengthMethod(MethodTypeDesc lengthArgs) {
        return new Consumer<CodeBuilder>() {
            @Override
            public void accept(CodeBuilder cb) {
                int lengthSlot = cb.parameterSlot(0);
                cb.iload(lengthSlot);
                for (int i = 1; i < lengthArgs.parameterCount(); i++) {
                    var cl = lengthArgs.parameterType(i);
                    MethodTypeDesc methodTypeDesc;
                    if (cl == CD_char) {
                        methodTypeDesc = MTD_int_int_char;
                    } else if (cl == CD_int) {
                        methodTypeDesc = MTD_int_int_int;
                    } else if (cl == CD_long) {
                        methodTypeDesc = MTD_int_int_long;
                    } else if (cl == CD_boolean) {
                        methodTypeDesc = MTD_int_int_boolean;
                    } else {
                        methodTypeDesc = MTD_int_int_String;
                    }
                    cb.loadLocal(TypeKind.from(cl), cb.parameterSlot(i))
                            .invokestatic(CD_StringConcatHelper, "stringSize", methodTypeDesc);
                }
                cb.ireturn();
            }
        };
    }

    /**
     * Generate coder method. <p>
     *
     * The following is an example of the generated target code:
     *
     * <blockquote><pre>
     * import static java.lang.StringConcatHelper.stringCoder;
     *
     * static int cocder(int coder, char arg3, String str4, String str5, String str6, String str7) {
     *     return coder | stringCoder(arg3) | str4.coder() | str5.coder() | str6.coder() | str7.coder();
     * }
     * </pre></blockquote>
     */
    private static Consumer<CodeBuilder> generateCoderMethod(MethodTypeDesc coderArgs) {
        return new Consumer<CodeBuilder>() {
            @Override
            public void accept(CodeBuilder cb) {
                /*
                 * return coder | stringCoder(argN) | ... | arg1.coder() | arg0.coder();
                 */
                int coderSlot = cb.parameterSlot(0);
                cb.iload(coderSlot);
                for (int i = 1; i < coderArgs.parameterCount(); i++) {
                    var cl = coderArgs.parameterType(i);
                    cb.loadLocal(TypeKind.from(cl), cb.parameterSlot(i));
                    if (cl == CD_char) {
                        cb.invokestatic(CD_StringConcatHelper, "stringCoder", MTD_byte_char);
                    } else {
                        cb.invokevirtual(CD_String, "coder", MTD_byte);
                    }
                    cb.ior();
                }
                cb.ireturn();
            }
        };
    }

    /**
     * Generate prepend method. <p>
     *
     * The following is an example of the generated target code:
     *
     * <blockquote><pre>
     * import static java.lang.StringConcatHelper.prepend;
     *
     * static int prepend(int length, int coder, byte[] buf, String[] constants,
     *                int arg0, long arg1, boolean arg2, char arg3,
     *                String str4, String str5, String str6, String str7) {
     *
     *     return prepend(prepend(prepend(prepend(
     *             prepend(prepend(prepend(prepend(length,
     *                  buf, str7, constant[7]), buf, str6, constant[6]),
     *                  buf, str5, constant[5]), buf, str4, constant[4]),
     *                  buf, arg3, constant[3]), buf, arg2, constant[2]),
     *                  buf, arg1, constant[1]), buf, arg0, constant[0]);
     * }
     * </pre></blockquote>
     */
    private static Consumer<CodeBuilder> generatePrependMethod(MethodTypeDesc prependArgs) {
        return new Consumer<CodeBuilder>() {
            @Override
            public void accept(CodeBuilder cb) {
                // Compute parameter variable slots
                int lengthSlot    = cb.parameterSlot(0),
                        coderSlot     = cb.parameterSlot(1),
                        bufSlot       = cb.parameterSlot(2),
                        constantsSlot = cb.parameterSlot(3);
                /*
                 * // StringConcatHelper.prepend
                 * return prepend(prepend(prepend(prepend(
                 *         prepend(apppend(prepend(prepend(length,
                 *              buf, str7, constant[7]), buf, str6, constant[6]),
                 *              buf, str5, constant[5]), buf, arg4, constant[4]),
                 *              buf, arg3, constant[3]), buf, arg2, constant[2]),
                 *              buf, arg1, constant[1]), buf, arg0, constant[0]);
                 */
                cb.iload(lengthSlot);
                for (int i = prependArgs.parameterCount() - 1; i >= 4; i--) {
                    var cl   = prependArgs.parameterType(i);
                    var kind = TypeKind.from(cl);

                    // There are only 5 types of parameters: int, long, boolean, char, String
                    MethodTypeDesc methodTypeDesc;
                    if (cl == CD_int) {
                        methodTypeDesc = PREPEND_int;
                    } else if (cl == CD_long) {
                        methodTypeDesc = PREPEND_long;
                    } else if (cl == CD_boolean) {
                        methodTypeDesc = PREPEND_boolean;
                    } else if (cl == CD_char) {
                        methodTypeDesc = PREPEND_char;
                    } else {
                        kind = TypeKind.ReferenceType;
                        methodTypeDesc = PREPEND_String;
                    }

                    cb.iload(coderSlot)
                            .aload(bufSlot)
                            .loadLocal(kind, cb.parameterSlot(i))
                            .aload(constantsSlot)
                            .ldc(i - 4)
                            .aaload()
                            .invokestatic(CD_StringConcatHelper, "prepend", methodTypeDesc);
                }
                cb.ireturn();
            }
        };
    }

    static boolean needStringOf(Class<?> cl) {
        return cl != int.class && cl != long.class && cl != boolean.class && cl != char.class;
    }

    static boolean maybeUTF16(Class<?> cl) {
        return cl == char.class || !cl.isPrimitive();
    }

    static boolean parameterMaybeUTF16(MethodType args) {
        for (int i = 0; i < args.parameterCount(); i++) {
            if (maybeUTF16(args.parameterType(i))) {
                return true;
            }
        }
        return false;
    }

    static MethodTypeDesc methodTypeDesc(MethodType type) {
        var returnDesc = ClassDesc.of(type.returnType().getName());
        if (type.parameterCount() == 0) {
            return MethodTypeDesc.of(returnDesc, new ClassDesc[0]);
        }
        var paramDescs = new ClassDesc[type.parameterCount()];
        for (int i = 0; i < type.parameterCount(); i++) {
            paramDescs[i] = ClassDesc.of(type.parameterType(i).getName());
        }
        return MethodTypeDesc.of(returnDesc, paramDescs);
    }
}