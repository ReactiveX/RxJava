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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
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

    // TODO later
    // @Test
    public void checkObservable() throws Exception {
        processFile(Observable.class);
    }

    @Test
    public void checkFlowable() throws Exception {
        processFile(Flowable.class);
    }

    // TODO later
    // @Test
    public void checkParallelFlowable() throws Exception {
        processFile(ParallelFlowable.class);
    }

    static void processFile(Class<?> clazz) throws Exception {
        String baseClassName = clazz.getSimpleName();
        File f = TestHelper.findSource(baseClassName);
        if (f == null) {
            return;
        }
        String fullClassName = clazz.getName();

        int errorCount = 0;
        StringBuilder errors = new StringBuilder();

        List<String> lines = Files.readAllLines(f.toPath());

        for (int j = 0; j < lines.size(); j++) {
            String line = lines.get(j).trim();

            if (line.startsWith("public static") || line.startsWith("public final")) {
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

                for (String typeName : CLASS_NAMES) {
                    String typeNameSpaced = typeName + " ";
                    for (int k = 0; k < args.length; k++) {
                        String typeDef = args[k];
                        if (typeDef.contains(typeNameSpaced)
                                && !typeDef.contains("@NonNull")
                                && !typeDef.contains("@Nullable")) {

                            if (!line.contains("@Nullable " + typeName)
                                    && !line.contains("@NonNull " + typeName)) {
                                errorCount++;
                                errors.append("L")
                                .append(j)
                                .append(" - argument ").append(k + 1).append(" - ").append(typeDef)
                                .append(" : Missing argument type nullability annotation |\r\n    ")
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
                }

                if (strippedArgumentsStr.contains("...") && !hasSafeVarargsAnnotation) {
                    errorCount++;
                    errors.append("L")
                    .append(j)
                    .append(" : Missing @SafeVarargs annotation |\r\n    ")
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

        if (errorCount != 0) {
            errors.insert(0, errorCount + " missing annotations\r\n");
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

            "BackpressureOverflowStrategy", "BackpressureStrategy",
            "Subject", "Processor", "FlowableProcessor",

            "T", "R", "U", "V"
    );
}
