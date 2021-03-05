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

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Callable;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.parallel.ParallelFlowable;

/**
 * Verify that an operator method uses base interfaces as its direct input or
 * has lambdas returning base interfaces.
 */
public class OperatorsUseInterfaces {

    @Test
    public void checkFlowable() {
        checkClass(Flowable.class);
    }

    @Test
    public void checkObservable() {
        checkClass(Observable.class);
    }

    @Test
    public void checkMaybe() {
        checkClass(Maybe.class);
    }

    @Test
    public void checkSingle() {
        checkClass(Single.class);
    }

    @Test
    public void checkCompletable() {
        checkClass(Completable.class);
    }

    @Test
    public void checkParallelFlowable() {
        checkClass(ParallelFlowable.class);
    }

    void checkClass(Class<?> clazz) {
        StringBuilder error = new StringBuilder();
        int errors = 0;

        for (Method method : clazz.getMethods()) {
            if (method.getDeclaringClass() == clazz) {
                int pidx = 1;
                for (Parameter param : method.getParameters()) {
                    Class<?> type = param.getType();
                    if (type.isArray()) {
                        type = type.getComponentType();
                    }
                    if (CLASSES.contains(type)) {
                        errors++;
                        error.append("Non-interface input parameter #")
                        .append(pidx)
                        .append(": ")
                        .append(type)
                        .append("\r\n")
                        .append("    ")
                        .append(method)
                        .append("\r\n")
                        ;
                    }
                    if (CAN_RETURN.contains(type)) {
                        Type gtype = method.getGenericParameterTypes()[pidx - 1];
                        if (gtype instanceof GenericArrayType) {
                            gtype = ((GenericArrayType)gtype).getGenericComponentType();
                        }
                        ParameterizedType ptype = (ParameterizedType)gtype;
                        for (;;) {
                            Type[] parameterArgTypes = ptype.getActualTypeArguments();
                            Type argType = parameterArgTypes[parameterArgTypes.length - 1];
                            if (argType instanceof GenericArrayType) {
                                argType = ((GenericArrayType)argType).getGenericComponentType();
                            }
                            if (argType instanceof ParameterizedType) {
                                ParameterizedType lastArg = (ParameterizedType)argType;

                                if (CLASSES.contains(lastArg.getRawType())) {
                                    errors++;
                                    error.append("Non-interface lambda return #")
                                    .append(pidx)
                                    .append(": ")
                                    .append(type)
                                    .append("\r\n")
                                    .append("    ")
                                    .append(method)
                                    .append("\r\n")
                                    ;
                                }

                                if (CAN_RETURN.contains(lastArg.getRawType())) {
                                    ptype = lastArg;
                                    continue;
                                }
                            }
                            break;
                        }
                    }
                    pidx++;
                }
            }
        }

        if (errors != 0) {
            error.insert(0, "Found " + errors + " issues\r\n");
            fail(error.toString());
        }
    }

    public void method1(Flowable<?> f) {
        // self-test
    }

    public void method2(Callable<Flowable<?>> c) {
        // self-test
    }

    public void method3(Supplier<Publisher<Flowable<?>>> c) {
        // self-test
    }

    public void method4(Flowable<?>[] array) {
        // self-test
    }

    public void method5(Callable<Flowable<?>[]> c) {
        // self-test
    }

    public void method6(Callable<Publisher<Flowable<?>[]>> c) {
        // self-test
    }

    @Test
    public void checkSelf() {
        try {
            checkClass(OperatorsUseInterfaces.class);
            throw new RuntimeException("Should have failed");
        } catch (AssertionError expected) {
            assertTrue(expected.toString(), expected.toString().contains("method1"));
            assertTrue(expected.toString(), expected.toString().contains("method2"));
            assertTrue(expected.toString(), expected.toString().contains("method3"));
            assertTrue(expected.toString(), expected.toString().contains("method4"));
            assertTrue(expected.toString(), expected.toString().contains("method5"));
            assertTrue(expected.toString(), expected.toString().contains("method6"));
        }
    }

    static final Set<Class<?>> CLASSES = new HashSet<>(Arrays.asList(
            Flowable.class, Observable.class,
            Maybe.class, Single.class,
            Completable.class
    ));

    static final Set<Class<?>> CAN_RETURN = new HashSet<>(Arrays.asList(
            Callable.class, Supplier.class,
            Function.class, BiFunction.class, Function3.class, Function4.class,
            Function5.class, Function6.class, Function7.class, Function8.class,
            Function9.class,
            Publisher.class, ObservableSource.class, MaybeSource.class, SingleSource.class
    ));
}
