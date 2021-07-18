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

import static org.junit.Assert.*;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import io.reactivex.rxjava3.testsupport.TestHelper;
import io.reactivex.rxjava3.validators.BaseTypeParser.RxMethod;

/**
 * Check if the method wording is consistent with the target base type.
 */
public class JavadocWording {

    public static int lineNumber(CharSequence s, int index) {
        int cnt = 1;
        for (int i = 0; i < index; i++) {
            if (s.charAt(i) == '\n') {
                cnt++;
            }
        }
        return cnt;
    }

    @Test
    public void maybeDocRefersToMaybeTypes() throws Exception {
        List<RxMethod> list = BaseTypeParser.parse(TestHelper.findSource("Maybe"), "Maybe");

        assertFalse(list.isEmpty());

        StringBuilder e = new StringBuilder();

        for (RxMethod m : list) {
            int jdx;
            if (m.javadoc != null) {
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("onNext", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")
                                && !m.signature.contains("Flowable")
                                && !m.signature.contains("Observable")
                                && !m.signature.contains("ObservableSource")) {
                            e.append("java.lang.RuntimeException: Maybe doc mentions onNext but no Flowable/Observable in signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Maybe.method(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Subscriber", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")
                                && !m.signature.contains("Flowable")
                                && !m.signature.contains("TestSubscriber")
                        ) {
                            e.append("java.lang.RuntimeException: Maybe doc mentions Subscriber but not using Flowable\r\n at io.reactivex.rxjava3.core.")
                            .append("Maybe.method(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Subscription", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")
                                && !m.signature.contains("Flowable")
                        ) {
                            e.append("java.lang.RuntimeException: Maybe doc mentions Subscription but not using Flowable\r\n at io.reactivex.rxjava3.core.")
                            .append("Maybe.method(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Observer", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("ObservableSource")
                                && !m.signature.contains("Observable")
                                && !m.signature.contains("TestObserver")) {

                            if (idx < 5 || !m.javadoc.substring(idx - 5, idx + 8).equals("MaybeObserver")) {
                                e.append("java.lang.RuntimeException: Maybe doc mentions Observer but not using Observable\r\n at io.reactivex.rxjava3.core.")
                                .append("Maybe.method(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Publisher", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")) {
                            if (idx == 0 || !m.javadoc.substring(idx - 1, idx + 9).equals("(Publisher")) {
                                e.append("java.lang.RuntimeException: Maybe doc mentions Publisher but not in the signature\r\n at io.reactivex.rxjava3.core.")
                                .append("Maybe.method(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Flowable", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Flowable")) {
                            Pattern p = Pattern.compile("@see\\s+#[A-Za-z0-9 _.,()]*Flowable");
                            if (!p.matcher(m.javadoc).find()) {
                                e.append("java.lang.RuntimeException: Maybe doc mentions Flowable but not in the signature\r\n at io.reactivex.rxjava3.core.")
                                .append("Maybe.method(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Single", jdx);
                    if (idx >= 0 && m.javadoc.indexOf("Single#", jdx) != idx) {
                        int j = m.javadoc.indexOf("#toSingle", jdx);
                        int k = m.javadoc.indexOf("{@code Single", jdx);
                        if (!m.signature.contains("Single") && (j + 3 != idx && k + 7 != idx)) {
                            e.append("java.lang.RuntimeException: Maybe doc mentions Single but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Maybe.method(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("SingleSource", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("SingleSource")) {
                            e.append("java.lang.RuntimeException: Maybe doc mentions SingleSource but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Maybe.method(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Observable", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Observable")) {
                            Pattern p = Pattern.compile("@see\\s+#[A-Za-z0-9 _.,()]*Observable");
                            if (!p.matcher(m.javadoc).find()) {
                                e.append("java.lang.RuntimeException: Maybe doc mentions Observable but not in the signature\r\n at io.reactivex.rxjava3.core.")
                                .append("Maybe.method(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("ObservableSource", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("ObservableSource")) {
                            e.append("java.lang.RuntimeException: Maybe doc mentions ObservableSource but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Maybe.method(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }

                checkAtReturnAndSignatureMatch("Maybe", m, e, "Flowable", "Observable", "Maybe", "Single", "Completable", "Disposable", "Iterable", "Stream", "Future", "CompletionStage");

                aOrAn(e, m, "Maybe");
                missingClosingDD(e, m, "Maybe", "io.reactivex.rxjava3.core");
                backpressureMentionedWithoutAnnotation(e, m, "Maybe");
            }
        }

        if (e.length() != 0) {
            System.out.println(e);

            fail(e.toString());
        }
    }

    @Test
    public void flowableDocRefersToFlowableTypes() throws Exception {
        List<RxMethod> list = BaseTypeParser.parse(TestHelper.findSource("Flowable"), "Flowable");

        assertFalse(list.isEmpty());

        StringBuilder e = new StringBuilder();

        for (RxMethod m : list) {
            int jdx;
            if (m.javadoc != null) {
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("onSuccess", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Maybe")
                                && !m.signature.contains("MaybeSource")
                                && !m.signature.contains("Single")
                                && !m.signature.contains("SingleSource")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions onSuccess\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Observer", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("ObservableSource")
                                && !m.signature.contains("Observable")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions Observer but not using Observable\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" SingleObserver", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("SingleSource")
                                && !m.signature.contains("Single")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions SingleObserver but not using Single\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" MaybeObserver", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("MaybeSource")
                                && !m.signature.contains("Maybe")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions MaybeObserver but not using Maybe\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Disposable", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Observable")
                                && !m.signature.contains("ObservableSource")
                                && !m.signature.contains("Single")
                                && !m.signature.contains("SingleSource")
                                && !m.signature.contains("Completable")
                                && !m.signature.contains("CompletableSource")
                                && !m.signature.contains("Maybe")
                                && !m.signature.contains("MaybeSource")
                                && !m.signature.contains("Disposable")
                                && !m.signature.contains("void subscribe")
                        ) {
                            CharSequence subSequence = m.javadoc.subSequence(idx - 6, idx + 11);
                            if (idx < 6 || !subSequence.equals("{@link Disposable")) {
                                e.append("java.lang.RuntimeException: Flowable doc mentions Disposable but not using Flowable\r\n at io.reactivex.rxjava3.core.")
                                .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Observable", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Observable")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions Observable but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("ObservableSource", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("ObservableSource")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions ObservableSource but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }

                checkAtReturnAndSignatureMatch("Flowable", m, e, "Flowable", "Observable", "Maybe", "Single", "Completable", "ConnectableFlowable", "ParallelFlowable", "Disposable", "Iterable", "Stream", "Future", "CompletionStage");

                aOrAn(e, m, "Flowable");
                missingClosingDD(e, m, "Flowable", "io.reactivex.rxjava3.core");
                backpressureMentionedWithoutAnnotation(e, m, "Flowable");
            }
        }

        if (e.length() != 0) {
            System.out.println(e);

            fail(e.toString());
        }
    }

    @Test
    public void parallelFlowableDocRefersToCorrectTypes() throws Exception {
        List<RxMethod> list = BaseTypeParser.parse(TestHelper.findSource("ParallelFlowable", "io.reactivex.rxjava3.parallel"), "ParallelFlowable");

        assertFalse(list.isEmpty());

        StringBuilder e = new StringBuilder();

        for (RxMethod m : list) {
            int jdx;
            if (m.javadoc != null) {
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("onSuccess", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Maybe")
                                && !m.signature.contains("MaybeSource")
                                && !m.signature.contains("Single")
                                && !m.signature.contains("SingleSource")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions onSuccess\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Observer", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("ObservableSource")
                                && !m.signature.contains("Observable")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions Observer but not using Observable\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" SingleObserver", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("SingleSource")
                                && !m.signature.contains("Single")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions SingleObserver but not using Single\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" MaybeObserver", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("MaybeSource")
                                && !m.signature.contains("Maybe")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions MaybeObserver but not using Maybe\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Disposable", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Observable")
                                && !m.signature.contains("ObservableSource")
                                && !m.signature.contains("Single")
                                && !m.signature.contains("SingleSource")
                                && !m.signature.contains("Completable")
                                && !m.signature.contains("CompletableSource")
                                && !m.signature.contains("Maybe")
                                && !m.signature.contains("MaybeSource")
                                && !m.signature.contains("Disposable")
                        ) {
                            CharSequence subSequence = m.javadoc.subSequence(idx - 6, idx + 11);
                            if (idx < 6 || !subSequence.equals("{@link Disposable")) {
                                e.append("java.lang.RuntimeException: Flowable doc mentions Disposable but not using Flowable\r\n at io.reactivex.rxjava3.core.")
                                .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Observable", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Observable")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions Observable but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("ObservableSource", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("ObservableSource")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions ObservableSource but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Flowable.method(Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }

                checkAtReturnAndSignatureMatch("ParallelFlowable", m, e, "Flowable", "Observable", "Maybe", "Single", "Completable", "ConnectableFlowable", "ParallelFlowable", "Disposable", "Iterable", "Stream", "Future", "CompletionStage");

                aOrAn(e, m, "ParallelFlowable");
                missingClosingDD(e, m, "ParallelFlowable", "io.reactivex.rxjava3.parallel");
                backpressureMentionedWithoutAnnotation(e, m, "ParallelFlowable");
            }
        }

        if (e.length() != 0) {
            System.out.println(e);

            fail(e.toString());
        }
    }

    @Test
    public void observableDocRefersToObservableTypes() throws Exception {
        List<RxMethod> list = BaseTypeParser.parse(TestHelper.findSource("Observable"), "Observable");

        assertFalse(list.isEmpty());

        StringBuilder e = new StringBuilder();

        for (RxMethod m : list) {
            int jdx;
            if (m.javadoc != null) {
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("onSuccess", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Maybe")
                                && !m.signature.contains("MaybeSource")
                                && !m.signature.contains("Single")
                                && !m.signature.contains("SingleSource")) {
                            e.append("java.lang.RuntimeException: Observable doc mentions onSuccess\r\n at io.reactivex.rxjava3.core.")
                            .append("Observable.method(Observable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Subscription", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Flowable")
                                && !m.signature.contains("Publisher")
                        ) {
                            e.append("java.lang.RuntimeException: Observable doc mentions Subscription but not using Flowable\r\n at io.reactivex.rxjava3.core.")
                            .append("Observable.method(Observable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Flowable", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Flowable")) {
                            if (idx < 6 || !m.javadoc.substring(idx - 6, idx + 8).equals("@link Flowable")) {
                                e.append("java.lang.RuntimeException: Observable doc mentions Flowable but not in the signature\r\n at io.reactivex.rxjava3.core.")
                                .append("Observable.method(Observable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Publisher", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")) {
                            e.append("java.lang.RuntimeException: Observable doc mentions Publisher but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Observable.method(Observable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Subscriber", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")
                                && !m.signature.contains("Flowable")) {
                            e.append("java.lang.RuntimeException: Observable doc mentions Subscriber but not using Flowable\r\n at io.reactivex.rxjava3.core.")
                            .append("Observable.method(Observable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                checkAtReturnAndSignatureMatch("Observable", m, e, "Flowable", "Observable", "Maybe", "Single", "Completable", "ConnectableObservable", "Disposable", "Iterable", "Stream", "Future", "CompletionStage");

                aOrAn(e, m, "Observable");
                missingClosingDD(e, m, "Observable", "io.reactivex.rxjava3.core");
                backpressureMentionedWithoutAnnotation(e, m, "Observable");
            }
        }

        if (e.length() != 0) {
            System.out.println(e);

            fail(e.toString());
        }
    }

    @Test
    public void singleDocRefersToSingleTypes() throws Exception {
        List<RxMethod> list = BaseTypeParser.parse(TestHelper.findSource("Single"), "Single");

        assertFalse(list.isEmpty());

        StringBuilder e = new StringBuilder();

        for (RxMethod m : list) {
            int jdx;
            if (m.javadoc != null) {
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("onNext", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")
                                && !m.signature.contains("Flowable")
                                && !m.signature.contains("Observable")
                                && !m.signature.contains("ObservableSource")) {
                            e.append("java.lang.RuntimeException: Single doc mentions onNext but no Flowable/Observable in signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Single.method(Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Subscriber", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")
                                && !m.signature.contains("Flowable")
                                && !m.signature.contains("TestSubscriber")) {
                            e.append("java.lang.RuntimeException: Single doc mentions Subscriber but not using Flowable\r\n at io.reactivex.rxjava3.core.")
                            .append("Single.method(Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Subscription", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Flowable")
                                && !m.signature.contains("Publisher")
                        ) {
                            e.append("java.lang.RuntimeException: Single doc mentions Subscription but not using Flowable\r\n at io.reactivex.rxjava3.core.")
                            .append("Single.method(Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Observer", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("ObservableSource")
                                && !m.signature.contains("Observable")
                                && !m.signature.contains("TestObserver")) {

                            if (idx < 6 || !m.javadoc.substring(idx - 6, idx + 8).equals("SingleObserver")) {
                                e.append("java.lang.RuntimeException: Single doc mentions Observer but not using Observable\r\n at io.reactivex.rxjava3.core.")
                                .append("Single.method(Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Publisher", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")) {
                            if (idx == 0 || !m.javadoc.substring(idx - 1, idx + 9).equals("(Publisher")) {
                                e.append("java.lang.RuntimeException: Single doc mentions Publisher but not in the signature\r\n at io.reactivex.rxjava3.core.")
                                .append("Single.method(Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Flowable", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Flowable")) {
                            e.append("java.lang.RuntimeException: Single doc mentions Flowable but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Single.method(Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Maybe", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Maybe")) {
                            e.append("java.lang.RuntimeException: Single doc mentions Maybe but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Single.method(Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" MaybeSource", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("MaybeSource")) {
                            e.append("java.lang.RuntimeException: Single doc mentions SingleSource but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Maybe.method(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Observable", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Observable")) {
                            e.append("java.lang.RuntimeException: Single doc mentions Observable but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Single.method(Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" ObservableSource", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("ObservableSource")) {
                            e.append("java.lang.RuntimeException: Single doc mentions ObservableSource but not in the signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Single.method(Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }

                checkAtReturnAndSignatureMatch("Single", m, e, "Flowable", "Observable", "Maybe", "Single", "Completable", "Disposable", "Iterable", "Stream", "Future", "CompletionStage");

                aOrAn(e, m, "Single");
                missingClosingDD(e, m, "Single", "io.reactivex.rxjava3.core");
                backpressureMentionedWithoutAnnotation(e, m, "Single");
            }
        }

        if (e.length() != 0) {
            System.out.println(e);

            fail(e.toString());
        }
    }

    @Test
    public void completableDocRefersToCompletableTypes() throws Exception {
        List<RxMethod> list = BaseTypeParser.parse(TestHelper.findSource("Completable"), "Completable");

        assertFalse(list.isEmpty());

        StringBuilder e = new StringBuilder();

        for (RxMethod m : list) {
            int jdx;
            if (m.javadoc != null) {
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("onNext", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")
                                && !m.signature.contains("Flowable")
                                && !m.signature.contains("Observable")
                                && !m.signature.contains("ObservableSource")) {
                            e.append("java.lang.RuntimeException: Completable doc mentions onNext but no Flowable/Observable in signature\r\n at io.reactivex.rxjava3.core.")
                            .append("Completable.method(Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Subscriber", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")
                                && !m.signature.contains("Flowable")
                                && !m.signature.contains("TestSubscriber")) {
                            e.append("java.lang.RuntimeException: Completable doc mentions Subscriber but not using Flowable\r\n at io.reactivex.rxjava3.core.")
                            .append("Completable.method(Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Subscription", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Flowable")
                                && !m.signature.contains("Publisher")
                        ) {
                            e.append("java.lang.RuntimeException: Completable doc mentions Subscription but not using Flowable\r\n at io.reactivex.rxjava3.core.")
                            .append("Completable.method(Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Observer", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("ObservableSource")
                                && !m.signature.contains("Observable")
                                && !m.signature.contains("TestObserver")) {

                            if (idx < 11 || !m.javadoc.substring(idx - 11, idx + 8).equals("CompletableObserver")) {
                                e.append("java.lang.RuntimeException: Completable doc mentions Observer but not using Observable\r\n at io.reactivex.rxjava3.core.")
                                .append("Completable.method(Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Publisher", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Publisher")) {
                            if (idx == 0 || !m.javadoc.substring(idx - 1, idx + 9).equals("(Publisher")) {
                                e.append("java.lang.RuntimeException: Completable doc mentions Publisher but not in the signature\r\n at io.reactivex.rxjava3.core.")
                                .append("Completable.method(Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Flowable", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Flowable")) {
                            Pattern p = Pattern.compile("@see\\s+#[A-Za-z0-9 _.,()]*Flowable");
                            if (!p.matcher(m.javadoc).find()) {
                                e.append("java.lang.RuntimeException: Completable doc mentions Flowable but not in the signature\r\n at io.reactivex.rxjava3.core.")
                                .append("Completable.method(Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("Single", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Single")) {
                            Pattern p = Pattern.compile("@see\\s+#[A-Za-z0-9 _.,()]*Single");
                            if (!p.matcher(m.javadoc).find()) {
                                e.append("java.lang.RuntimeException: Completable doc mentions Single but not in the signature\r\n at io.reactivex.rxjava3.core.")
                                .append("Completable.method(Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("SingleSource", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("SingleSource")) {
                            Pattern p = Pattern.compile("@see\\s+#[A-Za-z0-9 _.,()]*SingleSource");
                            if (!p.matcher(m.javadoc).find()) {
                                e.append("java.lang.RuntimeException: Completable doc mentions SingleSource but not in the signature\r\n at io.reactivex.rxjava3.core.")
                                .append("Completable.method(Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf(" Observable", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("Observable")) {
                            Pattern p = Pattern.compile("@see\\s+#[A-Za-z0-9 _.,()]*Observable");
                            if (!p.matcher(m.javadoc).find()) {
                                e.append("java.lang.RuntimeException: Completable doc mentions Observable but not in the signature\r\n at io.reactivex.rxjava3.core.")
                                .append("Completable.method(Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                jdx = 0;
                for (;;) {
                    int idx = m.javadoc.indexOf("ObservableSource", jdx);
                    if (idx >= 0) {
                        if (!m.signature.contains("ObservableSource")) {
                            Pattern p = Pattern.compile("@see\\s+#[A-Za-z0-9 _.,()]*ObservableSource");
                            if (!p.matcher(m.javadoc).find()) {
                                e.append("java.lang.RuntimeException: Completable doc mentions ObservableSource but not in the signature\r\n at io.reactivex.rxjava3.core.")
                                .append("Completable.method(Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                            }
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }

                checkAtReturnAndSignatureMatch("Completable", m, e, "Flowable", "Observable", "Maybe", "Single", "Completable", "Disposable", "Iterable", "Stream", "Future", "CompletionStage");

                aOrAn(e, m, "Completable");
                missingClosingDD(e, m, "Completable", "io.reactivex.rxjava3.core");
                backpressureMentionedWithoutAnnotation(e, m, "Completable");
            }
        }

        if (e.length() != 0) {
            System.out.println(e);

            fail(e.toString());
        }
    }

    static void checkAtReturnAndSignatureMatch(String className, RxMethod m, StringBuilder e, String... types) {
        for (String t : types) {
            String regex;
            if (t.contains("Completable")) {
                regex = "(?s).*?\\s" + t + "\\s+\\w+\\(.*";
            } else {
                regex = "(?s).*?\\s" + t + "\\<.*?\\>\\s+\\w+\\(.*";
            }
            if (m.signature.matches(regex)) {
                for (String at : AT_RETURN_WORDS) {
                    for (String u : types) {
                        if (!t.equals(u)) {
                            int idx = m.javadoc.indexOf(at + "{@code " + u);
                            if (idx >= 0) {
                                e.append("Returns ").append(t)
                                .append(" but docs return ")
                                .append(u)
                                .append("\r\n at io.reactivex.rxjava3.core.")
                                .append(className)
                                .append(".method(")
                                .append(className)
                                .append(".java:")
                                .append(m.javadocLine + lineNumber(m.javadoc, idx) - 1)
                                .append(")\r\n\r\n");
                            }
                        }
                    }
                }
            }
        }
    }

    static void aOrAn(StringBuilder e, RxMethod m, String baseTypeName) {
        aOrAn(e, m, " an", "Single", baseTypeName);
        aOrAn(e, m, " an", "Maybe", baseTypeName);
        aOrAn(e, m, " a", "Observer", baseTypeName);
        aOrAn(e, m, " a", "Observable", baseTypeName);
        aOrAn(e, m, " an", "Publisher", baseTypeName);
        aOrAn(e, m, " an", "Subscriber", baseTypeName);
        aOrAn(e, m, " an", "Flowable", baseTypeName);

        aOrAn(e, m, " a", "Observable", baseTypeName);

    }

    static void aOrAn(StringBuilder e, RxMethod m, String wrongPre, String word, String baseTypeName) {
        int jdx = 0;
        int idx;
        for (;;) {
            idx = m.javadoc.indexOf(wrongPre + " " + word, jdx);
            if (idx >= 0) {
                e.append("java.lang.RuntimeException: a/an typo ")
                .append(word)
                .append("\r\n at io.reactivex.rxjava3.core.")
                .append(baseTypeName)
                .append(".method(")
                .append(baseTypeName)
                .append(".java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                jdx = idx + 6;
            } else {
                break;
            }
        }

        jdx = 0;
        for (;;) {
            idx = m.javadoc.indexOf(wrongPre + " {@link " + word, jdx);
            if (idx >= 0) {
                e.append("java.lang.RuntimeException: a/an typo ")
                .append(word)
                .append("\r\n at io.reactivex.rxjava3.core.")
                .append(baseTypeName)
                .append(".method(")
                .append(baseTypeName)
                .append(".java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                jdx = idx + 6;
            } else {
                break;
            }
        }

        jdx = 0;
        for (;;) {
            idx = m.javadoc.indexOf(wrongPre + " {@linkplain " + word, jdx);
            if (idx >= 0) {
                e.append("java.lang.RuntimeException: a/an typo ")
                .append(word)
                .append("\r\n at io.reactivex.rxjava3.core.")
                .append(baseTypeName)
                .append(".method(")
                .append(baseTypeName)
                .append(".java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                jdx = idx + 6;
            } else {
                break;
            }
        }

        jdx = 0;
        for (;;) {
            idx = m.javadoc.indexOf(wrongPre + " {@code " + word, jdx);
            if (idx >= 0) {
                e.append("java.lang.RuntimeException: a/an typo ")
                .append(word)
                .append("\r\n at io.reactivex.rxjava3.core.")
                .append(baseTypeName)
                .append(".method(")
                .append(baseTypeName)
                .append(".java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                jdx = idx + 6;
            } else {
                break;
            }
        }

        // remove linebreaks and multi-spaces
        String javadoc2 = m.javadoc.replace("\n", " ").replace("\r", " ")
                .replace(" * ", " ")
                .replaceAll("\\s+", " ");

        // strip {@xxx } tags
        int kk = 0;
        for (;;) {
            int jj = javadoc2.indexOf("{@", kk);
            if (jj < 0) {
                break;
            }
            int nn = javadoc2.indexOf(" ", jj + 2);
            int mm = javadoc2.indexOf("}", jj + 2);

            javadoc2 = javadoc2.substring(0, jj) + javadoc2.substring(nn + 1, mm) + javadoc2.substring(mm + 1);

            kk = mm + 1;
        }

        jdx = 0;
        for (;;) {
            idx = javadoc2.indexOf(wrongPre + " " + word, jdx);
            if (idx >= 0) {
                e.append("java.lang.RuntimeException: a/an typo ")
                .append(word)
                .append("\r\n at io.reactivex.rxjava3.core.")
                .append(baseTypeName)
                .append(".method(")
                .append(baseTypeName)
                .append(".java:").append(m.javadocLine).append(")\r\n\r\n");
                jdx = idx + wrongPre.length() + 1 + word.length();
            } else {
                break;
            }
        }
    }

    static void missingClosingDD(StringBuilder e, RxMethod m, String baseTypeName, String packageName) {
        int jdx = 0;
        for (;;) {
            int idx1 = m.javadoc.indexOf("<dd>", jdx);
            int idx2 = m.javadoc.indexOf("</dd>", jdx);

            if (idx1 < 0 && idx2 < 0) {
                break;
            }

            int idx3 = m.javadoc.indexOf("<dd>", idx1 + 4);

            if (idx1 > 0 && idx2 > 0 && (idx3 < 0 || (idx2 < idx3 && idx3 > 0))) {
                jdx = idx2 + 5;
            } else {
                e.append("java.lang.RuntimeException: unbalanced <dd></dd> ")
                .append("\r\n at ")
                .append(packageName)
                .append(".")
                .append(baseTypeName)
                .append(".method(")
                .append(baseTypeName)
                .append(".java:").append(m.javadocLine + lineNumber(m.javadoc, idx1) - 1).append(")\r\n\r\n");
                break;
            }
        }
    }

    static void backpressureMentionedWithoutAnnotation(StringBuilder e, RxMethod m, String baseTypeName) {
        if (m.backpressureDocLine > 0 && m.backpressureKind == null) {
            e.append("java.lang.RuntimeException: backpressure documented but not annotated ")
            .append("\r\n at io.reactivex.rxjava3.core.")
            .append(baseTypeName)
            .append(".method(")
            .append(baseTypeName)
            .append(".java:").append(m.backpressureDocLine).append(")\r\n\r\n");
        }
    }

    static final String[] AT_RETURN_WORDS = { "@return a ", "@return an ", "@return the new ", "@return a new " };
}
