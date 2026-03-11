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

/**
 * @test
 * @bug 9999901
 * @summary Mac.doFinal(byte[], int) should reject negative outOffset
 * @run main MacDoFinalNegativeOffset
 */

import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public class MacDoFinalNegativeOffset {

    public static void main(String[] args) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        byte[] keyBytes = new byte[32];
        SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");

        // Test offset = -1: should throw ShortBufferException
        mac.init(key);
        mac.update(new byte[]{1, 2, 3});
        int macLen = mac.getMacLength();
        byte[] output = new byte[macLen + 64];

        try {
            mac.doFinal(output, -1);
            throw new RuntimeException("Expected ShortBufferException for offset=-1");
        } catch (ShortBufferException e) {
            System.out.println("PASS: offset=-1 threw ShortBufferException");
        }

        // Test offset = Integer.MIN_VALUE
        mac.init(key);
        mac.update(new byte[]{1, 2, 3});
        try {
            mac.doFinal(output, Integer.MIN_VALUE);
            throw new RuntimeException("Expected ShortBufferException for MIN_VALUE");
        } catch (ShortBufferException e) {
            System.out.println("PASS: offset=MIN_VALUE threw ShortBufferException");
        }

        // Test valid offset still works
        mac.init(key);
        mac.update(new byte[]{1, 2, 3});
        mac.doFinal(output, 0);
        System.out.println("PASS: offset=0 succeeded normally");
    }
}
