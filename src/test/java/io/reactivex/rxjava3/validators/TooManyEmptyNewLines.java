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
 * Test verifying there are no 2..5 empty newlines in the code.
 */
public class TooManyEmptyNewLines {

    @Test
    public void tooManyEmptyNewLines2() throws Exception  {
        findPattern(2);
    }

    @Test
    public void tooManyEmptyNewLines3() throws Exception  {
        findPattern(3);
    }

    @Test
    public void tooManyEmptyNewLines4() throws Exception  {
        findPattern(4);
    }

    @Test
    public void tooManyEmptyNewLines5() throws Exception  {
        findPattern(5);
    }

    static void findPattern(int newLines) throws Exception {
        File f = TestHelper.findSource("Flowable");
        if (f == null) {
            System.out.println("Unable to find sources of TestHelper.findSourceDir()");
            return;
        }

        Queue<File> dirs = new ArrayDeque<>();

        StringBuilder fail = new StringBuilder();
        fail.append("The following code pattern was found: ");
        fail.append("\\R");
        for (int i = 0; i < newLines; i++) {
            fail.append("\\R");
        }
        fail.append("\n");

        File parent = f.getParentFile().getParentFile();

        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/')));
        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/').replace("src/main/java", "src/test/java")));

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

                            List<String> lines = new ArrayList<>();
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

                            for (int i = 0; i < lines.size() - newLines; i++) {
                                String line1 = lines.get(i);
                                if (line1.isEmpty()) {
                                    int c = 1;
                                    for (int j = i + 1; j < lines.size(); j++) {
                                        if (lines.get(j).trim().isEmpty()) {
                                            c++;
                                        } else {
                                            break;
                                        }
                                    }

                                    if (c == newLines) {
                                        fail
                                        .append(fname)
                                        .append("#L").append(i + 1)
                                        .append("\n");
                                        total++;
                                        i += c;
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
