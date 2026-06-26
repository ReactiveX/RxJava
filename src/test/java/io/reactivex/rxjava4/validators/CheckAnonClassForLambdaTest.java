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
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

import io.reactivex.rxjava4.core.RxJavaTest;
import org.junit.Test;

import io.reactivex.rxjava4.testsupport.TestHelper;

/**
 * Adds license header to java files.
 */
public class CheckAnonClassForLambdaTest extends RxJavaTest {

    @Test
    public void checkAndUpdateLicenses() throws Exception {
        if (System.getenv("CI") != null) {
            // no point in changing the files in CI
            return;
        }
        File f = TestHelper.findSource("Flowable");
        if (f == null) {
            return;
        }

        Queue<File> dirs = new ArrayDeque<>();

        File parent = f.getParentFile().getParentFile();
        dirs.offer(parent);
        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/').replace("src/main/java", "src/perf/java")));
        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/').replace("src/main/java", "src/test/java")));

        var fail = new StringBuilder();
        var regex = Pattern.compile("new\\s+\\w+(?:\\s*<(?:[\\s\\w<>(\\[\\])?,.?]|\\s*<[\\s\\w<>(\\[\\])?,.?]*>)*>)?\\s*\\([^)]*\\)\\s*\\{");
        int total = 0;

        while (!dirs.isEmpty()) {
            f = dirs.poll();

            File[] list = f.listFiles();
            if (list != null) {

                for (File u : list) {
                    if (u.isDirectory()) {
                        dirs.offer(u);
                    } else {
                        if (u.getName().endsWith(".java")) {

                            List<String> lines = Files.readAllLines(u.toPath());

                            for (int i = 0; i < lines.size(); i++) {

                                String input = lines.get(i);
                                if (input.trim().startsWith("*")) {
                                    continue;
                                }
                                if (regex.matcher(input).find()) {
                                    if (fail.isEmpty()) {
                                        fail.append("java.lang.RuntimeException lambda possibility\r\n");
                                    }
                                    fail.append(" at ").append(u.getName())
                                    .append("(").append(u.getName()).append(":").append(i + 1)
                                    .append(")\r\n");
                                    total++;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!fail.isEmpty()) {
            System.out.println(fail);
            System.out.println(total);
            throw new AssertionError(fail.toString());
        }
    }
}
