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

import static org.junit.Assert.fail;

import java.io.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Checks the source code of Maybe and finds unnecessary since 2.0 annotations in the
 * method's javadocs.
 */
public class MaybeNo2Dot0Since {

    @Test
    public void noSince20InMaybe() throws Exception {

        File f = TestHelper.findSource(Maybe.class.getSimpleName());

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
