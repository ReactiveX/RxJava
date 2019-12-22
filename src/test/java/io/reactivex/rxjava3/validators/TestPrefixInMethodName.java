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
import java.util.regex.*;

import org.junit.Test;

import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Check verifying there are no methods with the prefix "test" in the name.
 */
public class TestPrefixInMethodName {

    private static final String pattern = "void\\s+test[a-zA-Z0-9]";
    private static final String replacement = "void ";

    @Test
    public void checkAndUpdateTestMethodNames() throws Exception {
        File f = TestHelper.findSource("Flowable");
        if (f == null) {
            System.out.println("Unable to find sources of RxJava");
            return;
        }

        Queue<File> dirs = new ArrayDeque<>();

        StringBuilder fail = new StringBuilder();
        fail.append("The following code pattern was found: ").append(pattern).append("\n");
        fail.append("Refresh and re-run tests!\n\n");

        File parent = f.getParentFile().getParentFile();

        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/')));
        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/').replace("src/main/java", "src/test/java")));

        Pattern p = Pattern.compile(pattern);

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

                            int lineNum = 0;
                            List<String> lines = new ArrayList<>();
                            BufferedReader in = new BufferedReader(new FileReader(u));
                            //boolean found = false;
                            try {
                                for (; ; ) {
                                    String line = in.readLine();
                                    if (line == null) {
                                        break;
                                    }
                                    lineNum++;

                                    Matcher matcher = p.matcher(line);
                                    if (!line.startsWith("//") && !line.startsWith("*") && matcher.find()) {
                                        // found = true;
                                        fail
                                                .append(fname)
                                                .append("#L").append(lineNum)
                                                .append("    ").append(line)
                                                .append("\n");
                                        total++;

                                        int methodNameStartIndex = matcher.end() - 1;
                                        char firstChar = Character.toLowerCase(line.charAt(methodNameStartIndex));

                                        String newLine = matcher.replaceAll(replacement + firstChar);

                                        lines.add(newLine);
                                    } else {
                                        lines.add(line);
                                    }

                                }
                            } finally {
                                in.close();
                            }

                            /*if (found && System.getenv("CI") == null) {
                                PrintWriter w = new PrintWriter(new FileWriter(u));

                                try {
                                    for (String s : lines) {
                                        w.println(s);
                                    }
                                } finally {
                                    w.close();
                                }
                            }*/
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
