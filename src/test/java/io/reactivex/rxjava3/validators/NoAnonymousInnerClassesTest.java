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
import java.net.URL;
import java.util.*;

import org.junit.Test;

public class NoAnonymousInnerClassesTest {

    @Test
    public void verify() throws Exception {
        URL u = NoAnonymousInnerClassesTest.class.getResource("/");
        File f = new File(u.toURI());

        String fs = f.toString().toLowerCase().replace("\\", "/");

        System.out.println("Found " + fs);

        // running this particular test from IntelliJ will have the wrong class directory
        // gradle will generate test classes into a separate directory too
        int idx = fs.indexOf("/test");
        if (idx >= 0) {
            f = new File(fs.substring(0, idx));
        }

        StringBuilder b = new StringBuilder("Anonymous inner classes found:");

        Queue<File> queue = new ArrayDeque<>();

        queue.offer(f);

        String prefix = f.getAbsolutePath();
        int count = 0;
        while (!queue.isEmpty()) {

            f = queue.poll();

            if (f.isDirectory()) {
                File[] dir = f.listFiles();
                if (dir != null && dir.length != 0) {
                    for (File g : dir) {
                        queue.offer(g);
                    }
                }
            } else {
                String name = f.getName();
                if (name.endsWith(".class") && name.contains("$")
                        && !name.contains("Perf") && !name.contains("Test")
                        && !name.startsWith("Test")) {
                    String baseName = name.substring(0, name.length() - 6);
                    String[] parts = name.split("\\$");
                    for (String s : parts) {
                        if (Character.isDigit(s.charAt(0))) {
                            String n = f.getAbsolutePath().substring(prefix.length()).replace('\\', '.').replace('/', '.');
                            if (n.startsWith(".")) {
                                n = n.substring(1);
                            }

                            // javac generates switch-map anonymous classes with the same $x.class pattern
                            // we have to look into the file and search for $SwitchMap$

                            boolean found = false;

                            FileInputStream fin = new FileInputStream(f);
                            try {
                                byte[] data = new byte[fin.available()];
                                fin.read(data);

                                String content = new String(data, "ISO-8859-1");

                                if (content.contains("$SwitchMap$")) {
                                    // the parent class can reference these synthetic inner classes
                                    // and thus they also have $SwitchMap$
                                    // but the synthetic inner classes should not have further inner classes

                                    File[] filesInTheSameDir = f.getParentFile().listFiles();

                                    for (File fsame : filesInTheSameDir) {
                                        String fsameName = fsame.getName();
                                        if (fsameName.endsWith(".class")) {
                                            fsameName = fsameName.substring(0, fsameName.length() - 6);

                                            if (fsameName.startsWith(baseName)
                                                    && fsameName.length() > baseName.length() + 1
                                                    && fsameName.charAt(baseName.length()) == '$'
                                                    && Character.isDigit(fsameName.charAt(baseName.length() + 1))) {
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    found = true;
                                }
                            } finally {
                                fin.close();
                            }

                            if (found) {
                                b.append("\r\n").append(n);
                                count++;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (count != 0) {
            throw new AssertionError(b.toString());
        }
    }
}
