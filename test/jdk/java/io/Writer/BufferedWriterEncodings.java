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

/* @test
   @summary test BufferedWriter + OutputStreamWriter under various character encodings
*/

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class BufferedWriterEncodings {
    public static void main(String[] args) throws Exception {
        Charset[] charset_array = new Charset[] {
                StandardCharsets.UTF_8,
                StandardCharsets.UTF_16,
                StandardCharsets.UTF_16LE,
                StandardCharsets.UTF_16BE,
                StandardCharsets.UTF_32,
                StandardCharsets.UTF_32BE,
                StandardCharsets.UTF_32LE,
                Charset.forName("GBK"),
                Charset.forName("GB2312"),
                Charset.forName("GB18030"),
                Charset.forName("Big5")
        };

        int[] size_array = new int[]{1};
        for (Charset charset : charset_array) {
            String[] hex_array = hex_strings(charset.name());

            for (String hex : hex_array) {
                byte[] hex_bytes = HexFormat.of().parseHex(hex);
                String hex_string = new String(hex_bytes, charset);

                String[] strings = new String[size_array.length * 2];
                for (int i = 0; i < size_array.length; i++) {
                    int size = size_array[i];
                    String ascii_leading = "a".repeat(size).concat(hex_string.repeat(size));
                    String mix = hex_string.repeat(size).concat(ascii_leading).concat("a".repeat(size));

                    strings[i * 2] = ascii_leading;
                    strings[i * 2 + 1] = mix;
                }

                for (String string : strings) {
                    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytesOut, charset))) {
                        writer.write(string);
                        writer.flush();
                    } catch (Throwable e) {
                        throw new Exception("charset: " + charset + ", hex: " + hex + ", hex_string " + hex_string, e);
                    }
                    byte[] out_bytes = bytesOut.toByteArray();
                    String out_string = new String(out_bytes, charset);
                    if (!string.equals(out_string)) {
                        throw new Exception("charset: " + charset + ", string: " + string + ", out_string: " + out_string);
                    }
                }
            }
        }
    }


    static String[] hex_strings(String charsetName) {
        return switch (charsetName) {
            case "UTF-8", "UTF-16", "UTF-16LE", "UTF-16BE", "UTF-32", "UTF-32BE", "UTF-32LE" -> new String[]{
                    "00", "78", "7f", // ascii
                    "c2a9", // utf8_2_bytes
                    "e6b8a9", // utf8_3_bytes
            };
            case "GBK", "GB2312" -> new String[]{
                    "00", "78", "7f", // ascii
                    "c1fa", "cdf2", // GBK/GB2312 2 bytes
            };
            case "GB18030" -> new String[]{
                    "00", "78", "7f", // ascii
                    "c1fa", "cdf2", "a0c4", "fd93", "a994", // GB18030 2 bytes
                    "8132e834", "82359833", "8134d630", "92319134", "9439fc36", "a1de", "95329031" // GB18030 4 bytes
            };
            case "Big5" -> new String[]{
                    "00", "78", "7f", // ascii
                    "a374", "a5fe", "c6a6", // Big5 2 bytes
            };
            default -> new String[]{"00", "01", "78", "7f"};
        };
    }
}
