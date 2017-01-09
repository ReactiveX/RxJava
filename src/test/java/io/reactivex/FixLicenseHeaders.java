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
public class FixLicenseHeaders {

    String[] header = {
    "/**",
    " * Copyright (c) 2016-present, RxJava Contributors.",
    " *",
    " * Licensed under the Apache License, Version 2.0 (the \"License\"); you may not use this file except in",
    " * compliance with the License. You may obtain a copy of the License at",
    " *",
    " * http://www.apache.org/licenses/LICENSE-2.0",
    " *",
    " * Unless required by applicable law or agreed to in writing, software distributed under the License is",
    " * distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See",
    " * the License for the specific language governing permissions and limitations under the License.",
    " */",
    ""
    };

    @Test
    public void checkAndUpdateLicenses() throws Exception {
        if (System.getenv("CI") != null) {
            // no point in changing the files in CI
            return;
        }
        File f = MaybeNo2Dot0Since.findSource("Flowable");
        if (f == null) {
            return;
        }

        Queue<File> dirs = new ArrayDeque<File>();

        File parent = f.getParentFile();
        dirs.offer(parent);
        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/').replace("src/main/java", "src/perf/java")));
        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/').replace("src/main/java", "src/test/java")));

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

                            if (!lines.get(0).equals(header[0]) && !lines.get(1).equals(header[1])) {
                                fail.append("java.lang.RuntimeException: missing header added, refresh and re-run tests!\r\n")
                                .append(" at ")
                                ;

                                String fn = u.toString().replace('\\', '/');

                                int idx = fn.indexOf("io/reactivex/");

                                fn = fn.substring(idx).replace('/', '.').replace(".java", "");

                                fail.append(fn).append(" (")
                                ;

                                int jdx = fn.lastIndexOf('.');

                                fail.append(fn.substring(jdx + 1));

                                fail.append(".java:1)\r\n\r\n");

                                lines.addAll(0, Arrays.asList(header));

                                PrintWriter w = new PrintWriter(new FileWriter(u));

                                try {
                                    for (String s : lines) {
                                        w.println(s);
                                    }
                                } finally {
                                    w.close();
                                }
                            }
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
}
