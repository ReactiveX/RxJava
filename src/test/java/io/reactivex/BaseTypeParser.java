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

import java.io.File;
import java.util.*;

/**
 * Parses the java file of a reactive base type to allow discovering Javadoc mistakes algorithmically.
 */
public final class BaseTypeParser {

    private BaseTypeParser() {
        throw new IllegalStateException("No instances!");
    }

    public static class RxMethod {
        public String signature;

        public String backpressureKind;

        public String schedulerKind;

        public String javadoc;

        public String backpressureDocumentation;

        public String schedulerDocumentation;

        public int javadocLine;

        public int methodLine;

        public int backpressureDocLine;

        public int schedulerDocLine;
    }

    public static List<RxMethod> parse(File f, String baseClassName) throws Exception {
        List<RxMethod> list = new ArrayList<RxMethod>();

        StringBuilder b = JavadocForAnnotations.readFile(f);

        int baseIndex = b.indexOf("public abstract class " + baseClassName);

        if (baseIndex < 0) {
            throw new AssertionError("Wrong base class file: " + baseClassName);
        }

        for (;;) {
            RxMethod m = new RxMethod();

            int javadocStart = b.indexOf("/**", baseIndex);

            if (javadocStart < 0) {
                break;
            }

            int javadocEnd = b.indexOf("*/", javadocStart + 2);

            m.javadoc = b.substring(javadocStart, javadocEnd + 2);
            m.javadocLine = JavadocForAnnotations.lineNumber(b, javadocStart);

            int backpressureDoc = b.indexOf("<dt><b>Backpressure:</b></dt>", javadocStart);
            if (backpressureDoc > 0 && backpressureDoc < javadocEnd) {
                m.backpressureDocLine = JavadocForAnnotations.lineNumber(b, backpressureDoc);
                int nextDD = b.indexOf("</dd>", backpressureDoc);
                if (nextDD > 0 && nextDD < javadocEnd) {
                    m.backpressureDocumentation = b.substring(backpressureDoc, nextDD + 5);
                }
            }

            int schedulerDoc = b.indexOf("<dt><b>Scheduler:</b></dt>", javadocStart);
            if (schedulerDoc > 0 && schedulerDoc < javadocEnd) {
                m.schedulerDocLine = JavadocForAnnotations.lineNumber(b, schedulerDoc);
                int nextDD = b.indexOf("</dd>", schedulerDoc);
                if (nextDD > 0 && nextDD < javadocEnd) {
                    m.schedulerDocumentation = b.substring(schedulerDoc, nextDD + 5);
                }
            }

            int staticMethodDef = b.indexOf("public static ", javadocEnd + 2);
            if (staticMethodDef < 0) {
                staticMethodDef = Integer.MAX_VALUE;
            }
            int instanceMethodDef = b.indexOf("public final ", javadocEnd + 2);
            if (instanceMethodDef < 0) {
                instanceMethodDef = Integer.MAX_VALUE;
            }

            int javadocStartNext = b.indexOf("/**", javadocEnd + 2);
            if (javadocStartNext < 0) {
                javadocStartNext = Integer.MAX_VALUE;
            }

            int definitionStart = -1;

            if (staticMethodDef > 0 && staticMethodDef < javadocStartNext && staticMethodDef < instanceMethodDef) {
                definitionStart = staticMethodDef;
            }
            if (instanceMethodDef > 0 && instanceMethodDef < javadocStartNext && instanceMethodDef < staticMethodDef) {
                definitionStart = instanceMethodDef;
            }

            if (definitionStart > 0) {
                int methodDefEnd = b.indexOf("{", definitionStart);

                m.signature = b.substring(definitionStart, methodDefEnd + 1);

                m.methodLine = JavadocForAnnotations.lineNumber(b, definitionStart);

                int backpressureSpec = b.indexOf("@BackpressureSupport(", javadocEnd);
                if (backpressureSpec > 0 && backpressureSpec < definitionStart) {
                    int backpressureSpecEnd = b.indexOf(")", backpressureSpec + 21);
                    m.backpressureKind = b.substring(backpressureSpec + 21, backpressureSpecEnd);
                }

                int schhedulerSpec = b.indexOf("@SchedulerSupport(", javadocEnd);
                if (schhedulerSpec > 0 && schhedulerSpec < definitionStart) {
                    int schedulerSpecEnd = b.indexOf(")", schhedulerSpec + 18);
                    m.schedulerKind = b.substring(schhedulerSpec + 18, schedulerSpecEnd);
                }

                list.add(m);
                baseIndex = methodDefEnd;
            } else {
                baseIndex = javadocEnd + 2;
            }

        }

        return list;
    }
}
