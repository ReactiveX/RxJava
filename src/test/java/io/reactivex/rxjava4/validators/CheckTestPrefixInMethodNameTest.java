/*
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

package io.reactivex.rxjava4.validators;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.testsupport.TestHelper;

/**
 * Check verifying there are no methods with the prefix "test" in the name.
 */
public class CheckTestPrefixInMethodNameTest extends RxJavaTest {

    private static final String pattern = "void\\s+test[a-zA-Z0-9]";

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
            if (list != null) {

                for (File u : list) {
                    if (u.isDirectory()) {
                        dirs.offer(u);
                    } else {
                        String fileName = u.getName();
                        if (fileName.endsWith(".java")) {

                            int lineNum = 0;
                            //boolean found = false;
                            try (BufferedReader in = new BufferedReader(new FileReader(u))) {
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
                                                .append(fileName)
                                                .append("#L").append(lineNum)
                                                .append("    ").append(line)
                                                .append("\n");
                                        total++;
                                    }
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
