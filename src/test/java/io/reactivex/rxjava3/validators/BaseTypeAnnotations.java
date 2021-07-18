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

import static org.junit.Assert.fail;

import java.lang.reflect.*;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.DisposableContainer;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.subjects.*;

/**
 * Verifies several properties.
 * <ul>
 * <li>Certain public base type methods have the {@link CheckReturnValue} present</li>
 * <li>All public base type methods have the {@link SchedulerSupport} present</li>
 * <li>All public base type methods which return Flowable have the {@link BackpressureSupport} present</li>
 * <li>All public base types that don't return Flowable don't have the {@link BackpressureSupport} present (these are copy-paste errors)</li>
 * </ul>
 */
public class BaseTypeAnnotations {

    static void checkCheckReturnValueSupport(Class<?> clazz) {
        StringBuilder b = new StringBuilder();

        for (Method m : clazz.getMethods()) {
            if (m.getDeclaringClass() == clazz) {
                boolean isSubscribeMethod = "subscribe".equals(m.getName()) &&
                        (m.getParameterTypes().length == 0 || m.getParameterTypes()[m.getParameterCount() - 1] == DisposableContainer.class);
                boolean isConnectMethod = "connect".equals(m.getName()) && m.getParameterTypes().length == 0;
                boolean isAnnotationPresent = m.isAnnotationPresent(CheckReturnValue.class);

                if (isSubscribeMethod || isConnectMethod) {
                    if (isAnnotationPresent) {
                        b.append(m.getName()).append(" method has @CheckReturnValue: ").append(m).append("\r\n");
                    }
                    continue;
                }

                if (Modifier.isPrivate(m.getModifiers()) && isAnnotationPresent) {
                    b.append("Private method has @CheckReturnValue: ").append(m).append("\r\n");
                    continue;
                }

                if (m.getReturnType().equals(Void.TYPE)) {
                    if (isAnnotationPresent) {
                        b.append("Void method has @CheckReturnValue: ").append(m).append("\r\n");
                    }
                    continue;
                }

                if (!isAnnotationPresent) {
                    b.append("Missing @CheckReturnValue: ").append(m).append("\r\n");
                }
            }
        }

        if (b.length() != 0) {
            System.out.println(clazz);
            System.out.println("------------------------");
            System.out.println(b);

            fail(b.toString());
        }
    }

    static void checkSchedulerSupport(Class<?> clazz) {
        StringBuilder b = new StringBuilder();

        for (Method m : clazz.getMethods()) {
            if (m.getName().equals("bufferSize")
                    || m.getName().equals("parallelism")) {
                continue;
            }
            if (m.getDeclaringClass() == clazz) {
                if (!m.isAnnotationPresent(SchedulerSupport.class)) {
                    b.append("Missing @SchedulerSupport: ").append(m).append("\r\n");
                } else {
                    SchedulerSupport ann = m.getAnnotation(SchedulerSupport.class);

                    if (ann.value().equals(SchedulerSupport.CUSTOM)) {
                        boolean found = false;
                        for (Class<?> paramclazz : m.getParameterTypes()) {
                            if (Scheduler.class.isAssignableFrom(paramclazz)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            b.append("Marked with CUSTOM scheduler but no Scheduler parameter: ").append(m).append("\r\n");
                        }
                    } else {
                        for (Class<?> paramclazz : m.getParameterTypes()) {
                            if (Scheduler.class.isAssignableFrom(paramclazz)) {
                                if (!m.getName().equals("timestamp") && !m.getName().equals("timeInterval")) {
                                    b.append("Marked with specific scheduler but Scheduler parameter found: ").append(m).append("\r\n");
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (b.length() != 0) {
            System.out.println(clazz);
            System.out.println("------------------------");
            System.out.println(b);

            fail(b.toString());
        }
    }

    static void checkBackpressureSupport(Class<?> clazz) {
        StringBuilder b = new StringBuilder();

        for (Method m : clazz.getMethods()) {
            if (m.getName().equals("bufferSize")
                    || m.getName().equals("parallelism")) {
                continue;
            }
            if (m.getDeclaringClass() == clazz) {
                if (clazz == Flowable.class || clazz == ParallelFlowable.class) {
                    if (!m.isAnnotationPresent(BackpressureSupport.class)) {
                        b.append("No @BackpressureSupport annotation (being ")
                        .append(clazz.getSimpleName())
                        .append("): ").append(m).append("\r\n");
                    }
                } else {
                    if (m.getReturnType() == Flowable.class
                            || m.getReturnType() == ParallelFlowable.class) {
                        if (!m.isAnnotationPresent(BackpressureSupport.class)) {
                            b.append("No @BackpressureSupport annotation (having ")
                            .append(m.getReturnType().getSimpleName())
                            .append(" return): ").append(m).append("\r\n");
                        }
                    } else {
                        boolean found = false;
                        for (Class<?> paramclazz : m.getParameterTypes()) {
                            if (Publisher.class.isAssignableFrom(paramclazz)) {
                                found = true;
                                break;
                            }
                        }

                        if (found) {
                            if (!m.isAnnotationPresent(BackpressureSupport.class)) {
                                b.append("No @BackpressureSupport annotation (has Publisher param): ").append(m).append("\r\n");
                            }
                        } else {
                            if (m.isAnnotationPresent(BackpressureSupport.class)) {
                                b.append("Unnecessary @BackpressureSupport annotation: ").append(m).append("\r\n");
                            }
                        }
                    }
                }
            }
        }

        if (b.length() != 0) {
            System.out.println(clazz);
            System.out.println("------------------------");
            System.out.println(b);

            fail(b.toString());
        }
    }

    @Test
    public void checkReturnValueFlowable() {
        checkCheckReturnValueSupport(Flowable.class);
    }

    @Test
    public void checkReturnValueObservable() {
        checkCheckReturnValueSupport(Observable.class);
    }

    @Test
    public void checkReturnValueSingle() {
        checkCheckReturnValueSupport(Single.class);
    }

    @Test
    public void checkReturnValueCompletable() {
        checkCheckReturnValueSupport(Completable.class);
    }

    @Test
    public void checkReturnValueMaybe() {
        checkCheckReturnValueSupport(Maybe.class);
    }

    @Test
    public void checkReturnValueConnectableObservable() {
        checkCheckReturnValueSupport(ConnectableObservable.class);
    }

    @Test
    public void checkReturnValueConnectableFlowable() {
        checkCheckReturnValueSupport(ConnectableFlowable.class);
    }

    @Test
    public void checkReturnValueParallelFlowable() {
        checkCheckReturnValueSupport(ParallelFlowable.class);
    }

    @Test
    public void checkReturnValueAsyncSubject() {
        checkCheckReturnValueSupport(AsyncSubject.class);
    }

    @Test
    public void checkReturnValueBehaviorSubject() {
        checkCheckReturnValueSupport(BehaviorSubject.class);
    }

    @Test
    public void checkReturnValuePublishSubject() {
        checkCheckReturnValueSupport(PublishSubject.class);
    }

    @Test
    public void checkReturnValueReplaySubject() {
        checkCheckReturnValueSupport(ReplaySubject.class);
    }

    @Test
    public void checkReturnValueUnicastSubject() {
        checkCheckReturnValueSupport(UnicastSubject.class);
    }

    @Test
    public void checkReturnValueAsyncProcessor() {
        checkCheckReturnValueSupport(AsyncProcessor.class);
    }

    @Test
    public void checkReturnValueBehaviorProcessor() {
        checkCheckReturnValueSupport(BehaviorProcessor.class);
    }

    @Test
    public void checkReturnValuePublishProcessor() {
        checkCheckReturnValueSupport(PublishProcessor.class);
    }

    @Test
    public void checkReturnValueReplayProcessor() {
        checkCheckReturnValueSupport(ReplayProcessor.class);
    }

    @Test
    public void checkReturnValueUnicastProcessor() {
        checkCheckReturnValueSupport(UnicastProcessor.class);
    }

    @Test
    public void checkReturnValueMulticastProcessor() {
        checkCheckReturnValueSupport(MulticastProcessor.class);
    }

    @Test
    public void checkReturnValueSubject() {
        checkCheckReturnValueSupport(Subject.class);
    }

    @Test
    public void checkReturnValueFlowableProcessor() {
        checkCheckReturnValueSupport(FlowableProcessor.class);
    }

    @Test
    public void schedulerSupportFlowable() {
        checkSchedulerSupport(Flowable.class);
    }

    @Test
    public void schedulerSupportObservable() {
        checkSchedulerSupport(Observable.class);
    }

    @Test
    public void schedulerSupportSingle() {
        checkSchedulerSupport(Single.class);
    }

    @Test
    public void schedulerSupportCompletable() {
        checkSchedulerSupport(Completable.class);
    }

    @Test
    public void schedulerSupportMaybe() {
        checkSchedulerSupport(Maybe.class);
    }

    @Test
    public void schedulerSupportConnectableObservable() {
        checkSchedulerSupport(ConnectableObservable.class);
    }

    @Test
    public void schedulerSupportConnectableFlowable() {
        checkSchedulerSupport(ConnectableFlowable.class);
    }

    @Test
    public void schedulerSupportParallelFlowable() {
        checkSchedulerSupport(ParallelFlowable.class);
    }

    @Test
    public void backpressureSupportFlowable() {
        checkBackpressureSupport(Flowable.class);
    }

    @Test
    public void backpressureSupportObservable() {
        checkBackpressureSupport(Observable.class);
    }

    @Test
    public void backpressureSupportSingle() {
        checkBackpressureSupport(Single.class);
    }

    @Test
    public void backpressureSupportCompletable() {
        checkBackpressureSupport(Completable.class);
    }

    @Test
    public void backpressureSupportMaybe() {
        checkBackpressureSupport(Maybe.class);
    }

    @Test
    public void backpressureSupportConnectableFlowable() {
        checkBackpressureSupport(ConnectableFlowable.class);
    }

    @Test
    public void backpressureSupportConnectableObservable() {
        checkBackpressureSupport(ConnectableObservable.class);
    }

    @Test
    public void backpressureSupportParallelFlowable() {
        checkBackpressureSupport(ParallelFlowable.class);
    }
}
