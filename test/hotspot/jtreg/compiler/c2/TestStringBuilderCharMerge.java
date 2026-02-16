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

/*
 * @test
 * @bug 8350000
 * @summary Test StringBuilder char merging optimization correctness
 * @run main compiler.c2.TestStringBuilderCharMerge
 */

public class TestStringBuilderCharMerge {

    char c1 = 'a';
    char c2 = 'b';
    char c3 = 'c';
    char c4 = 'd';

    public static void main(String[] args) {
        TestStringBuilderCharMerge test = new TestStringBuilderCharMerge();

        // Warm up
        for (int i = 0; i < 10000; i++) {
            test.testTwoCharsChained();
            test.testFourCharsChained();
            test.testFourCharsNonChained();
            test.testConstantString2();
            test.testConstantString4();
        }

        // Verify correctness
        test.verifyCorrectness();

        System.out.println("All tests passed!");
    }

    // Test: Two consecutive append(char) calls
    public String testTwoCharsChained() {
        return new StringBuilder()
            .append(c1)
            .append(c2)
            .toString();
    }

    // Test: Four consecutive append(char) calls
    public String testFourCharsChained() {
        return new StringBuilder()
            .append(c1)
            .append(c2)
            .append(c3)
            .append(c4)
            .toString();
    }

    // Test: Non-chained (separate) append calls
    public String testFourCharsNonChained() {
        StringBuilder sb = new StringBuilder();
        sb.append(c1);
        sb.append(c2);
        sb.append(c3);
        sb.append(c4);
        return sb.toString();
    }

    // Test: append(String) with constant length 2 string
    public String testConstantString2() {
        return new StringBuilder()
            .append("ab")
            .toString();
    }

    // Test: append(String) with constant length 4 string
    public String testConstantString4() {
        return new StringBuilder()
            .append("abcd")
            .toString();
    }

    public void verifyCorrectness() {
        // Test 2 chars
        String result2 = new StringBuilder().append('a').append('b').toString();
        if (!result2.equals("ab")) {
            throw new RuntimeException("Expected 'ab' but got '" + result2 + "'");
        }

        // Test 4 chars
        String result4 = new StringBuilder().append('a').append('b').append('c').append('d').toString();
        if (!result4.equals("abcd")) {
            throw new RuntimeException("Expected 'abcd' but got '" + result4 + "'");
        }

        // Test constant strings
        String resultConst2 = new StringBuilder().append("xy").toString();
        if (!resultConst2.equals("xy")) {
            throw new RuntimeException("Expected 'xy' but got '" + resultConst2 + "'");
        }

        String resultConst4 = new StringBuilder().append("wxyz").toString();
        if (!resultConst4.equals("wxyz")) {
            throw new RuntimeException("Expected 'wxyz' but got '" + resultConst4 + "'");
        }

        // Test with non-Latin1 chars
        String resultUtf16 = new StringBuilder().append('\u1234').append('\u5678').toString();
        if (!resultUtf16.equals("\u1234\u5678")) {
            throw new RuntimeException("Expected '\\u1234\\u5678' but got '" + resultUtf16 + "'");
        }
    }
}
