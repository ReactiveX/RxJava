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

package io.reactivex.rxjava3.validators;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Check if a {@code catch(Throwable} is followed by a
 * {@code Exceptions.throwIfFatal}, {@code Exceptions.wrapOrThrow}
 * or {@code fail} call.
 * @since 3.0.0
 */
public class CatchThrowIfFatalCheck {

    @Test
    public void check() throws Exception {
        File f = TestHelper.findSource("Flowable");
        if (f == null) {
            System.out.println("Unable to find sources of RxJava");
            return;
        }

        Queue<File> dirs = new ArrayDeque<>();

        StringBuilder fail = new StringBuilder();
        int errors = 0;

        File parent = f.getParentFile().getParentFile();

        dirs.offer(new File(parent.getAbsolutePath().replace('\\', '/')));

        while (!dirs.isEmpty()) {
            f = dirs.poll();

            File[] list = f.listFiles();
            if (list != null && list.length != 0) {

                for (File u : list) {
                    if (u.isDirectory()) {
                        dirs.offer(u);
                    } else {
                        List<String> lines = Files.readAllLines(u.toPath());

                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i).trim();

                            if (line.startsWith("} catch (Throwable ")) {
                                String next = lines.get(i + 1).trim();
                                boolean throwIfFatal = next.contains("Exceptions.throwIfFatal");
                                boolean wrapOrThrow = next.contains("ExceptionHelper.wrapOrThrow");
                                boolean failCall = next.startsWith("fail(");

                                if (!(throwIfFatal || wrapOrThrow || failCall)) {
                                    errors++;
                                    fail.append("Missing Exceptions.throwIfFatal\n    ")
                                    .append(next)
                                    .append("\n at ")
                                    .append(u.getName().replace(".java", ""))
                                    .append(".method(")
                                    .append(u.getName())
                                    .append(":")
                                    .append(i + 1)
                                    .append(")\n")
                                    ;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (errors != 0) {
            fail.insert(0, "Found " + errors + " cases\n");
            System.out.println(fail);
            throw new AssertionError(fail.toString());
        }
    }
}
