/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 7, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"-Xms512M", "-Xmx512M"})
public class CharSequenceCharAtBenchmark {

    @Param(value = {"ascii", "non-ascii"})
    public String data;

    @Param(value = {"String", "StringBuffer"})
    public String source;

    private CharSequence sequence;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        StringBuilder sb = new StringBuilder(3152);
        for (int i = 0; i < 3152; ++i) {
            char c = (char) i;
            if ("ascii".equals(data)) {
                c = (char) (i & 0x7f);
            }
            sb.append(c);
        }

        switch(source) {
            case "String":
                sequence = sb.toString();
                break;
            case "StringBuffer":
                sequence = sb;
                break;
            default:
                throw new IllegalArgumentException(source);
        }
    }

    @Benchmark
    public int test() {
        var sequence = this.sequence;
        int sum = 0;
        for (int i = 0, j = sequence.length(); i < j; ++i) {
            sum += sequence.charAt(i);
        }
        return sum;
    }
}