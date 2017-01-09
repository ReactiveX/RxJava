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

/**
 * Adds license header to java files.
 */
public class TextualAorAn {

    @Test
    public void checkFiles() throws Exception {
        File f = MaybeNo2Dot0Since.findSource("Flowable");
        if (f == null) {
            return;
        }

        Queue<File> dirs = new ArrayDeque<File>();

        File parent = f.getParentFile();
        dirs.offer(parent);
//        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/').replace("src/main/java", "src/perf/java")));
//        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/').replace("src/main/java", "src/test/java")));

        StringBuilder fail = new StringBuilder();

        while (!dirs.isEmpty()) {
            f = dirs.poll();

            File[] list = f.listFiles();
            if (list != null && list.length != 0) {

                for (File u : list) {
                    if (u.isDirectory()) {
                        dirs.offer(u);
                    } else {
                        if (u.getName().endsWith(".java")) {

                            List<String> lines = new ArrayList<String>();
                            BufferedReader in = new BufferedReader(new FileReader(u));
                            try {
                                for (;;) {
                                    String line = in.readLine();
                                    if (line == null) {
                                        break;
                                    }

                                    lines.add(line);
                                }
                            } finally {
                                in.close();
                            }

                            String clazz = u.getAbsolutePath().replace('\\', '/');
                            int idx = clazz.indexOf("/io/reactivex/");
                            clazz = clazz.substring(idx + 14).replace(".java", "");

                            processFile(fail, lines, clazz, u.getName());
                        }
                    }
                }
            }
        }

        if (fail.length() != 0) {
            System.out.println(fail);
            throw new AssertionError(fail.toString());
        }
    }

    static void processFile(StringBuilder b, List<String> lines, String className, String fileName) {
        int i = 1;
        for (String s : lines) {

            if (s.contains(" a Observer")) {
                b.append("java.lang.RuntimeException: ' a Observer'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains("A Observer")) {
                b.append("java.lang.RuntimeException: 'A Observer'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains(" a Observable")) {
                b.append("java.lang.RuntimeException: ' a Observable'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains("A Observable")) {
                b.append("java.lang.RuntimeException: 'A Observable'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains(" an Subscriber")) {
                b.append("java.lang.RuntimeException: ' an Subscriber'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains("An Subscriber")) {
                b.append("java.lang.RuntimeException: 'An Subscriber'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains(" an Publisher")) {
                b.append("java.lang.RuntimeException: ' an Publisher'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains("An Publisher")) {
                b.append("java.lang.RuntimeException: 'An Publisher'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains(" an Flowable")) {
                b.append("java.lang.RuntimeException: ' an Flowable'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains("An Flowable")) {
                b.append("java.lang.RuntimeException: 'An Flowable'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains(" an Single")) {
                b.append("java.lang.RuntimeException: ' an Single'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains("An Single")) {
                b.append("java.lang.RuntimeException: 'An Single'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains(" an Maybe")) {
                b.append("java.lang.RuntimeException: ' an Maybe'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains("An Maybe")) {
                b.append("java.lang.RuntimeException: 'An Maybe'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains(" an Completable")) {
                b.append("java.lang.RuntimeException: ' an Completable'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains("An Completable")) {
                b.append("java.lang.RuntimeException: 'An Completable'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            if (s.contains(" an cancel")) {
                b.append("java.lang.RuntimeException: ' an cancel'\r\n at io.reactivex.")
                .append(className).append(" (").append(fileName).append(":").append(i).append(")\r\n");
                ;
            }

            i++;
        }
    }
}
