/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package test.bench;

import java.io.*;
import java.util.*;

import test.bench.util.*;
import test.bench.util.RunBench.RunType;
import test.bench.util.RunBench.SubArray;

import com.sun.max.program.option.*;

/**
 * Simple main program that reads a file of data, assumed to be originally generated by {@link RunBench} with the
 * {@code test.bench.file} property, and reports the same analysed results as {@link RunBench}.
 */

public class OfflineReport {

    private static final OptionSet options = new OptionSet(true);
    private static Option<String> fileOption = options.newStringOption("file", null, "file of data to be analyzed");
    private static Option<Boolean> outliersOption = options.newBooleanOption("outliers", false, "remove outlier values");


    public static void main(String[] args) {
        options.parseArguments(args).getArguments();
        String pathname = fileOption.getValue();
        if (pathname == null) {
            System.err.println("file argument must be present");
            System.exit(1);
        }
        System.setProperty(RunBench.WARMUP_COUNT_PROPERTY, "0");
        RunBench.getBenchProperties();
        try {
            final SubArray encapValues = readFile(pathname, RunType.ENCAP, 0, 0);
            final SubArray runValues = readFile(pathname, RunType.ACTUAL, 0, 0);
            RunBench.report(encapValues, runValues, "offline");
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    private static SubArray readFile(String baseName, RunType runType, int threadId, int runId) throws IOException {
        BufferedReader r = null;
        ArrayList<Long> values = new ArrayList<Long>();
        try {
            r = new BufferedReader(new FileReader(RunBench.fileOutputName(baseName, runType, threadId, runId)));
            while (true) {
                final String line = r.readLine();
                if (line == null) {
                    break;
                }
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                values.add(Long.parseLong(line));
            }
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {

                }
            }
        }
        final long[] result = new long[values.size()];
        int i = 0;
        for (Long value : values) {
            result[i++] = value;
        }
        if (outliersOption.getValue()) {
            return RunBench.removeOutliers(result);
        }
        return new RunBench.SubArray(result, 0, result.length);
    }

}
