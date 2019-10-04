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

package io.reactivex.rxjava3.validators;

import java.io.*;
import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Detect two or more practically empty lines: {@code \s+\R\s+\R\s+\R}.
 *
 */
public class TooManyNewlinesTest {

    @Test
    public void verifySources() throws Exception {
        File f = TestHelper.findSource("Flowable");
        if (f == null) {
            System.out.println("Unable to find sources of RxJava");
            return;
        }

        Queue<File> dirs = new ArrayDeque<File>();

        File parent = f.getParentFile().getParentFile();

        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/')));
        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/').replace("src/main/java", "src/test/java")));

        StringBuilder fail = new StringBuilder();
        fail.append("The following code pattern was found:\n    .\\R\\s+\\R\\s+\\R\n");
        int total = 0;

        while (!dirs.isEmpty()) {
            f = dirs.poll();

            File[] list = f.listFiles();
            if (list != null && list.length != 0) {

                for (File u : list) {
                    if (u.isDirectory()) {
                        dirs.offer(u);
                    } else {
                        String fname = u.getName();
                        if (fname.endsWith(".java")) {

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

                            for (int i = 0; i < lines.size() - 2; i++) {
                                String line1 = lines.get(i).trim();
                                String line2 = lines.get(i + 1).trim();
                                if (line1.length() == 0 && line2.length() == 0) {
                                    fail
                                    .append(fname)
                                    .append("#L").append(i + 1)
                                    .append("\n");
                                    total++;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (total != 0) {
            fail.append("Found ")
            .append(total)
            .append(" instances");
            System.out.println(fail);
            throw new AssertionError(fail.toString());
        }
    }
}
