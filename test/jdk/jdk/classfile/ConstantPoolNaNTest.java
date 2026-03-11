/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that Float/Double NaN constant pool entries are correctly
 *          deduplicated and have correct equals/hashCode behavior.
 * @run junit ConstantPoolNaNTest
 */
import java.lang.classfile.*;
import java.lang.classfile.constantpool.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstantPoolNaNTest {

    @Test
    void testFloatNaNDeduplication() {
        var cc = ClassFile.of();
        byte[] bytes = cc.build(ClassDesc.of("TestClass"), clb ->
            clb.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void),
                ClassFile.ACC_STATIC, mb -> mb.withCode(cob -> {
                    var cpb = cob.constantPool();
                    FloatEntry f1 = cpb.floatEntry(Float.NaN);
                    FloatEntry f2 = cpb.floatEntry(Float.NaN);
                    assertSame(f1, f2, "Float.NaN entries should be deduplicated to the same entry");
                    cob.return_();
                })));
    }

    @Test
    void testDoubleNaNDeduplication() {
        var cc = ClassFile.of();
        byte[] bytes = cc.build(ClassDesc.of("TestClass"), clb ->
            clb.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void),
                ClassFile.ACC_STATIC, mb -> mb.withCode(cob -> {
                    var cpb = cob.constantPool();
                    DoubleEntry d1 = cpb.doubleEntry(Double.NaN);
                    DoubleEntry d2 = cpb.doubleEntry(Double.NaN);
                    assertSame(d1, d2, "Double.NaN entries should be deduplicated to the same entry");
                    cob.return_();
                })));
    }

    @Test
    void testFloatNaNEquals() {
        var cc = ClassFile.of();
        byte[] bytes = cc.build(ClassDesc.of("TestClass1"), clb ->
            clb.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void),
                ClassFile.ACC_STATIC, mb -> mb.withCode(cob -> {
                    cob.constantPool().floatEntry(Float.NaN);
                    cob.return_();
                })));
        // Parse twice to get distinct FloatEntry objects from different pools
        ClassModel cm1 = cc.parse(bytes);
        ClassModel cm2 = cc.parse(bytes);
        FloatEntry fe1 = findEntry(cm1, FloatEntry.class);
        FloatEntry fe2 = findEntry(cm2, FloatEntry.class);

        assertNotSame(fe1, fe2, "Entries should be from different constant pools");
        assertTrue(Float.isNaN(fe1.floatValue()), "Entry should hold NaN");
        assertTrue(Float.isNaN(fe2.floatValue()), "Entry should hold NaN");
        assertEquals(fe1, fe2, "Two FloatEntry objects both holding NaN should be equal");
        assertEquals(fe1.hashCode(), fe2.hashCode(), "Equal entries must have same hashCode");
    }

    @Test
    void testDoubleNaNEquals() {
        var cc = ClassFile.of();
        byte[] bytes = cc.build(ClassDesc.of("TestClass2"), clb ->
            clb.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void),
                ClassFile.ACC_STATIC, mb -> mb.withCode(cob -> {
                    cob.constantPool().doubleEntry(Double.NaN);
                    cob.return_();
                })));
        ClassModel cm1 = cc.parse(bytes);
        ClassModel cm2 = cc.parse(bytes);
        DoubleEntry de1 = findEntry(cm1, DoubleEntry.class);
        DoubleEntry de2 = findEntry(cm2, DoubleEntry.class);

        assertNotSame(de1, de2, "Entries should be from different constant pools");
        assertTrue(Double.isNaN(de1.doubleValue()), "Entry should hold NaN");
        assertTrue(Double.isNaN(de2.doubleValue()), "Entry should hold NaN");
        assertEquals(de1, de2, "Two DoubleEntry objects both holding NaN should be equal");
        assertEquals(de1.hashCode(), de2.hashCode(), "Equal entries must have same hashCode");
    }

    @Test
    void testFloatPositiveAndNegativeZeroAreDistinct() {
        var cc = ClassFile.of();
        byte[] bytes = cc.build(ClassDesc.of("TestClass"), clb ->
            clb.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void),
                ClassFile.ACC_STATIC, mb -> mb.withCode(cob -> {
                    var cpb = cob.constantPool();
                    FloatEntry pos = cpb.floatEntry(0.0f);
                    FloatEntry neg = cpb.floatEntry(-0.0f);
                    assertNotEquals(pos.index(), neg.index(),
                        "Float +0.0 and -0.0 should be distinct constant pool entries");
                    assertNotEquals(pos, neg,
                        "Float +0.0 and -0.0 entries should not be equal");
                    cob.return_();
                })));
    }

    @Test
    void testDoublePositiveAndNegativeZeroAreDistinct() {
        var cc = ClassFile.of();
        byte[] bytes = cc.build(ClassDesc.of("TestClass"), clb ->
            clb.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void),
                ClassFile.ACC_STATIC, mb -> mb.withCode(cob -> {
                    var cpb = cob.constantPool();
                    DoubleEntry pos = cpb.doubleEntry(0.0d);
                    DoubleEntry neg = cpb.doubleEntry(-0.0d);
                    assertNotEquals(pos.index(), neg.index(),
                        "Double +0.0 and -0.0 should be distinct constant pool entries");
                    assertNotEquals(pos, neg,
                        "Double +0.0 and -0.0 entries should not be equal");
                    cob.return_();
                })));
    }

    @Test
    void testNormalFloatDeduplicationStillWorks() {
        var cc = ClassFile.of();
        cc.build(ClassDesc.of("TestClass"), clb ->
            clb.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void),
                ClassFile.ACC_STATIC, mb -> mb.withCode(cob -> {
                    var cpb = cob.constantPool();
                    FloatEntry f1 = cpb.floatEntry(1.5f);
                    FloatEntry f2 = cpb.floatEntry(1.5f);
                    assertSame(f1, f2, "Normal float entries should be deduplicated");
                    FloatEntry f3 = cpb.floatEntry(2.5f);
                    assertNotSame(f1, f3, "Different float values should not be deduplicated");
                    cob.return_();
                })));
    }

    @Test
    void testNormalDoubleDeduplicationStillWorks() {
        var cc = ClassFile.of();
        cc.build(ClassDesc.of("TestClass"), clb ->
            clb.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void),
                ClassFile.ACC_STATIC, mb -> mb.withCode(cob -> {
                    var cpb = cob.constantPool();
                    DoubleEntry d1 = cpb.doubleEntry(1.5d);
                    DoubleEntry d2 = cpb.doubleEntry(1.5d);
                    assertSame(d1, d2, "Normal double entries should be deduplicated");
                    DoubleEntry d3 = cpb.doubleEntry(2.5d);
                    assertNotSame(d1, d3, "Different double values should not be deduplicated");
                    cob.return_();
                })));
    }

    @Test
    void testNaNRoundTripThroughClassFile() {
        var cc = ClassFile.of();
        byte[] bytes = cc.build(ClassDesc.of("TestNaN"), clb ->
            clb.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void),
                ClassFile.ACC_STATIC, mb -> mb.withCode(cob -> {
                    cob.constantPool().floatEntry(Float.NaN);
                    cob.constantPool().doubleEntry(Double.NaN);
                    cob.return_();
                })));

        // Parse and verify NaN values survive round-trip
        ClassModel cm = cc.parse(bytes);
        FloatEntry fe = findEntry(cm, FloatEntry.class);
        DoubleEntry de = findEntry(cm, DoubleEntry.class);
        assertTrue(Float.isNaN(fe.floatValue()), "Float NaN should survive class file round-trip");
        assertTrue(Double.isNaN(de.doubleValue()), "Double NaN should survive class file round-trip");

        // Rebuild and verify NaN entries are deduplicated during transformation
        byte[] rebuilt = cc.transformClass(cm, ClassTransform.ACCEPT_ALL);
        ClassModel cm2 = cc.parse(rebuilt);
        FloatEntry fe2 = findEntry(cm2, FloatEntry.class);
        DoubleEntry de2 = findEntry(cm2, DoubleEntry.class);
        assertTrue(Float.isNaN(fe2.floatValue()), "Float NaN should survive rebuild");
        assertTrue(Double.isNaN(de2.doubleValue()), "Double NaN should survive rebuild");
        assertEquals(fe, fe2, "NaN float entries from original and rebuilt should be equal");
        assertEquals(de, de2, "NaN double entries from original and rebuilt should be equal");
    }

    @SuppressWarnings("unchecked")
    private static <T extends PoolEntry> T findEntry(ClassModel cm, Class<T> type) {
        var cp = cm.constantPool();
        for (int i = 1; i < cp.size(); i++) {
            PoolEntry e = cp.entryByIndex(i);
            if (type.isInstance(e)) {
                return (T) e;
            }
            i += e.width() - 1;
        }
        throw new AssertionError("No entry of type " + type.getSimpleName() + " found in constant pool");
    }
}
