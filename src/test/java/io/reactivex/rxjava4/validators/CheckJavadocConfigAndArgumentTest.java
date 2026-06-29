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

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.testsupport.TestHelper;

/**
 * Scan the Javadocs of a source and check if mentions of other classes,
 * interfaces and enums are using at-link and at-code wrapping for style.
 * <p>
 * The check ignores HTML tag content on a line, &#64;see and &#64;throws entries
 * and &lt;code&gt;&lt;/code&gt; lines.
 */
public class CheckJavadocConfigAndArgumentTest extends RxJavaTest {

    @Test
    public void checkFlowable() throws Exception {
        checkSource("Flowable", "io.reactivex.rxjava4.core");
    }

    @Test
    public void checkCompletable() throws Exception {
        checkSource("Completable", "io.reactivex.rxjava4.core");
    }

    @Test
    public void checkSingle() throws Exception {
        checkSource("Single", "io.reactivex.rxjava4.core");
    }

    @Test
    public void checkMaybe() throws Exception {
        checkSource("Maybe", "io.reactivex.rxjava4.core");
    }

    @Test
    public void checkObservable() throws Exception {
        checkSource("Observable", "io.reactivex.rxjava4.core");
    }

    @Test
    public void checkParallelFlowable() throws Exception {
        checkSource("ParallelFlowable", "io.reactivex.rxjava4.parallel");
    }

    @Test
    public void checkCompositeDisposable() throws Exception {
        checkSource("CompositeDisposable", "io.reactivex.rxjava4.disposables");
    }

    @Test
    public void checkConnectableFlowable() throws Exception {
        checkSource("ConnectableFlowable", "io.reactivex.rxjava4.flowables");
    }

    @Test
    public void checkConnectableObservable() throws Exception {
        checkSource("ConnectableObservable", "io.reactivex.rxjava4.observables");
    }

    @Test
    public void checkSchedulers() throws Exception {
        checkSource("Schedulers", "io.reactivex.rxjava4.schedulers");
    }

    static void checkSource(String baseClassName, String packageName) throws Exception {
        File f = TestHelper.findSource(baseClassName, packageName);
        if (f == null) {
            return;
        }

        StringBuilder errors = new StringBuilder(2048);
        int errorCount = 0;

        List<String> lines = Files.readAllLines(f.toPath());

        // i = 1 skip the header Javadoc
        for (int i = 1; i < lines.size(); i++) {

            if (lines.get(i).trim().equals("/**")) {

                for (int j = i + 1; j < lines.size(); j++) {
                    if (lines.get(j).trim().equals("*/")) {

                        List<String> subList = lines.subList(i + 1, j);

                        if (subList.stream().anyMatch(s -> s.contains("@param config"))) {

                            if (!subList.stream().anyMatch(s -> s.contains("@since 4."))) {

                                errorCount++;
                                errors.append("Since 4.0.0 missing:\r\n at ")
                                .append(packageName)
                                .append(".")
                                .append(baseClassName)
                                .append(".method(")
                                .append(baseClassName)
                                .append(".java:")
                                .append(j)
                                .append(")\r\n");
                            }

                            if (subList.stream().anyMatch(s -> s.contains("@throws IllegalArgumentException"))) {

                                for (int m = j + 1; m < lines.size(); m++) {
                                    if (lines.get(m).isEmpty()) {
                                        List<String> subSubList = lines.subList(j + 1, m);

                                        if (!subSubList.stream().anyMatch(s -> s.contains("new IllegalArgumentException"))
                                                && !subSubList.stream().anyMatch(s -> s.contains("verifyPositive"))) {
                                            errorCount++;
                                            errors.append("Unnecessary IllegalArgumentException:\r\n at ")
                                            .append(packageName)
                                            .append(".")
                                            .append(baseClassName)
                                            .append(".method(")
                                            .append(baseClassName)
                                            .append(".java:")
                                            .append(j)
                                            .append(")\r\n");
                                        }

                                        break;
                                    }
                                }
                            }
}
                        i = j;
                        break;
                    }
                }
            }
        }

        if (errorCount != 0) {
            errors.insert(0, "Found " + (errorCount > ERROR_LIMIT ? ERROR_LIMIT + "+" : errorCount + "") + " cases\r\n");
            throw new AssertionError(errors.toString());
        }
    }

    static String removeCurlies(String input) {
        StringBuilder result = new StringBuilder(input.length());

        boolean skip = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '{') {
                skip = true;
            }
            if (!skip) {
                result.append(c);
            }
            if (c == '}') {
                skip = false;
            }
        }

        return result.toString();
    }

    static String stripTags(String input) {
        StringBuilder result = new StringBuilder(input.length());
        result.append(input, input.length() > 1 ? 2 : 1, input.length());

        clearTag(result, "<a ", "</a>");
        clearTag(result, "<b>", "</b>");
        clearTag(result, "<strong>", "</strong>");
        clearTag(result, "<em>", "</em>");
        clearTag(result, "<img ", ">");

        return result.toString();
    }

    static void clearTag(StringBuilder builder, String startTag, String endTag) {
        int k = 0;
        for (;;) {
            int j = builder.indexOf(startTag, k);
            if (j < 0) {
                break;
            }

            int e = builder.indexOf(endTag, j);
            if (e < 0) {
                e = builder.length();
            }

            blankRange(builder, j, e);

            k = e + endTag.length();
        }
    }

    static void blankRange(StringBuilder builder, int start, int end) {
        for (int i = start; i < end; i++) {
            int c = builder.charAt(i);
            if (c != '\r' && c != '\n') {
                builder.setCharAt(i, ' ');
            }
        }
    }

    static final List<String> NAMES = Arrays.asList(
            "Flowable", "Observable", "Maybe", "Single", "Completable", "ParallelFlowable",

            "Publisher", "ObservableSource", "MaybeSource", "SingleSource", "CompletableSource",

            "FlowableSubscriber", "Subscriber", "Observer", "MaybeObserver", "SingleObserver", "CompletableObserver",

            "FlowableOperator", "ObservableOperator", "MaybeOperator", "SingleOperator", "CompletableOperator",

            "FlowableOnSubscribe", "ObservableOnSubscribe", "MaybeOnSubscribe", "SingleOnSubscribe", "CompletableOnSubscribe",

            "FlowableTransformer", "ObservableTransformer", "MaybeTransformer", "SingleTransformer", "CompletableTransformer", "ParallelTransformer",

            "FlowableConverter", "ObservableConverter", "MaybeConverter", "SingleConverter", "CompletableConverter",

            "FlowableEmitter", "ObservableEmitter", "MaybeEmitter", "SingleEmitter", "CompletableEmitter",

            "Iterable", "Stream",

            "Function", "BiFunction", "Function3", "Function4", "Function5", "Function6", "Function7", "Function8", "Function9",

            "Action", "Runnable", "Disposable", "Subscription", "Consumer", "BiConsumer", "Future",

            "Supplier", "Callable", "TimeUnit",

            "BackpressureOverflowStrategy", "ParallelFailureHandling",

            "Exception", "Throwable", "NullPointerException", "IllegalStateException", "IllegalArgumentException", "MissingBackpressureException", "UndeliverableException",
            "OutOfMemoryError", "StackOverflowError", "NoSuchElementException", "ClassCastException", "CompositeException",
            "RuntimeException", "Error", "TimeoutException", "OnErrorNotImplementedException",

            "false", "true", "onNext", "onError", "onComplete", "onSuccess", "onSubscribe", "null",

            "ConnectableFlowable", "ConnectableObservable", "Subject", "FlowableProcessor", "Processor", "Scheduler",

            "Optional", "CompletionStage", "Collector", "Collectors", "Schedulers", "RxJavaPlugins", "CompletableFuture",

            "Object", "Integer", "Long", "Boolean", "LongConsumer", "BooleanSupplier",

            "GroupedFlowable", "GroupedObservable", "UnicastSubject", "UnicastProcessor",

            "Notification", "Comparable", "Comparator", "Collection",

            "SafeSubscriber", "SafeObserver",

            "List", "ArrayList", "HashMap", "HashSet", "CharSequence",

            "TestSubscriber", "TestObserver", "Class",

            "ThreadFactory", "Runnable", "Executor", "ExecutorService", "Executors", "RejectedExecutionException"
    );

    static final Set<String> ALWAYS_CODE = new HashSet<>(Arrays.asList(
            "false", "true", "null", "onSuccess", "onNext", "onError", "onComplete", "onSubscribe"
    ));

    static final int ERROR_LIMIT = 5000;
}
