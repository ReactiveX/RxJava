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

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Scan the Javadocs of a source and check if mentions of other classes,
 * interfaces and enums are using at-link and at-code wrapping for style.
 * <p>
 * The check ignores html tag content on a line, &#64;see and &#64;throws entries
 * and &lt;code&gt;&lt;/code&gt; lines.
 */
public class JavadocCodesAndLinks {

    @Test
    public void checkFlowable() throws Exception {
        checkSource("Flowable", "io.reactivex.rxjava3.core");
    }

    @Test
    public void checkCompletable() throws Exception {
        checkSource("Completable", "io.reactivex.rxjava3.core");
    }

    @Test
    public void checkSingle() throws Exception {
        checkSource("Single", "io.reactivex.rxjava3.core");
    }

    @Test
    public void checkMaybe() throws Exception {
        checkSource("Maybe", "io.reactivex.rxjava3.core");
    }

    @Test
    public void checkObservable() throws Exception {
        checkSource("Observable", "io.reactivex.rxjava3.core");
    }

    @Test
    public void checkParallelFlowable() throws Exception {
        checkSource("ParallelFlowable", "io.reactivex.rxjava3.parallel");
    }

    @Test
    public void checkCompositeDisposable() throws Exception {
        checkSource("CompositeDisposable", "io.reactivex.rxjava3.disposables");
    }

    static void checkSource(String baseClassName, String packageName) throws Exception {
        File f = TestHelper.findSource(baseClassName, packageName);
        if (f == null) {
            return;
        }

        StringBuilder errors = new StringBuilder(2048);
        int errorCount = 0;

        List<String> lines = Files.readAllLines(f.toPath());

        List<String> docs = new ArrayList<>();

        // i = 1 skip the header javadoc
        for (int i = 1; i < lines.size(); i++) {

            if (lines.get(i).trim().equals("/**")) {
                docs.clear();

                boolean skipCode = false;
                for (int j = i + 1; j < lines.size(); j++) {
                    String line = lines.get(j).trim();
                    if (line.contains("<code>")) {
                        skipCode = true;
                    }
                    if (line.equals("*/")) {
                        break;
                    }
                    if (!skipCode) {
                        // strip <a> </a>
                        line = stripTags(line);
                        if (line.startsWith("@see")) {
                            docs.add("");
                        }
                        else if (line.startsWith("@throws") || line.startsWith("@param")) {
                            int space = line.indexOf(' ');
                            if (space < 0) {
                                docs.add("");
                            } else {
                                space = line.indexOf(" ", space + 1);
                                if (space < 0) {
                                    docs.add("");
                                } else {
                                    docs.add(line.substring(space + 1));
                                }
                            }
                        } else {
                            docs.add(line);
                        }
                    } else {
                        docs.add("");
                    }
                    if (line.contains("</code>")) {
                        skipCode = false;
                    }
                }

                for (String name : NAMES) {
                    boolean isHostType = name.equals(baseClassName);
                    boolean isAlwaysCode = ALWAYS_CODE.contains(name);
                    String asLink = "{@link " + name + "}";
                    String asCode = "{@code " + name + "}";
                    boolean seenBefore = false;

                    for (int j = 0; j < docs.size(); j++) {
                        String line = docs.get(j);

                        int idxLink = line.indexOf(asLink);
                        if (idxLink >= 0 && !isHostType && !isAlwaysCode) {
                            int k = idxLink + asLink.length();
                            for (;;) {
                                int jdxLink = line.indexOf(asLink, k);
                                if (jdxLink < 0) {
                                    break;
                                }
                                if (jdxLink >= 0) {
                                    errorCount++;
                                    errors.append("The subsequent mention should be code: ")
                                    .append("{@code ").append(name)
                                    .append("}\r\n at ")
                                    .append(packageName)
                                    .append(".")
                                    .append(baseClassName)
                                    .append(".method(")
                                    .append(baseClassName)
                                    .append(".java:")
                                    .append(i + 2 + j)
                                    .append(")\r\n");
                                }
                                k = jdxLink + asLink.length();
                            }
                        }
                        if (seenBefore) {
                            if (idxLink >= 0 && !isHostType && !isAlwaysCode) {
                                errorCount++;
                                errors.append("The subsequent mention should be code: ")
                                .append("{@code ").append(name)
                                .append("}\r\n at ")
                                .append(packageName)
                                .append(".")
                                .append(baseClassName)
                                .append(".method(")
                                .append(baseClassName)
                                .append(".java:")
                                .append(i + 2 + j)
                                .append(")\r\n");
                            }
                        } else {
                            int idxCode = line.indexOf(asCode);

                            if (isHostType) {
                                if (idxLink >= 0) {
                                    errorCount++;
                                    errors.append("The host type mention should be code: ")
                                    .append("{@code ").append(name)
                                    .append("}\r\n at ")
                                    .append(packageName)
                                    .append(".")
                                    .append(baseClassName)
                                    .append(".method(")
                                    .append(baseClassName)
                                    .append(".java:")
                                    .append(i + 2 + j)
                                    .append(")\r\n");
                                }
                            } else {
                                if ((idxLink < 0 && idxCode >= 0 && !isAlwaysCode)
                                        || (idxLink >= 0 && idxCode >= 0 && idxCode < idxLink)) {
                                    errorCount++;
                                    if (isAlwaysCode) {
                                        errors.append("The first mention should be code: ")
                                        .append("{@code ")
                                        ;
                                    } else {
                                        errors.append("The first mention should be link: ")
                                        .append("{@link ")
                                        ;
                                    }
                                    errors
                                    .append(name)
                                    .append("}\r\n at ")
                                    .append(packageName)
                                    .append(".")
                                    .append(baseClassName)
                                    .append(".method(")
                                    .append(baseClassName)
                                    .append(".java:")
                                    .append(i + 2 + j)
                                    .append(")\r\n");
                                }
                            }

                            seenBefore = idxLink >= 0 || idxCode >= 0;
                        }

                        // strip out all existing {@code } and {@link } instances
                        String noCurly = removeCurlies(line);

                        int k = 0;

                        for (;;) {
                            int idx = noCurly.indexOf(name, k);
                            if (idx < 0) {
                                break;
                            }
                            k = idx + name.length();

                            if (isHostType) {
                                errorCount++;
                                errors.append("The host type mention should be code: ")
                                .append("{@code ").append(name)
                                .append("}\r\n at ")
                                .append(packageName)
                                .append(".")
                                .append(baseClassName)
                                .append(".method(")
                                .append(baseClassName)
                                .append(".java:")
                                .append(i + 2 + j)
                                .append(")\r\n");
                            }
                            else if (!seenBefore) {
                                errorCount++;
                                if (isAlwaysCode) {
                                    errors.append("The first mention should be code: ")
                                    .append("{@code ");
                                } else {
                                    errors.append("The first mention should be link: ")
                                    .append("{@link ");
                                }
                                errors
                                .append(name)
                                .append("}\r\n at ")
                                .append(packageName)
                                .append(".")
                                .append(baseClassName)
                                .append(".method(")
                                .append(baseClassName)
                                .append(".java:")
                                .append(i + 2 + j)
                                .append(")\r\n");
                            } else {
                                errorCount++;
                                errors.append("The subsequent mention should be code: ")
                                .append("{@code ").append(name)
                                .append("}\r\n at ")
                                .append(packageName)
                                .append(".")
                                .append(baseClassName)
                                .append(".method(")
                                .append(baseClassName)
                                .append(".java:")
                                .append(i + 2 + j)
                                .append(")\r\n");
                            }

                            seenBefore = true;
                        }
                    }
                }

                i += docs.size();

                if (errorCount >= ERROR_LIMIT) {
                    break;
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

            "TestSubscriber", "TestObserver", "Class"
    );

    static final Set<String> ALWAYS_CODE = new HashSet<>(Arrays.asList(
            "false", "true", "null", "onSuccess", "onNext", "onError", "onComplete", "onSubscribe"
    ));

    static final int ERROR_LIMIT = 5000;
}
