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
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Parse the given files and check if all public final and public static methods have
 * &#64;NonNull or &#64;Nullable annotations specified on their return type and object-type parameters
 * as well as &#64;SafeVarargs for varargs.
 */
public class SourceAnnotationCheck {

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
    public void checkRxJavaPlugins() throws Exception {
        processFile(RxJavaPlugins.class);
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

            if (line.contains("class")) {
                continue;
            }
            if (line.startsWith("public static")
                    || line.startsWith("public final")
                    || line.startsWith("protected final")
                    || line.startsWith("protected abstract")
                    || line.startsWith("public abstract")) {
                int methodArgStart = line.indexOf("(");

                int isBoolean = line.indexOf(" boolean ");
                int isInt = line.indexOf(" int ");
                int isLong = line.indexOf(" long ");
                int isVoid = line.indexOf(" void ");
                int isElementType = line.indexOf(" R ");

                boolean hasSafeVarargsAnnotation = false;

                if (!((isBoolean > 0 && isBoolean < methodArgStart)
                        || (isInt > 0 && isInt < methodArgStart)
                        || (isLong > 0 && isLong < methodArgStart)
                        || (isVoid > 0 && isVoid < methodArgStart)
                        || (isElementType > 0 && isElementType < methodArgStart)
                    )) {

                    boolean annotationFound = false;
                    for (int k = j - 1; k >= 0; k--) {

                        String prevLine = lines.get(k).trim();

                        if (prevLine.startsWith("}") || prevLine.startsWith("*/")) {
                            break;
                        }
                        if (prevLine.startsWith("@NonNull") || prevLine.startsWith("@Nullable")) {
                            annotationFound = true;
                        }
                        if (prevLine.startsWith("@SafeVarargs")) {
                            hasSafeVarargsAnnotation = true;
                        }
                    }

                    if (!annotationFound) {
                        errorCount++;
                        errors.append("L")
                        .append(j)
                        .append(" : Missing return type nullability annotation | ")
                        .append(line)
                        .append("\r\n")
                        .append(" at ")
                        .append(fullClassName)
                        .append(".method(")
                        .append(f.getName())
                        .append(":")
                        .append(j + 1)
                        .append(")\r\n")
                        ;
                    }
                }

                // Extract arguments
                StringBuilder arguments = new StringBuilder();
                int methodArgEnd = line.indexOf(")", methodArgStart);
                if (methodArgEnd > 0) {
                    arguments.append(line.substring(methodArgStart + 1, methodArgEnd));
                } else {
                    arguments.append(line.substring(methodArgStart + 1));
                    for (int k = j + 1; k < lines.size(); k++) {
                        String ln = lines.get(k).trim();
                        int idx = ln.indexOf(")");
                        if (idx > 0) {
                            arguments.append(ln.substring(0, idx));
                            break;
                        }
                        arguments.append(ln).append(" ");
                    }
                }

                // Strip generics arguments
                StringBuilder strippedArguments = new StringBuilder();
                int skippingDepth = 0;
                for (int k = 0; k < arguments.length(); k++) {
                    char c = arguments.charAt(k);
                    if (c == '<') {
                        skippingDepth++;
                    }
                    else if (c == '>') {
                        skippingDepth--;
                    }
                    else if (skippingDepth == 0) {
                        strippedArguments.append(c);
                    }
                }

                String strippedArgumentsStr = strippedArguments.toString();
                String[] args = strippedArgumentsStr.split("\\s*,\\s*");

                for (int k = 0; k < args.length; k++) {
                    String typeDef = args[k];

                    for (String typeName : CLASS_NAMES) {
                        String typeNameSpaced = typeName + " ";

                        if (typeDef.contains(typeNameSpaced)
                                && !typeDef.contains("@NonNull")
                                && !typeDef.contains("@Nullable")) {

                            if (!line.contains("@Nullable " + typeName)
                                    && !line.contains("@NonNull " + typeName)) {
                                errorCount++;
                                errors.append("L")
                                .append(j)
                                .append(" - argument ").append(k + 1)
                                .append(" : Missing argument type nullability annotation\r\n    ")
                                .append(typeDef).append("\r\n    ")
                                .append(strippedArgumentsStr)
                                .append("\r\n")
                                .append(" at ")
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

                    if (typeDef.contains("final ")) {
                        errorCount++;
                        errors.append("L")
                        .append(j)
                        .append(" - argument ").append(k + 1)
                        .append(" : unnecessary final on argument\r\n    ")
                        .append(typeDef).append("\r\n    ")
                        .append(strippedArgumentsStr)
                        .append("\r\n")
                        .append(" at ")
                        .append(fullClassName)
                        .append(".method(")
                        .append(f.getName())
                        .append(":")
                        .append(j + 1)
                        .append(")\r\n")
                        ;
                    }
                    if (typeDef.contains("@NonNull int")
                            || typeDef.contains("@NonNull long")
                            || typeDef.contains("@Nullable int")
                            || typeDef.contains("@Nullable long")
                        ) {
                        errorCount++;
                        errors.append("L")
                        .append(j)
                        .append(" - argument ").append(k + 1)
                        .append(" : unnecessary nullability annotation\r\n    ")
                        .append(typeDef).append("\r\n    ")
                        .append(strippedArgumentsStr)
                        .append("\r\n")
                        .append(" at ")
                        .append(fullClassName)
                        .append(".method(")
                        .append(f.getName())
                        .append(":")
                        .append(j + 1)
                        .append(")\r\n")
                        ;
                    }

                }

                if (strippedArgumentsStr.contains("...") && !hasSafeVarargsAnnotation) {
                    errorCount++;
                    errors.append("L")
                    .append(j)
                    .append(" : Missing @SafeVarargs annotation\r\n    ")
                    .append(strippedArgumentsStr)
                    .append("\r\n")
                    .append(" at ")
                    .append(fullClassName)
                    .append(".method(")
                    .append(f.getName())
                    .append(":")
                    .append(j + 1)
                    .append(")\r\n")
                    ;
                }
            }

            for (String typeName : TYPES_REQUIRING_NONNULL_TYPEARG) {
                String pattern = typeName + "<?";
                String patternRegex = ".*" + typeName + "\\<\\? (extends|super) " + COMMON_TYPE_ARG_NAMES + "\\>.*";
                if (line.contains(pattern) && !line.matches(patternRegex)) {

                    errorCount++;
                    errors.append("L")
                    .append(j)
                    .append(" : Missing @NonNull type argument annotation on ")
                    .append(typeName)
                    .append("\r\n")
                    .append(" at ")
                    .append(fullClassName)
                    .append(".method(")
                    .append(f.getName())
                    .append(":")
                    .append(j + 1)
                    .append(")\r\n")
                    ;
                }
            }
            for (String typeName : TYPES_FORBIDDEN_NONNULL_TYPEARG) {
                String patternRegex = ".*" + typeName + "\\<@NonNull (\\? (extends|super) )?" + COMMON_TYPE_ARG_NAMES + "\\>.*";

                if (line.matches(patternRegex)) {
                    errorCount++;
                    errors.append("L")
                    .append(j)
                    .append(" : @NonNull type argument should be on the arg declaration ")
                    .append(typeName)
                    .append("\r\n")
                    .append(" at ")
                    .append(fullClassName)
                    .append(".method(")
                    .append(f.getName())
                    .append(":")
                    .append(j + 1)
                    .append(")\r\n")
                    ;
                }
            }

            for (String typeName : TYPES_REQUIRING_NONNULL_TYPEARG_ON_FUNC) {
                if (line.matches(".*Function[\\d]?\\<.*, (\\? (extends|super) )?" + typeName + ".*")) {
                    errorCount++;
                    errors.append("L")
                    .append(j)
                    .append(" : Missing @NonNull type argument annotation on Function argument ")
                    .append(typeName)
                    .append("\r\n")
                    .append(" at ")
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

        if (errorCount != 0) {
            errors.insert(0, errorCount + " problems\r\n");
            errors.setLength(errors.length() - 2);
            throw new AssertionError(errors.toString());
        }
    }

    static final List<String> CLASS_NAMES = Arrays.asList(
            "TimeUnit", "Scheduler", "Emitter",

            "Completable", "CompletableSource", "CompletableObserver", "CompletableOnSubscribe",
            "CompletableTransformer", "CompletableOperator", "CompletableEmitter", "CompletableConverter",

            "Single", "SingleSource", "SingleObserver", "SingleOnSubscribe",
            "SingleTransformer", "SingleOperator", "SingleEmitter", "SingleConverter",

            "Maybe", "MaybeSource", "MaybeObserver", "MaybeOnSubscribe",
            "MaybeTransformer", "MaybeOperator", "MaybeEmitter", "MaybeConverter",

            "Observable", "ObservableSource", "Observer", "ObservableOnSubscribe",
            "ObservableTransformer", "ObservableOperator", "ObservableEmitter", "ObservableConverter",

            "Flowable", "Publisher", "Subscriber", "FlowableSubscriber", "FlowableOnSubscribe",
            "FlowableTransformer", "FlowableOperator", "FlowableEmitter", "FlowableConverter",

            "Function", "BiFunction", "Function3", "Function4", "Function5", "Function6",
            "Function7", "Function8", "Function9",

            "Action", "Runnable", "Consumer", "BiConsumer", "Supplier", "Callable", "Void",
            "Throwable", "Optional", "CompletionStage", "BooleanSupplier", "LongConsumer",
            "Predicate", "BiPredicate", "Object",

            "Iterable", "Stream", "Iterator",

            "BackpressureOverflowStrategy", "BackpressureStrategy",
            "Subject", "Processor", "FlowableProcessor",

            "T", "R", "U", "V"
    );

    static final List<String> TYPES_REQUIRING_NONNULL_TYPEARG = Arrays.asList(
            "Iterable", "Stream", "Publisher", "Processor", "Subscriber", "Optional"
    );
    static final List<String> TYPES_FORBIDDEN_NONNULL_TYPEARG = Arrays.asList(
            "Iterable", "Stream", "Publisher", "Processor", "Subscriber", "Optional"
    );

    static final List<String> TYPES_REQUIRING_NONNULL_TYPEARG_ON_FUNC = Arrays.asList(
            "Iterable", "Stream", "Publisher", "Processor", "Subscriber", "Optional",
            "Observer", "SingleObserver", "MaybeObserver", "CompletableObserver"
    );

    static final String COMMON_TYPE_ARG_NAMES = "([A-Z][0-9]?|TOpening|TClosing|TLeft|TLeftEnd|TRight|TRightEnd)";
}
