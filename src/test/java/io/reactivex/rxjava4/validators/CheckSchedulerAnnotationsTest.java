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

import java.lang.classfile.*;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.annotations.SchedulerSupport;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.Observable;

public class CheckSchedulerAnnotationsTest extends RxJavaTest {

    // Via Grok
    static int firstLine(Method m) {
        Class<?> c = m.getDeclaringClass();
        byte[] bytes;
        try (var in = c.getResourceAsStream("/" + c.getName().replace('.', '/') + ".class")) {
            if (in == null) {
                return -1;
            }
            bytes = in.readAllBytes();
        } catch (Exception ex) {
            return 0;
        }

        var cm = ClassFile.of().parse(bytes);
        var desc = MethodTypeDesc.ofDescriptor(
                java.lang.invoke.MethodType.methodType(m.getReturnType(), m.getParameterTypes())
                        .descriptorString()).descriptorString(); // <- had to fix this because Grok made a type error

        for (var mm : cm.methods()) {
            if (mm.methodName().equalsString(m.getName())
                    && mm.methodType().toString().equals(desc)) { // <- Grok mistake, need to compare via canonical strings
                var mc = mm.code();
                if (!mc.isEmpty()) {
                    var lnc = mc.get().findAttribute(Attributes.lineNumberTable());
                    if (!lnc.isEmpty()) {
                        var lnn = lnc.get().lineNumbers();
                        var x = lnn.stream().mapToInt(i -> i.lineNumber()).min().orElse(-2);
                        return x;
                    }
                }
            }
        }
        return -3;
    }

    /// Methods do not need a Scheduler because they effectively block on the current thread
    static final Set<String> skipMethods = new HashSet<>(List.of(
            "fromFuture",
            "blockingAwait"
    ));

    void processClass(Class<?> theClass) {
        var sb = new StringBuilder();

        for (var method : theClass.getMethods()) {
            if (skipMethods.contains(method.getName())) {
                continue;
            }

            var ann = method.getAnnotation(SchedulerSupport.class);

            var hasTimeUnit = Flowable.fromArray(method.getParameters()).any(p -> p.getType() == TimeUnit.class).blockingGet();
            var hasScheduler = Flowable.fromArray(method.getParameters()).any(p -> p.getType() == Scheduler.class).blockingGet();
            var hasExecutor = Flowable.fromArray(method.getParameters()).any(p ->
                p.getType() == Scheduler.class
                || p.getType() == Executor.class
                || p.getType() == ExecutorService.class
                || p.getType() == ScheduledExecutorService.class
            ).blockingGet();

            var lineNum = firstLine(method);

            if (hasTimeUnit) {
                if (hasScheduler || hasExecutor) {
                    if (ann == null) {
                        sb.append("java.lang.AssertionError: missing SchedulerSupport annotation: ")
                        .append(method)
                        .append("\r\n")
                        .append(" at ")
                        .append(theClass.getCanonicalName())
                        .append(".")
                        .append(method.getName())
                        .append("(")
                        .append(theClass.getSimpleName())
                        .append(".java:")
                        .append(lineNum)
                        .append(")\r\n");
                        ;
                    } else {
                        if (!ann.value().equals(SchedulerSupport.CUSTOM)) {
                            sb.append("java.lang.AssertionError: SchedulerSupport annotation is not CUSTOM: ")
                            .append(method)
                            .append("\r\n")
                            .append(" at ")
                            .append(theClass.getCanonicalName())
                            .append(".")
                            .append(method.getName())
                            .append("(")
                            .append(theClass.getSimpleName())
                            .append(".java:")
                            .append(lineNum)
                            .append(")\r\n");
                        }
                    }
                } else {
                    if (ann == null) {
                        sb.append("java.lang.AssertionError: missing SchedulerSupport annotation: ")
                        .append(method)
                        .append("\r\n")
                        .append(" at ")
                        .append(theClass.getCanonicalName())
                        .append(".")
                        .append(method.getName())
                        .append("(")
                        .append(theClass.getSimpleName())
                        .append(".java:")
                        .append(lineNum)
                        .append(")\r\n");
                    } else {
                        if (ann.value().equals(SchedulerSupport.NONE)) {
                            sb.append("java.lang.AssertionError: SchedulerSupport annotation is NONE: ")
                            .append(method)
                            .append("\r\n")
                            .append(" at ")
                            .append(theClass.getCanonicalName())
                            .append(".")
                            .append(method.getName())
                            .append("(")
                            .append(theClass.getSimpleName())
                            .append(".java:")
                            .append(lineNum)
                            .append(")\r\n");
                        }
                    }
                }
            }
        }

        if (sb.length() != 0) {
            sb.insert(0, "\r\n");
            throw new AssertionError(sb.toString());
        }
    }

    @Test
    public void checkObservable() {
        processClass(Observable.class);
    }

    @Test
    public void checkFlowable() {
        processClass(Flowable.class);
    }

    @Test
    public void checkSingle() {
        processClass(Single.class);
    }

    @Test
    public void checkMaybe() {
        processClass(Maybe.class);
    }

    @Test
    public void checkCompletable() {
        processClass(Completable.class);
    }

    @Test
    public void checkStreamable() {
        processClass(Streamable.class);
    }
}
