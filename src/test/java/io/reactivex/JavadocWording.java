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

import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;

import io.reactivex.BaseTypeParser.RxMethod;

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
        List<RxMethod> list = BaseTypeParser.parse(MaybeNo2Dot0Since.findSource("Maybe"), "Maybe");

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
                            e.append("java.lang.RuntimeException: Maybe doc mentions onNext but no Flowable/Observable in signature\r\n at io.reactivex.")
                            .append("Maybe (Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Maybe doc mentions Subscriber but not using Flowable\r\n at io.reactivex.")
                            .append("Maybe (Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Maybe doc mentions Subscription but not using Flowable\r\n at io.reactivex.")
                            .append("Maybe (Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                                e.append("java.lang.RuntimeException: Maybe doc mentions Observer but not using Observable\r\n at io.reactivex.")
                                .append("Maybe (Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                                e.append("java.lang.RuntimeException: Maybe doc mentions Publisher but not in the signature\r\n at io.reactivex.")
                                .append("Maybe (Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Maybe doc mentions Flowable but not in the signature\r\n at io.reactivex.")
                            .append("Maybe (Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                        int j = m.javadoc.indexOf("#toSingle", jdx);
                        int k = m.javadoc.indexOf("{@code Single", jdx);
                        if (!m.signature.contains("Single") && (j + 3 != idx && k + 7 != idx)) {
                            e.append("java.lang.RuntimeException: Maybe doc mentions Single but not in the signature\r\n at io.reactivex.")
                            .append("Maybe(Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Maybe doc mentions SingleSource but not in the signature\r\n at io.reactivex.")
                            .append("Maybe (Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Maybe doc mentions Observable but not in the signature\r\n at io.reactivex.")
                            .append("Maybe (Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Maybe doc mentions ObservableSource but not in the signature\r\n at io.reactivex.")
                            .append("Maybe (Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                aOrAn(e, m, "Maybe");
                missingClosingDD(e, m, "Maybe");
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
        List<RxMethod> list = BaseTypeParser.parse(MaybeNo2Dot0Since.findSource("Flowable"), "Flowable");

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
                            e.append("java.lang.RuntimeException: Flowable doc mentions onSuccess\r\n at io.reactivex.")
                            .append("Flowable (Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                                && !m.signature.contains("Observable")) {
                            e.append("java.lang.RuntimeException: Flowable doc mentions Observer but not using Flowable\r\n at io.reactivex.")
                            .append("Flowable (Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                                e.append("java.lang.RuntimeException: Flowable doc mentions Disposable but not using Flowable\r\n at io.reactivex.")
                                .append("Flowable (Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Flowable doc mentions Observable but not in the signature\r\n at io.reactivex.")
                            .append("Flowable (Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Flowable doc mentions ObservableSource but not in the signature\r\n at io.reactivex.")
                            .append("Flowable (Flowable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                aOrAn(e, m, "Flowable");
                missingClosingDD(e, m, "Flowable");
                backpressureMentionedWithoutAnnotation(e, m, "Flowable");
            }
        }

        if (e.length() != 0) {
            System.out.println(e);

            fail(e.toString());
        }
    }

    @Test
    public void observableDocRefersToObservableTypes() throws Exception {
        List<RxMethod> list = BaseTypeParser.parse(MaybeNo2Dot0Since.findSource("Observable"), "Observable");

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
                            e.append("java.lang.RuntimeException: Observable doc mentions onSuccess\r\n at io.reactivex.")
                            .append("Observable (Observable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Observable doc mentions Subscription but not using Flowable\r\n at io.reactivex.")
                            .append("Observable (Observable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                                e.append("java.lang.RuntimeException: Observable doc mentions Flowable but not in the signature\r\n at io.reactivex.")
                                .append("Observable (Observable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Observable doc mentions Publisher but not in the signature\r\n at io.reactivex.")
                            .append("Observable (Observable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Observable doc mentions Subscriber but not using Flowable\r\n at io.reactivex.")
                            .append("Observable (Observable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }

                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                aOrAn(e, m, "Observable");
                missingClosingDD(e, m, "Observable");
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
        List<RxMethod> list = BaseTypeParser.parse(MaybeNo2Dot0Since.findSource("Single"), "Single");

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
                            e.append("java.lang.RuntimeException: Single doc mentions onNext but no Flowable/Observable in signature\r\n at io.reactivex.")
                            .append("Single (Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Single doc mentions Subscriber but not using Flowable\r\n at io.reactivex.")
                            .append("Single (Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Single doc mentions Subscription but not using Flowable\r\n at io.reactivex.")
                            .append("Single (Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                                e.append("java.lang.RuntimeException: Single doc mentions Observer but not using Observable\r\n at io.reactivex.")
                                .append("Single (Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                                e.append("java.lang.RuntimeException: Single doc mentions Publisher but not in the signature\r\n at io.reactivex.")
                                .append("Single (Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Single doc mentions Flowable but not in the signature\r\n at io.reactivex.")
                            .append("Single (Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Single doc mentions Maybe but not in the signature\r\n at io.reactivex.")
                            .append("Single (Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Single doc mentions SingleSource but not in the signature\r\n at io.reactivex.")
                            .append("Maybe (Maybe.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Single doc mentions Observable but not in the signature\r\n at io.reactivex.")
                            .append("Single (Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Single doc mentions ObservableSource but not in the signature\r\n at io.reactivex.")
                            .append("Single (Single.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }

                aOrAn(e, m, "Single");
                missingClosingDD(e, m, "Single");
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
        List<RxMethod> list = BaseTypeParser.parse(MaybeNo2Dot0Since.findSource("Completable"), "Completable");

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
                            e.append("java.lang.RuntimeException: Completable doc mentions onNext but no Flowable/Observable in signature\r\n at io.reactivex.")
                            .append("Completable (Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Completable doc mentions Subscriber but not using Flowable\r\n at io.reactivex.")
                            .append("Completable (Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Completable doc mentions Subscription but not using Flowable\r\n at io.reactivex.")
                            .append("Completable (Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                                e.append("java.lang.RuntimeException: Completable doc mentions Observer but not using Observable\r\n at io.reactivex.")
                                .append("Completable (Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                                e.append("java.lang.RuntimeException: Completable doc mentions Publisher but not in the signature\r\n at io.reactivex.")
                                .append("Completable (Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Completable doc mentions Flowable but not in the signature\r\n at io.reactivex.")
                            .append("Completable (Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Completable doc mentions Single but not in the signature\r\n at io.reactivex.")
                            .append("Completable (Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Completable doc mentions SingleSource but not in the signature\r\n at io.reactivex.")
                            .append("Completable (Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Completable doc mentions Observable but not in the signature\r\n at io.reactivex.")
                            .append("Completable (Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
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
                            e.append("java.lang.RuntimeException: Completable doc mentions ObservableSource but not in the signature\r\n at io.reactivex.")
                            .append("Completable (Completable.java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                        }
                        jdx = idx + 6;
                    } else {
                        break;
                    }
                }
                aOrAn(e, m, "Completable");
                missingClosingDD(e, m, "Completable");
                backpressureMentionedWithoutAnnotation(e, m, "Completable");
            }
        }

        if (e.length() != 0) {
            System.out.println(e);

            fail(e.toString());
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
                .append("\r\n at io.reactivex.")
                .append(baseTypeName)
                .append(" (")
                .append(baseTypeName)
                .append(".java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                jdx = idx + 6;
            } else {
                break;
            }
        }

        for (;;) {
            idx = m.javadoc.indexOf(wrongPre + " {@link " + word, jdx);
            if (idx >= 0) {
                e.append("java.lang.RuntimeException: a/an typo ")
                .append(word)
                .append("\r\n at io.reactivex.")
                .append(baseTypeName)
                .append(" (")
                .append(baseTypeName)
                .append(".java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                jdx = idx + 6;
            } else {
                break;
            }
        }

        for (;;) {
            idx = m.javadoc.indexOf(wrongPre + " {@linkplain " + word, jdx);
            if (idx >= 0) {
                e.append("java.lang.RuntimeException: a/an typo ")
                .append(word)
                .append("\r\n at io.reactivex.")
                .append(baseTypeName)
                .append(" (")
                .append(baseTypeName)
                .append(".java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                jdx = idx + 6;
            } else {
                break;
            }
        }

        for (;;) {
            idx = m.javadoc.indexOf(wrongPre + " {@code " + word, jdx);
            if (idx >= 0) {
                e.append("java.lang.RuntimeException: a/an typo ")
                .append(word)
                .append("\r\n at io.reactivex.")
                .append(baseTypeName)
                .append(" (")
                .append(baseTypeName)
                .append(".java:").append(m.javadocLine + lineNumber(m.javadoc, idx) - 1).append(")\r\n\r\n");
                jdx = idx + 6;
            } else {
                break;
            }
        }

    }

    static void missingClosingDD(StringBuilder e, RxMethod m, String baseTypeName) {
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
                .append("\r\n at io.reactivex.")
                .append(baseTypeName)
                .append(" (")
                .append(baseTypeName)
                .append(".java:").append(m.javadocLine + lineNumber(m.javadoc, idx1) - 1).append(")\r\n\r\n");
                break;
            }
        }
    }

    static void backpressureMentionedWithoutAnnotation(StringBuilder e, RxMethod m, String baseTypeName) {
        if (m.backpressureDocLine > 0 && m.backpressureKind == null) {
            e.append("java.lang.RuntimeException: backpressure documented but not annotated ")
            .append("\r\n at io.reactivex.")
            .append(baseTypeName)
            .append(" (")
            .append(baseTypeName)
            .append(".java:").append(m.backpressureDocLine).append(")\r\n\r\n");
        }
    }
}
