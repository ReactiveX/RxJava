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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Check if the parameter name in Objects.requireNonNull
 * and ObjectHelper.verifyPositive calls match the parameter
 *  name in the message.
 */
public class ParamValidationNaming {

    @Test
    public void checkCompletable() throws Exception {
        processFile(Completable.class);
    }

    @Test
    public void checkSingle() throws Exception {
        processFile(Single.class);
    }

    @Test
    public void checkMaybe() throws Exception {
        processFile(Maybe.class);
    }

    @Test
    public void checkObservable() throws Exception {
        processFile(Observable.class);
    }

    @Test
    public void checkFlowable() throws Exception {
        processFile(Flowable.class);
    }

    @Test
    public void checkParallelFlowable() throws Exception {
        processFile(ParallelFlowable.class);
    }

    @Test
    public void checkConnectableObservable() throws Exception {
        processFile(ConnectableObservable.class);
    }

    @Test
    public void checkConnectableFlowable() throws Exception {
        processFile(ConnectableFlowable.class);
    }

    @Test
    public void checkSubject() throws Exception {
        processFile(Subject.class);
    }

    @Test
    public void checkFlowableProcessor() throws Exception {
        processFile(FlowableProcessor.class);
    }

    @Test
    public void checkDisposable() throws Exception {
        processFile(Disposable.class);
    }

    @Test
    public void checkScheduler() throws Exception {
        processFile(Scheduler.class);
    }

    @Test
    public void checkSchedulers() throws Exception {
        processFile(Schedulers.class);
    }

    @Test
    public void checkAsyncSubject() throws Exception {
        processFile(AsyncSubject.class);
    }

    @Test
    public void checkBehaviorSubject() throws Exception {
        processFile(BehaviorSubject.class);
    }

    @Test
    public void checkPublishSubject() throws Exception {
        processFile(PublishSubject.class);
    }

    @Test
    public void checkReplaySubject() throws Exception {
        processFile(ReplaySubject.class);
    }

    @Test
    public void checkUnicastSubject() throws Exception {
        processFile(UnicastSubject.class);
    }

    @Test
    public void checkSingleSubject() throws Exception {
        processFile(SingleSubject.class);
    }

    @Test
    public void checkMaybeSubject() throws Exception {
        processFile(MaybeSubject.class);
    }

    @Test
    public void checkCompletableSubject() throws Exception {
        processFile(CompletableSubject.class);
    }

    @Test
    public void checkAsyncProcessor() throws Exception {
        processFile(AsyncProcessor.class);
    }

    @Test
    public void checkBehaviorProcessor() throws Exception {
        processFile(BehaviorProcessor.class);
    }

    @Test
    public void checkPublishProcessor() throws Exception {
        processFile(PublishProcessor.class);
    }

    @Test
    public void checkReplayProcessor() throws Exception {
        processFile(ReplayProcessor.class);
    }

    @Test
    public void checkUnicastProcessor() throws Exception {
        processFile(UnicastProcessor.class);
    }

    @Test
    public void checkMulticastProcessor() throws Exception {
        processFile(MulticastProcessor.class);
    }

    @Test
    public void checkCompositeDisposable() throws Exception {
        processFile(CompositeDisposable.class);
    }

    static void processFile(Class<?> clazz) throws Exception {
        String baseClassName = clazz.getSimpleName();
        File f = TestHelper.findSource(baseClassName, clazz.getPackage().getName());
        if (f == null) {
            return;
        }
        String fullClassName = clazz.getName();

        int errorCount = 0;
        StringBuilder errors = new StringBuilder();

        List<String> lines = Files.readAllLines(f.toPath());

        for (int j = 0; j < lines.size(); j++) {
            String line = lines.get(j).trim();

            for (ValidatorStrings validatorStr : VALIDATOR_STRINGS) {
                int strIdx = line.indexOf(validatorStr.code);
                if (strIdx >= 0) {

                    int comma = line.indexOf(',', strIdx + validatorStr.code.length());

                    String paramName = line.substring(strIdx + validatorStr.code.length(), comma);

                    int quote = line.indexOf('"', comma);

                    String message = line.substring(quote + 1, Math.min(line.length(), quote + 2 + paramName.length()));

                    if (line.contains("\"A Disposable")) {
                        continue;
                    }

                    if (!line.contains("\"The RxJavaPlugins")
                            && !(message.startsWith(paramName)
                            && (message.endsWith(" ") || message.endsWith("\"")))) {
                        errorCount++;
                        errors.append("L")
                        .append(j)
                        .append(" : Wrong validator message parameter name\r\n    ")
                        .append(line)
                        .append("\r\n")
                        .append("    ").append(paramName).append(" != ").append(message)
                        .append("\r\n at ")
                        .append(fullClassName)
                        .append(".method(")
                        .append(f.getName())
                        .append(":")
                        .append(j + 1)
                        .append(")\r\n")
                        ;
                    }

                    int midx = j - 1;
                    // find the method declaration
                    for (; midx >= 0; midx--) {
                        String linek = lines.get(midx).trim();
                        if (linek.startsWith("public") || linek.startsWith("private")
                                || linek.startsWith("protected")
                                || linek.startsWith("static")
                                || linek.startsWith(baseClassName)) {
                            break;
                        }
                    }

                    if (line.contains("\"The RxJavaPlugins")) {
                        continue;
                    }

                    // find JavaDoc of throws
                    boolean found = false;
                    for (int k = midx - 1; k >= 0; k--) {
                        String linek = lines.get(k).trim();
                        if (linek.startsWith("/**")) {
                            break;
                        }
                        if (linek.startsWith("}")) {
                            found = true; // no method JavaDoc present
                            break;
                        }
                        if (linek.startsWith(validatorStr.javadoc)) {
                            // see if a @code paramName is present
                            String paramStr = "{@code " + paramName + "}";
                            for (int m = k; m < lines.size(); m++) {
                                String linem = lines.get(m).trim();
                                if (linem.startsWith("* @see")
                                        || linem.startsWith("* @since")
                                        || linem.startsWith("*/")) {
                                    break;
                                }
                                if (linem.contains(paramStr)) {
                                    found = true;
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    if (!found) {
                        errorCount++;
                        errors.append("L")
                        .append(j)
                        .append(" : missing '")
                        .append(validatorStr.javadoc)
                        .append("' for argument validation: ")
                        .append(paramName)
                        .append("\r\n    ")
                        .append(line)
                        .append("\r\n at ")
                        .append(fullClassName)
                        .append(".method(")
                        .append(f.getName())
                        .append(":")
                        .append(j + 1)
                        .append(")\r\n")
                        ;
                    }
                }
            }

            for (ValidatorStrings validatorStr : EXCEPTION_STRINGS) {
                int strIdx = line.indexOf(validatorStr.code);
                if (strIdx >= 0) {

                    int midx = j - 1;
                    // find the method declaration
                    for (; midx >= 0; midx--) {
                        String linek = lines.get(midx).trim();
                        if (linek.startsWith("public") || linek.startsWith("private")
                                || linek.startsWith("protected")
                                || linek.startsWith("static")
                                || linek.startsWith(baseClassName)) {
                            break;
                        }
                    }

                    // find JavaDoc of throws
                    boolean found = false;
                    for (int k = midx - 1; k >= 0; k--) {
                        String linek = lines.get(k).trim();
                        if (linek.startsWith("/**")) {
                            break;
                        }
                        if (linek.startsWith("}")) {
                            found = true; // no JavaDoc
                            break;
                        }
                        if (linek.startsWith(validatorStr.javadoc)) {
                            found = true;
                        }
                    }

                    if (!found) {
                        errorCount++;
                        errors.append("L")
                        .append(j)
                        .append(" : missing '")
                        .append(validatorStr.javadoc)
                        .append("' for exception\r\n    ")
                        .append(line)
                        .append("\r\n at ")
                        .append(fullClassName)
                        .append(".method(")
                        .append(f.getName())
                        .append(":")
                        .append(j + 1)
                        .append(")\r\n")
                        ;
                    }
                }
            }

            if (line.startsWith("public") || line.startsWith("protected") || line.startsWith("final") || line.startsWith("private")
                    || line.startsWith("static")) {
                for (ValidatorStrings validatorStr : TYPICAL_ARGUMENT_STRINGS) {
                    // find the method declaration ending {
                    for (int i = j; i < lines.size(); i++) {
                        String linei = lines.get(i).trim();

                        // space + code for capturing type declarations
                        String varPattern = " " + validatorStr.code;
                        if (linei.contains(varPattern + ")")
                                || linei.contains(varPattern + ",")
                                || linei.endsWith(varPattern)) {
                            // ignore nullable-annotated arguments
                            if (!linei.matches(".*\\@Nullable\\s.*" + validatorStr.code + ".*")) {
                                boolean found = false;
                                for (int k = i - 1; k >= 0; k--) {
                                    String linek = lines.get(k).trim();
                                    if (linek.startsWith("/**")) {
                                        break;
                                    }
                                    if (linek.startsWith("}")) {
                                        found = true; // no method JavaDoc present
                                        break;
                                    }
                                    if (linek.startsWith(validatorStr.javadoc)) {
                                        // see if a @code paramName is present
                                        String paramStr = "{@code " + validatorStr.code + "}";
                                        for (int m = k; m < lines.size(); m++) {
                                            String linem = lines.get(m).trim();
                                            if (linem.startsWith("* @see")
                                                    || linem.startsWith("* @since")
                                                    || linem.startsWith("*/")) {
                                                break;
                                            }
                                            if (linem.contains(paramStr)) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        break;
                                    }
                                }

                                if (!found) {
                                    errorCount++;
                                    errors.append("L")
                                    .append(j)
                                    .append(" : missing '")
                                    .append(validatorStr.javadoc)
                                    .append("' for typical argument: ")
                                    .append(validatorStr.code)
                                    .append("\r\n    ")
                                    .append(line)
                                    .append("\r\n at ")
                                    .append(fullClassName)
                                    .append(".method(")
                                    .append(f.getName())
                                    .append(":")
                                    .append(j + 1)
                                    .append(")\r\n")
                                    ;
                                }
                            }
                        }

                        if (linei.endsWith("{") || linei.endsWith(";")) {
                            break;
                        }
                    }
                }
            }
        }

        if (errorCount != 0) {
            errors.insert(0, errorCount + " problems\r\n");
            errors.setLength(errors.length() - 2);
            throw new AssertionError(errors.toString());
        }
    }

    static final class ValidatorStrings {
        final String code;
        final String javadoc;
        ValidatorStrings(String code, String javadoc) {
            this.code = code;
            this.javadoc = javadoc;
        }
    }

    static final List<ValidatorStrings> VALIDATOR_STRINGS = Arrays.asList(
            new ValidatorStrings("Objects.requireNonNull(", "* @throws NullPointerException"),
            new ValidatorStrings("ObjectHelper.verifyPositive(", "* @throws IllegalArgumentException")
    );

    static final List<ValidatorStrings> EXCEPTION_STRINGS = Arrays.asList(
            new ValidatorStrings("throw new NullPointerException(", "* @throws NullPointerException"),
            new ValidatorStrings("throw new IllegalArgumentException(", "* @throws IllegalArgumentException"),
            new ValidatorStrings("throw new IndexOutOfBoundsException(", "* @throws IndexOutOfBoundsException")
    );

    static final List<ValidatorStrings> TYPICAL_ARGUMENT_STRINGS = Arrays.asList(
            new ValidatorStrings("source", "* @throws NullPointerException"),
            new ValidatorStrings("source1", "* @throws NullPointerException"),
            new ValidatorStrings("source2", "* @throws NullPointerException"),
            new ValidatorStrings("source3", "* @throws NullPointerException"),
            new ValidatorStrings("source4", "* @throws NullPointerException"),
            new ValidatorStrings("source5", "* @throws NullPointerException"),
            new ValidatorStrings("source6", "* @throws NullPointerException"),
            new ValidatorStrings("source7", "* @throws NullPointerException"),
            new ValidatorStrings("source8", "* @throws NullPointerException"),
            new ValidatorStrings("source9", "* @throws NullPointerException"),
            new ValidatorStrings("sources", "* @throws NullPointerException"),
            new ValidatorStrings("mapper", "* @throws NullPointerException"),
            new ValidatorStrings("combiner", "* @throws NullPointerException"),
            new ValidatorStrings("zipper", "* @throws NullPointerException"),
            new ValidatorStrings("predicate", "* @throws NullPointerException"),
            new ValidatorStrings("item", "* @throws NullPointerException"),
            new ValidatorStrings("item1", "* @throws NullPointerException"),
            new ValidatorStrings("item2", "* @throws NullPointerException"),
            new ValidatorStrings("item3", "* @throws NullPointerException"),
            new ValidatorStrings("item4", "* @throws NullPointerException"),
            new ValidatorStrings("item5", "* @throws NullPointerException"),
            new ValidatorStrings("item6", "* @throws NullPointerException"),
            new ValidatorStrings("item7", "* @throws NullPointerException"),
            new ValidatorStrings("item8", "* @throws NullPointerException"),
            new ValidatorStrings("item9", "* @throws NullPointerException"),
            new ValidatorStrings("item10", "* @throws NullPointerException"),
            new ValidatorStrings("unit", "* @throws NullPointerException"),
            new ValidatorStrings("scheduler", "* @throws NullPointerException"),
            new ValidatorStrings("other", "* @throws NullPointerException"),
            new ValidatorStrings("fallback", "* @throws NullPointerException"),
            new ValidatorStrings("defaultItem", "* @throws NullPointerException"),
            new ValidatorStrings("defaultValue", "* @throws NullPointerException"),
            new ValidatorStrings("stop", "* @throws NullPointerException"),
            new ValidatorStrings("stopPredicate", "* @throws NullPointerException"),
            new ValidatorStrings("handler", "* @throws NullPointerException"),
            new ValidatorStrings("bufferSupplier", "* @throws NullPointerException"),
            new ValidatorStrings("openingIndicator", "* @throws NullPointerException"),
            new ValidatorStrings("closingIndicator", "* @throws NullPointerException"),
            new ValidatorStrings("boundary", "* @throws NullPointerException"),
            new ValidatorStrings("boundaryIndicator", "* @throws NullPointerException"),
            new ValidatorStrings("selector", "* @throws NullPointerException"),
            new ValidatorStrings("resultSelector", "* @throws NullPointerException"),
            new ValidatorStrings("keySelector", "* @throws NullPointerException"),
            new ValidatorStrings("valueSelector", "* @throws NullPointerException"),
            new ValidatorStrings("valueSupplier", "* @throws NullPointerException"),
            new ValidatorStrings("collectionSupplier", "* @throws NullPointerException"),
            new ValidatorStrings("onNext", "* @throws NullPointerException"),
            new ValidatorStrings("onError", "* @throws NullPointerException"),
            new ValidatorStrings("onComplete", "* @throws NullPointerException"),
            new ValidatorStrings("onEvent", "* @throws NullPointerException"),
            new ValidatorStrings("onAfterNext", "* @throws NullPointerException"),
            new ValidatorStrings("onAfterTerminate", "* @throws NullPointerException"),
            new ValidatorStrings("onTerminate", "* @throws NullPointerException"),
            new ValidatorStrings("onSuccess", "* @throws NullPointerException"),
            new ValidatorStrings("onSubscribe", "* @throws NullPointerException"),
            new ValidatorStrings("onNotification", "* @throws NullPointerException"),
            new ValidatorStrings("onCancel", "* @throws NullPointerException"),
            new ValidatorStrings("onDispose", "* @throws NullPointerException"),
            new ValidatorStrings("onRequest", "* @throws NullPointerException"),
            new ValidatorStrings("onNextMapper", "* @throws NullPointerException"),
            new ValidatorStrings("onErrorMapper", "* @throws NullPointerException"),
            new ValidatorStrings("onCompleteSupplier", "* @throws NullPointerException"),
            new ValidatorStrings("clazz", "* @throws NullPointerException"),
            new ValidatorStrings("next", "* @throws NullPointerException"),
            new ValidatorStrings("reducer", "* @throws NullPointerException"),
            new ValidatorStrings("seed", "* @throws NullPointerException"),
            new ValidatorStrings("seedSupplier", "* @throws NullPointerException"),
            new ValidatorStrings("mapSupplier", "* @throws NullPointerException"),
            new ValidatorStrings("collectionFactory", "* @throws NullPointerException"),
            new ValidatorStrings("factory", "* @throws NullPointerException"),
            new ValidatorStrings("stage", "* @throws NullPointerException"),
            new ValidatorStrings("stream", "* @throws NullPointerException"),
            new ValidatorStrings("collector", "* @throws NullPointerException"),
            new ValidatorStrings("subscriptionIndicator", "* @throws NullPointerException"),
            new ValidatorStrings("itemDelayIndicator", "* @throws NullPointerException"),
            new ValidatorStrings("future", "* @throws NullPointerException"),

            new ValidatorStrings("maxConcurrency", "* @throws IllegalArgumentException"),
            new ValidatorStrings("parallelism", "* @throws IllegalArgumentException"),
            new ValidatorStrings("prefetch", "* @throws IllegalArgumentException"),
            new ValidatorStrings("bufferSize", "* @throws IllegalArgumentException"),
            new ValidatorStrings("capacityHint", "* @throws IllegalArgumentException"),
            new ValidatorStrings("capacity", "* @throws IllegalArgumentException"),
            new ValidatorStrings("count", "* @throws IllegalArgumentException"),
            new ValidatorStrings("skip", "* @throws IllegalArgumentException"),
            new ValidatorStrings("times", "* @throws IllegalArgumentException"),
            new ValidatorStrings("n", "* @throws IllegalArgumentException")
    );

}
