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
 * You should have received a copy of the GNU General Public License
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit Oracle if you need additional information or have any questions.
 */

package compiler.c2;

import jdk.internal.util.DecimalDigits;

/*
 * @test
 * @bug 8350000
 * @summary Test DecimalDigits appendPair/appendQuad optimization correctness
 * @modules java.base/jdk.internal.util
 * @run main compiler.c2.TestDecimalDigitsMergeStores
 */

public class TestDecimalDigitsMergeStores {

    int pairValue = 42;
    int quadValue = 1234;

    public static void main(String[] args) {
        TestDecimalDigitsMergeStores test = new TestDecimalDigitsMergeStores();

        // Warm up
        for (int i = 0; i < 10000; i++) {
            test.testAppendPair();
            test.testAppendQuad();
            test.testMultipleAppendPair();
        }

        // Verify correctness
        test.verifyCorrectness();

        System.out.println("All tests passed!");
    }

    // Test: appendPair
    public String testAppendPair() {
        StringBuilder sb = new StringBuilder();
        DecimalDigits.appendPair(sb, pairValue);
        return sb.toString();
    }

    // Test: appendQuad
    public String testAppendQuad() {
        StringBuilder sb = new StringBuilder();
        DecimalDigits.appendQuad(sb, quadValue);
        return sb.toString();
    }

    // Test: Multiple appendPair calls in sequence
    public String testMultipleAppendPair() {
        StringBuilder sb = new StringBuilder();
        DecimalDigits.appendPair(sb, 12);
        DecimalDigits.appendPair(sb, 34);
        DecimalDigits.appendPair(sb, 56);
        return sb.toString();
    }

    public void verifyCorrectness() {
        StringBuilder sb = new StringBuilder();

        // Test appendPair
        sb.setLength(0);
        DecimalDigits.appendPair(sb, 0);
        if (!sb.toString().equals("00")) {
            throw new RuntimeException("Expected '00' but got '" + sb + "'");
        }

        sb.setLength(0);
        DecimalDigits.appendPair(sb, 5);
        if (!sb.toString().equals("05")) {
            throw new RuntimeException("Expected '05' but got '" + sb + "'");
        }

        sb.setLength(0);
        DecimalDigits.appendPair(sb, 42);
        if (!sb.toString().equals("42")) {
            throw new RuntimeException("Expected '42' but got '" + sb + "'");
        }

        sb.setLength(0);
        DecimalDigits.appendPair(sb, 99);
        if (!sb.toString().equals("99")) {
            throw new RuntimeException("Expected '99' but got '" + sb + "'");
        }

        // Test appendQuad
        sb.setLength(0);
        DecimalDigits.appendQuad(sb, 0);
        if (!sb.toString().equals("0000")) {
            throw new RuntimeException("Expected '0000' but got '" + sb + "'");
        }

        sb.setLength(0);
        DecimalDigits.appendQuad(sb, 5);
        if (!sb.toString().equals("0005")) {
            throw new RuntimeException("Expected '0005' but got '" + sb + "'");
        }

        sb.setLength(0);
        DecimalDigits.appendQuad(sb, 25);
        if (!sb.toString().equals("0025")) {
            throw new RuntimeException("Expected '0025' but got '" + sb + "'");
        }

        sb.setLength(0);
        DecimalDigits.appendQuad(sb, 123);
        if (!sb.toString().equals("0123")) {
            throw new RuntimeException("Expected '0123' but got '" + sb + "'");
        }

        sb.setLength(0);
        DecimalDigits.appendQuad(sb, 1234);
        if (!sb.toString().equals("1234")) {
            throw new RuntimeException("Expected '1234' but got '" + sb + "'");
        }

        sb.setLength(0);
        DecimalDigits.appendQuad(sb, 9999);
        if (!sb.toString().equals("9999")) {
            throw new RuntimeException("Expected '9999' but got '" + sb + "'");
        }
    }
}
