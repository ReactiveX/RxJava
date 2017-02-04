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

import static org.junit.Assert.fail;

import java.io.*;
import java.net.URL;

import org.junit.Test;

/**
 * Checks the source code of Maybe and finds unnecessary since 2.0 annotations in the
 * method's javadocs.
 */
public class MaybeNo2Dot0Since {

    /**
     * Given a base reactive type name, try to find its source in the current runtime
     * path and return a file to it or null if not found.
     * @param baseClassName the class name such as {@code Maybe}
     * @return the File pointing to the source
     * @throws Exception on error
     */
    public static File findSource(String baseClassName) throws Exception {
        URL u = MaybeNo2Dot0Since.class.getResource(MaybeNo2Dot0Since.class.getSimpleName() + ".class");

        String path = new File(u.toURI()).toString().replace('\\', '/');

//        System.out.println(path);

        int i = path.indexOf("/RxJava");
        if (i < 0) {
            System.out.println("Can't find the base RxJava directory");
            return null;
        }

        // find end of any potential postfix to /RxJava
        int j = path.indexOf("/", i + 6);

        String p = path.substring(0, j + 1) + "src/main/java/io/reactivex/" + baseClassName + ".java";

        File f = new File(p);

        if (!f.canRead()) {
            System.out.println("Can't read " + p);
            return null;
        }

        return f;
    }

    @Test
    public void noSince20InMaybe() throws Exception {

        File f = findSource(Maybe.class.getSimpleName());

        String line;

        StringBuilder b = new StringBuilder();

        boolean classDefPassed = false;

        BufferedReader in = new BufferedReader(new FileReader(f));
        try {
            int ln = 1;
            while (true) {
                line = in.readLine();

                if (line == null) {
                    break;
                }

                if (line.startsWith("public abstract class Maybe<")) {
                    classDefPassed = true;
                }

                if (classDefPassed) {
                    if (line.contains("@since") && line.contains("2.0") && !line.contains("2.0.")) {
                        b.append("java.lang.RuntimeException: @since 2.0 found").append("\r\n")
                        .append(" at io.reactivex.Maybe (Maybe.java:").append(ln).append(")\r\n\r\n");
                        ;
                    }
                }

                ln++;
            }
        } finally {
            in.close();
        }

        if (b.length() != 0) {
            System.out.println(b);

            fail(b.toString());
        }
    }
}
