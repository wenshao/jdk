/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025, Alibaba Group Holding Limited. All Rights Reserved.
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

/*
 * @test
 * @bug 8342206
 * @summary Testing handling of various constant descriptors in ClassFile API.
 * @modules java.base/jdk.internal.constant
 *          java.base/jdk.internal.classfile.impl
 * @run junit PoolEntryMatches
 */

import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.constantpool.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.constant.ModuleDesc;
import java.lang.constant.PackageDesc;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PoolEntryMatches {
    static final ClassDesc NOT_MATCH_CLASS_DESC = ClassDesc.ofDescriptor("La/b/c/d/e/f/g/h/i/j/k;");
    static final ClassDesc NOT_MATCH_CLASS_DESC_ARRAY = ClassDesc.ofDescriptor("[La/b/c/d/e/f/g/h/i/j/k;");
    static final MethodTypeDesc NOT_MATCH_METHO_TYPE_DESC = MethodTypeDesc.of(NOT_MATCH_CLASS_DESC);

    @Test
    public void classEntryMatches() throws Throwable {
        Class<?> clazz = ClassForMethodTypeGenerate.class;
        ClassDesc classDesc = ClassDesc.ofDescriptor(clazz.descriptorString());
        Class<?> arrayClass = Array.newInstance(clazz, 1).getClass();

        ConstantPoolBuilder cpb = ConstantPoolBuilder.of();

        {
            // ClassOrInterfaceDescImpl match
            ClassEntry classEntry = generateClassEntry(clazz);
            assertFalse(classEntry.matches(NOT_MATCH_CLASS_DESC)); // not match without sym
            assertFalse(classEntry.matches(NOT_MATCH_CLASS_DESC_ARRAY)); // not match without sym
            assertTrue(classEntry.matches(classDesc)); // match without sym
            assertTrue(classEntry.matches(classDesc)); // match with sym
            assertFalse(classEntry.matches(NOT_MATCH_CLASS_DESC)); // not match with sym
            assertFalse(classEntry.matches(NOT_MATCH_CLASS_DESC_ARRAY)); // not with without sym
        }
        {
            // ClassOrInterfaceDescImpl isFieldType
            Utf8Entry utf8Entry = generateClassUtf8Entry(clazz);
            assertFalse(utf8Entry.isFieldType(NOT_MATCH_CLASS_DESC)); // not match without sym
            assertFalse(utf8Entry.isFieldType(NOT_MATCH_CLASS_DESC_ARRAY)); // not match without sym
            assertTrue(utf8Entry.isFieldType(classDesc)); // match without sym
            assertTrue(utf8Entry.isFieldType(classDesc)); // match with sym
            assertFalse(utf8Entry.isFieldType(NOT_MATCH_CLASS_DESC)); // not match with sym
            assertFalse(utf8Entry.isFieldType(NOT_MATCH_CLASS_DESC_ARRAY)); // not with without sym
        }

        // mayBeArrayDescriptor match
        ClassDesc arrayClassDesc = ClassDesc.ofDescriptor(arrayClass.descriptorString());
        ClassEntry arrayClassEntry = cpb.classEntry(cpb.utf8Entry(arrayClassDesc.descriptorString()));
        assertFalse(arrayClassEntry.matches(NOT_MATCH_CLASS_DESC));
        assertFalse(arrayClassEntry.matches(NOT_MATCH_CLASS_DESC_ARRAY));
        assertTrue(arrayClassEntry.matches(arrayClassDesc));
        assertTrue(arrayClassEntry.matches(arrayClassDesc));
        assertFalse(arrayClassEntry.matches(NOT_MATCH_CLASS_DESC));
        assertFalse(arrayClassEntry.matches(NOT_MATCH_CLASS_DESC_ARRAY));

        // methodType matches
        MethodTypeDesc methodTypeDesc = MethodTypeDesc.of(ClassDesc.ofDescriptor(int.class.descriptorString()));
        MethodTypeEntry methodTypeEntry = generateMethodTypeEntryI();
        assertFalse(methodTypeEntry.matches(NOT_MATCH_METHO_TYPE_DESC)); // not match without sym
        assertTrue(methodTypeEntry.matches(methodTypeDesc)); // match without sym
        assertTrue(methodTypeEntry.matches(methodTypeDesc)); // match with sym
        assertFalse(methodTypeEntry.matches(NOT_MATCH_METHO_TYPE_DESC)); // not match with sym

        String moduleName = "java.base";
        ModuleEntry moduleEntry = cpb.moduleEntry(cpb.utf8Entry(moduleName));
        assertFalse(moduleEntry.matches(ModuleDesc.of("x.y.z")));
        assertTrue(moduleEntry.matches(ModuleDesc.of(moduleName)));
        assertFalse(moduleEntry.matches(ModuleDesc.of("x.y.z")));

        String packageName = "java.lang";
        PackageEntry packageEntry = cpb.packageEntry(cpb.utf8Entry(packageName.replace('.', '/')));
        assertFalse(packageEntry.matches(PackageDesc.of("x.y.z")));
        assertTrue(packageEntry.matches(PackageDesc.of(packageName)));
        assertFalse(packageEntry.matches(PackageDesc.of("x.y.z")));

        String str = "abc";
        StringEntry stringEntry = cpb.stringEntry(str);
        assertFalse(stringEntry.equalsString("a.b.c"));
        assertTrue(stringEntry.equalsString(str));
    }

    private static ClassModel classModel(Class clazz)
            throws IOException {
        String name = clazz.getName();
        int dotIndex = name.lastIndexOf('.');
        name = name.substring(dotIndex == -1 ? 0 : dotIndex + 1);
        byte[] code = clazz.getResourceAsStream(name + ".class").readAllBytes();
        return ClassFile.of().parse(code);
    }

    static ClassEntry generateClassEntry(Class<?> clazz) throws Exception {
        ClassModel classModel = classModel(clazz);
        return classModel.thisClass();
    }

    static Utf8Entry generateClassUtf8Entry(Class<?> clazz) throws Exception {
        ClassModel classModel = classModel(clazz);
        return classModel.fields().getFirst().fieldType();
    }

    static MethodTypeEntry generateMethodTypeEntryI() throws Exception {
        ClassModel classModel = classModel(ClassForMethodTypeGenerate.class);
        ConstantPool cp = classModel.constantPool();
        for (Iterator<PoolEntry> it = cp.iterator(); it.hasNext();) {
            PoolEntry next = it.next();
            if (next instanceof MethodTypeEntry) {
                return (MethodTypeEntry) next;
            }
        }

        return null;
    }

    private static class ClassForMethodTypeGenerate {
        public ClassForMethodTypeGenerate value;
        public void foo() {
            IntSupplier F = ClassForMethodTypeGenerate::get;
            F.getAsInt();
        }

        public static int get() {
            return 0;
        }
    }
}
