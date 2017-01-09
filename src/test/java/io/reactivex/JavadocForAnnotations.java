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

import org.junit.*;

/**
 * Checks the source code of the base reactive types and locates missing
 * mention of {@code Backpressure:} and {@code Scheduler:} of methods.
 */
public class JavadocForAnnotations {

    static void checkSource(String baseClassName, boolean scheduler) throws Exception {
        File f = MaybeNo2Dot0Since.findSource(baseClassName);
        if (f == null) {
            return;
        }

        StringBuilder b = readFile(f);

        StringBuilder e = new StringBuilder();

        if (scheduler) {
            scanFor(b, "@SchedulerSupport", "Scheduler:", e, baseClassName);
        } else {
            scanFor(b, "@BackpressureSupport", "Backpressure:", e, baseClassName);
        }

        if (e.length() != 0) {
            System.out.println(e);

            fail(e.toString());
        }
    }

    public static StringBuilder readFile(File f) throws Exception {
        StringBuilder b = new StringBuilder();

        BufferedReader in = new BufferedReader(new FileReader(f));
        try {
            for (;;) {
                String line = in.readLine();

                if (line == null) {
                    break;
                }

                b.append(line).append('\n');
            }
        } finally {
            in.close();
        }

        return b;
    }

    static final void scanFor(StringBuilder sourceCode, String annotation, String inDoc,
            StringBuilder e, String baseClassName) {
        int index = 0;
        for (;;) {
            int idx = sourceCode.indexOf(annotation, index);

            if (idx < 0) {
                break;
            }

            int j = sourceCode.lastIndexOf("/**", idx);

            // see if the last /** is not before the index (last time the annotation was found
            // indicating an uncommented method like subscribe()
            if (j > index) {
                int k = sourceCode.indexOf(inDoc, j);

                if (k < 0 || k > idx) {
                    // when printed on the console, IDEs will create a clickable link to help navigate to the offending point
                    e.append("java.lang.RuntimeException: missing ").append(inDoc).append(" section\r\n")
                    ;
                    int lc = lineNumber(sourceCode, idx);

                    e.append(" at io.reactivex.").append(baseClassName)
                    .append(" (").append(baseClassName).append(".java:")
                    .append(lc).append(")").append("\r\n\r\n");
                }
            }

            index = idx + annotation.length();
        }
    }


    static final void scanForBadMethod(StringBuilder sourceCode, String annotation, String inDoc,
            StringBuilder e, String baseClassName) {
        int index = 0;
        for (;;) {
            int idx = sourceCode.indexOf(annotation, index);

            if (idx < 0) {
                break;
            }

            int j = sourceCode.lastIndexOf("/**", idx);

            // see if the last /** is not before the index (last time the annotation was found
            // indicating an uncommented method like subscribe()
            if (j > index) {
                int k = sourceCode.indexOf(inDoc, j);

                if (k >= 0 && k <= idx) {

                    int ll = sourceCode.indexOf("You specify", k);
                    int lm = sourceCode.indexOf("This operator", k);
                    if ((ll < 0 || ll > idx) && (lm < 0 || lm > idx)) {

                        int n = sourceCode.indexOf("{@code ", k);

                        if (n < idx) {
                            int m = sourceCode.indexOf("}", n);

                            if (m < idx) {
                                String mname = sourceCode.substring(n + 7, m);

                                int q = sourceCode.indexOf("@SuppressWarnings({", idx);

                                int o = sourceCode.indexOf("{", idx);

                                if (q + 18 == o) {
                                    o = sourceCode.indexOf("{", q + 20);
                                }

                                if (o >= 0) {

                                    int p = sourceCode.indexOf(" " + mname + "(", idx);

                                    if (p < 0 || p > o) {
                                        // when printed on the console, IDEs will create a clickable link to help navigate to the offending point
                                        e.append("java.lang.RuntimeException: wrong method name in description of ").append(inDoc).append(" '").append(mname).append("'\r\n")
                                        ;
                                        int lc = lineNumber(sourceCode, idx);

                                        e.append(" at io.reactivex.").append(baseClassName)
                                        .append(" (").append(baseClassName).append(".java:")
                                        .append(lc).append(")").append("\r\n\r\n");
                                    }
                                }
                            }

                        }

                    }
                }
            }

            index = idx + annotation.length();
        }
    }

    static void checkSchedulerBadMethod(String baseClassName) throws Exception {
        File f = MaybeNo2Dot0Since.findSource(baseClassName);
        if (f == null) {
            return;
        }

        StringBuilder b = readFile(f);

        StringBuilder e = new StringBuilder();

        scanForBadMethod(b, "@SchedulerSupport", "Scheduler:", e, baseClassName);

        if (e.length() != 0) {
            System.out.println(e);

            fail(e.toString());
        }
    }

    public static int lineNumber(StringBuilder s, int index) {
        int cnt = 1;
        for (int i = 0; i < index; i++) {
            if (s.charAt(i) == '\n') {
                cnt++;
            }
        }
        return cnt;
    }

    @Test
    public void checkFlowableBackpressure() throws Exception {
        checkSource(Flowable.class.getSimpleName(), false);
    }

    @Test
    public void checkFlowableScheduler() throws Exception {
        checkSource(Flowable.class.getSimpleName(), true);
    }

    @Test
    public void checkObservableBackpressure() throws Exception {
        checkSource(Observable.class.getSimpleName(), false);
    }

    @Test
    public void checkObservableScheduler() throws Exception {
        checkSource(Observable.class.getSimpleName(), true);
    }

    @Test
    public void checkSingleBackpressure() throws Exception {
        checkSource(Single.class.getSimpleName(), false);
    }

    @Test
    public void checkSingleScheduler() throws Exception {
        checkSource(Single.class.getSimpleName(), true);
    }

    @Test
    public void checkCompletableBackpressure() throws Exception {
        checkSource(Completable.class.getSimpleName(), false);
    }

    @Test
    public void checkCompletableScheduler() throws Exception {
        checkSource(Completable.class.getSimpleName(), true);
    }

    @Test
    public void checkMaybeBackpressure() throws Exception {
        checkSource(Maybe.class.getSimpleName(), false);
    }

    @Test
    public void checkMaybeScheduler() throws Exception {
        checkSource(Maybe.class.getSimpleName(), true);
    }

    @Test
    public void checkFlowableSchedulerDoc() throws Exception {
        checkSchedulerBadMethod(Flowable.class.getSimpleName());
    }

    @Test
    public void checkObservableSchedulerDoc() throws Exception {
        checkSchedulerBadMethod(Observable.class.getSimpleName());
    }

    @Test
    public void checkSingleSchedulerDoc() throws Exception {
        checkSchedulerBadMethod(Single.class.getSimpleName());
    }

    @Test
    public void checkCompletableSchedulerDoc() throws Exception {
        checkSchedulerBadMethod(Completable.class.getSimpleName());
    }

    @Test
    public void checkMaybeSchedulerDoc() throws Exception {
        checkSchedulerBadMethod(Maybe.class.getSimpleName());
    }
}
