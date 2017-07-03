/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex;
import java.io.*;
import java.util.*;

import org.junit.Test;

public class JavadocFindUnescapedAngleBrackets {

    @Test
    public void find() throws Exception {
        File base = MaybeNo2Dot0Since.findSource("Flowable");

        if (base == null) {
            return;
        }

        base = base.getParentFile();

        Queue<File[]> files = new ArrayDeque<File[]>();

        files.offer(base.listFiles());

        StringBuilder b = new StringBuilder();
        int count = 0;

        while (!files.isEmpty()) {
            for (File file : files.poll()) {
                if (file.getName().endsWith(".java")) {

                    String s = readFile(file);

                    String fn = file.toString().substring(base.toString().length());
                    fn = fn.replace("\\", ".");
                    fn = fn.replace("//", ".");
                    fn = fn.replace(".java", "");
                    fn = "io.reactivex" + fn;

                    int j = 0;
                    for (;;) {
                        int idx = s.indexOf("<code>", j);
                        if (idx < 0) {
                            break;
                        }
                        int jdx = s.indexOf("</code>", idx + 6);

                        int k = idx + 6;
                        for (;;) {
                            int kdx = s.indexOf('>', k);
                            if (kdx < 0) {
                                break;
                            }

                            if (kdx < jdx) {
                                b.append("at ")
                                .append(fn)
                                .append(".gt(")
                                .append(file.getName()).append(":")
                                .append(countLine(s, kdx))
                                .append(")\r\n");
                                count++;
                            } else {
                                break;
                            }
                            k = kdx + 1;
                        }

                        k = idx + 6;
                        for (;;) {
                            int kdx = s.indexOf('<', k);
                            if (kdx < 0) {
                                break;
                            }

                            if (kdx < jdx) {
                                b.append("at ")
                                .append(fn)
                                .append(".lt(")
                                .append(file.getName()).append(":")
                                .append(countLine(s, kdx))
                                .append(")\r\n");
                                count++;
                            } else {
                                break;
                            }
                            k = kdx + 1;
                        }

                        j = jdx + 7;
                    }
                }
            }
        }

        if (b.length() > 0) {
            System.err.println("Should escape < and > in <code> blocks! " + count);
            System.err.println(b);
            throw new Exception("Should escape < and > in <code> blocks! " + count + "\r\n" + b);
        }
    }

    static int countLine(String s, int kdx) {
        int c = 1;
        for (int i = kdx; i >= 0; i--) {
            if (s.charAt(i) == '\n') {
                c++;
            }
        }
        return c;
    }

    static String readFile(File f) throws IOException {
        StringBuilder b = new StringBuilder((int)f.length());
        BufferedReader in = new BufferedReader(new FileReader(f));
        try {
            String line = null;

            while ((line = in.readLine()) != null) {
                b.append(line).append("\n");
            }

        } finally {
            in.close();
        }
        return b.toString();
    }
}
