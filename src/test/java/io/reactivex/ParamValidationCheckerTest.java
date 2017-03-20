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

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.parallel.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Check that static and instance methods validate their parameters against
 * null and invalid values properly.
 */
public class ParamValidationCheckerTest {

    @Test(timeout = 30000)
    public void checkFlowable() {
        checkClass(Flowable.class);
    }

    @Test(timeout = 30000)
    public void checkObservable() {
        checkClass(Observable.class);
    }

    @Test(timeout = 30000)
    public void checkSingle() {
        checkClass(Single.class);
    }

    @Test(timeout = 30000)
    public void checkMaybe() {
        checkClass(Maybe.class);
    }

    @Test(timeout = 30000)
    public void checkCompletable() {
        checkClass(Completable.class);
    }

    @Test(timeout = 30000)
    public void checkParallelFlowable() {
        checkClass(ParallelFlowable.class);
    }

    // ---------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------

    static Map<String, List<ParamOverride>> overrides;

    static Map<String, List<ParamIgnore>> ignores;

    static Map<Class<?>, Object> defaultValues;

    static Map<Class<?>, List<Object>> defaultInstances;

    static {
        overrides = new HashMap<String, List<ParamOverride>>();

        // ***********************************************************************************************************************

        // zero index allowed
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "elementAt", Long.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "elementAt", Long.TYPE, Object.class));

        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "elementAtOrError", Long.TYPE));

        // negative skip count is ignored
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "skip", Long.TYPE));
        // negative skip time is considered as zero skip time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "skip", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "skip", Long.TYPE, TimeUnit.class, Scheduler.class));

        // can start with zero initial request
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "test", Long.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "test", Long.TYPE, Boolean.TYPE));

        // negative timeout time is considered as zero timeout time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, Publisher.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, Scheduler.class, Publisher.class));

        // negative buffer time is considered as zero buffer time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, TimeUnit.class, Integer.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, TimeUnit.class, Scheduler.class, Integer.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, TimeUnit.class, Scheduler.class, Integer.TYPE, Callable.class, Boolean.TYPE));

        // negative time/skip is considered zero time/skip
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class, Callable.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class, Callable.class));

        // negative timeout is allowed
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "fromFuture", Future.class, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "fromFuture", Future.class, Long.TYPE, TimeUnit.class, Scheduler.class));

        // null default is allowed
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "blockingLast", Object.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "timer", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "timer", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "interval", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "interval", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "interval", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "interval", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "interval", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "interval", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class, Boolean.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE));

        // null default is allowed
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "blockingMostRecent", Object.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "delaySubscription", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "delaySubscription", Long.TYPE, TimeUnit.class, Scheduler.class));

        // null default is allowed
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "blockingFirst", Object.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "debounce", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "debounce", Long.TYPE, TimeUnit.class, Scheduler.class));

        // null Action allowed
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "onBackpressureBuffer", Long.TYPE, Action.class, BackpressureOverflowStrategy.class));

        // zero repeat is allowed
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "repeat", Long.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "replay", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "replay", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "replay", Integer.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "replay", Integer.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "replay", Function.class, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "replay", Function.class, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 2, ParamMode.ANY, "replay", Function.class, Integer.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 2, ParamMode.ANY, "replay", Function.class, Integer.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));

        // zero retry is allowed
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "retry", Long.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "retry", Long.TYPE, Predicate.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "throttleWithTimeout", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "throttleWithTimeout", Long.TYPE, TimeUnit.class, Scheduler.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "take", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "take", Long.TYPE, TimeUnit.class, Scheduler.class));

        // zero retry is allowed
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "take", Long.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "sample", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "sample", Long.TYPE, TimeUnit.class, Boolean.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "sample", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "sample", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "takeLast", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "takeLast", Long.TYPE, TimeUnit.class, Boolean.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "takeLast", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "takeLast", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "takeLast", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE, Integer.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE, Integer.TYPE));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 1, ParamMode.ANY, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE, Integer.TYPE));

        // take last 0 is allowed
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "takeLast", Integer.TYPE));

        // skip last 0 is allowed
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.NON_NEGATIVE, "skipLast", Integer.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "skipLast", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "skipLast", Long.TYPE, TimeUnit.class, Boolean.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "skipLast", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "skipLast", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "skipLast", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE, Integer.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "throttleFirst", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "throttleFirst", Long.TYPE, TimeUnit.class, Scheduler.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "throttleLast", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "throttleLast", Long.TYPE, TimeUnit.class, Scheduler.class));


        // negative buffer time is considered as zero buffer time
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Long.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Long.TYPE, Boolean.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Scheduler.class, Long.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Scheduler.class, Long.TYPE, Boolean.TYPE));
        addOverride(new ParamOverride(Flowable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Scheduler.class, Long.TYPE, Boolean.TYPE, Integer.TYPE));

        // ***********************************************************************************************************************

        // negative timeout time is considered as zero timeout time
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, CompletableSource.class));
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, Scheduler.class, CompletableSource.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.ANY, "timer", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.ANY, "timer", Long.TYPE, TimeUnit.class, Scheduler.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE));

        // zero repeat is allowed
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.NON_NEGATIVE, "repeat", Long.TYPE));

        // zero retry is allowed
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.NON_NEGATIVE, "retry", Long.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.ANY, "blockingGet", Long.TYPE, TimeUnit.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Completable.class, 0, ParamMode.ANY, "blockingAwait", Long.TYPE, TimeUnit.class));

        // ***********************************************************************************************************************

        // negative timeout time is considered as zero timeout time
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, MaybeSource.class));
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, Scheduler.class, MaybeSource.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.ANY, "timer", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.ANY, "timer", Long.TYPE, TimeUnit.class, Scheduler.class));

        // negative timeout is allowed
        addOverride(new ParamOverride(Maybe.class, 1, ParamMode.ANY, "fromFuture", Future.class, Long.TYPE, TimeUnit.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class, Scheduler.class));

        // zero repeat is allowed
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.NON_NEGATIVE, "repeat", Long.TYPE));

        // zero retry is allowed
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.NON_NEGATIVE, "retry", Long.TYPE));
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.NON_NEGATIVE, "retry", Long.TYPE, Predicate.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.ANY, "delaySubscription", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Maybe.class, 0, ParamMode.ANY, "delaySubscription", Long.TYPE, TimeUnit.class, Scheduler.class));

        // ***********************************************************************************************************************

        // negative timeout time is considered as zero timeout time
        addOverride(new ParamOverride(Single.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Single.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Single.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, SingleSource.class));
        addOverride(new ParamOverride(Single.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, Scheduler.class, SingleSource.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Single.class, 0, ParamMode.ANY, "timer", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Single.class, 0, ParamMode.ANY, "timer", Long.TYPE, TimeUnit.class, Scheduler.class));

        // negative timeout is allowed
        addOverride(new ParamOverride(Single.class, 1, ParamMode.ANY, "fromFuture", Future.class, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Single.class, 1, ParamMode.ANY, "fromFuture", Future.class, Long.TYPE, TimeUnit.class, Scheduler.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Single.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Single.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class, Scheduler.class));


        // zero repeat is allowed
        addOverride(new ParamOverride(Single.class, 0, ParamMode.NON_NEGATIVE, "repeat", Long.TYPE));

        // zero retry is allowed
        addOverride(new ParamOverride(Single.class, 0, ParamMode.NON_NEGATIVE, "retry", Long.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Single.class, 0, ParamMode.ANY, "delaySubscription", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Single.class, 0, ParamMode.ANY, "delaySubscription", Long.TYPE, TimeUnit.class, Scheduler.class));

        // ***********************************************************************************************************************

        // zero index allowed
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "elementAt", Long.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "elementAt", Long.TYPE, Object.class));

        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "elementAtOrError", Long.TYPE));

        // negative skip count is ignored
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "skip", Long.TYPE));
        // negative skip time is considered as zero skip time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "skip", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "skip", Long.TYPE, TimeUnit.class, Scheduler.class));

        // negative timeout time is considered as zero timeout time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, ObservableSource.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "timeout", Long.TYPE, TimeUnit.class, Scheduler.class, ObservableSource.class));

        // negative buffer time is considered as zero buffer time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, TimeUnit.class, Integer.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, TimeUnit.class, Scheduler.class, Integer.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, TimeUnit.class, Scheduler.class, Integer.TYPE, Callable.class, Boolean.TYPE));

        // negative time/skip is considered zero time/skip
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class, Callable.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "buffer", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class, Callable.class));

        // negative timeout is allowed
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "fromFuture", Future.class, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "fromFuture", Future.class, Long.TYPE, TimeUnit.class, Scheduler.class));

        // null default is allowed
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "blockingLast", Object.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "timer", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "timer", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "interval", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "interval", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "interval", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "interval", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "interval", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "interval", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class, Boolean.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "delay", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE));

        // null default is allowed
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "blockingMostRecent", Object.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "delaySubscription", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "delaySubscription", Long.TYPE, TimeUnit.class, Scheduler.class));

        // null default is allowed
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "blockingFirst", Object.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "debounce", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "debounce", Long.TYPE, TimeUnit.class, Scheduler.class));

        // zero repeat is allowed
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "repeat", Long.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "replay", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "replay", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "replay", Integer.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "replay", Integer.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "replay", Function.class, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "replay", Function.class, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 2, ParamMode.ANY, "replay", Function.class, Integer.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 2, ParamMode.ANY, "replay", Function.class, Integer.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));

        // zero retry is allowed
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "retry", Long.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "retry", Long.TYPE, Predicate.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "throttleWithTimeout", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "throttleWithTimeout", Long.TYPE, TimeUnit.class, Scheduler.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "take", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "take", Long.TYPE, TimeUnit.class, Scheduler.class));

        // zero retry is allowed
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "take", Long.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "sample", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "sample", Long.TYPE, TimeUnit.class, Boolean.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "sample", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "sample", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "takeLast", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "takeLast", Long.TYPE, TimeUnit.class, Boolean.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "takeLast", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "takeLast", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "takeLast", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE, Integer.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE, Integer.TYPE));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 1, ParamMode.ANY, "takeLast", Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE, Integer.TYPE));

        // take last 0 is allowed
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "takeLast", Integer.TYPE));

        // skip last 0 is allowed
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.NON_NEGATIVE, "skipLast", Integer.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "skipLast", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "skipLast", Long.TYPE, TimeUnit.class, Boolean.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "skipLast", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "skipLast", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "skipLast", Long.TYPE, TimeUnit.class, Scheduler.class, Boolean.TYPE, Integer.TYPE));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "throttleFirst", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "throttleFirst", Long.TYPE, TimeUnit.class, Scheduler.class));

        // negative time is considered as zero time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "throttleLast", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "throttleLast", Long.TYPE, TimeUnit.class, Scheduler.class));


        // negative buffer time is considered as zero buffer time
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Long.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Long.TYPE, Boolean.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Scheduler.class, Long.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Scheduler.class));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Scheduler.class, Long.TYPE, Boolean.TYPE));
        addOverride(new ParamOverride(Observable.class, 0, ParamMode.ANY, "window", Long.TYPE, TimeUnit.class, Scheduler.class, Long.TYPE, Boolean.TYPE, Integer.TYPE));

        // -----------------------------------------------------------------------------------

        ignores = new HashMap<String, List<ParamIgnore>>();

        // needs special param validation due to (long)start + end - 1 <= Integer.MAX_VALUE
        addIgnore(new ParamIgnore(Flowable.class, "range", Integer.TYPE, Integer.TYPE));
        addIgnore(new ParamIgnore(Flowable.class, "rangeLong", Long.TYPE, Long.TYPE));
        addIgnore(new ParamIgnore(Flowable.class, "intervalRange", Long.TYPE, Long.TYPE, Long.TYPE, TimeUnit.class));
        addIgnore(new ParamIgnore(Flowable.class, "intervalRange", Long.TYPE, Long.TYPE, Long.TYPE, Long.TYPE, TimeUnit.class));
        addIgnore(new ParamIgnore(Flowable.class, "intervalRange", Long.TYPE, Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addIgnore(new ParamIgnore(Flowable.class, "intervalRange", Long.TYPE, Long.TYPE, Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));

        addIgnore(new ParamIgnore(Flowable.class, "unsafeCreate", Publisher.class));

        // needs special param validation due to (long)start + end - 1 <= Integer.MAX_VALUE
        addIgnore(new ParamIgnore(Observable.class, "range", Integer.TYPE, Integer.TYPE));
        addIgnore(new ParamIgnore(Observable.class, "rangeLong", Long.TYPE, Long.TYPE));
        addIgnore(new ParamIgnore(Observable.class, "intervalRange", Long.TYPE, Long.TYPE, Long.TYPE, TimeUnit.class));
        addIgnore(new ParamIgnore(Observable.class, "intervalRange", Long.TYPE, Long.TYPE, Long.TYPE, Long.TYPE, TimeUnit.class));
        addIgnore(new ParamIgnore(Observable.class, "intervalRange", Long.TYPE, Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));
        addIgnore(new ParamIgnore(Observable.class, "intervalRange", Long.TYPE, Long.TYPE, Long.TYPE, Long.TYPE, TimeUnit.class, Scheduler.class));

        addIgnore(new ParamIgnore(Observable.class, "unsafeCreate", ObservableSource.class));

        addIgnore(new ParamIgnore(Maybe.class, "unsafeCreate", MaybeSource.class));

        addIgnore(new ParamIgnore(Single.class, "unsafeCreate", SingleSource.class));

        addIgnore(new ParamIgnore(Completable.class, "unsafeCreate", CompletableSource.class));

        // -----------------------------------------------------------------------------------

        defaultValues = new HashMap<Class<?>, Object>();

        defaultValues.put(Publisher.class, new NeverPublisher());
        defaultValues.put(Flowable.class, new NeverPublisher());

        defaultValues.put(ObservableSource.class, new NeverObservable());
        defaultValues.put(Observable.class, new NeverObservable());

        defaultValues.put(SingleSource.class, new NeverSingle());
        defaultValues.put(Single.class, new NeverSingle());

        defaultValues.put(MaybeSource.class, new NeverMaybe());
        defaultValues.put(Maybe.class, new NeverMaybe());

        defaultValues.put(CompletableSource.class, new NeverCompletable());
        defaultValues.put(Completable.class, new NeverCompletable());

        defaultValues.put(Action.class, Functions.EMPTY_ACTION);
        defaultValues.put(Runnable.class, Functions.EMPTY_RUNNABLE);
        defaultValues.put(Consumer.class, Functions.emptyConsumer());
        defaultValues.put(LongConsumer.class, Functions.EMPTY_LONG_CONSUMER);
        defaultValues.put(Function.class, Functions.justFunction(1));
        defaultValues.put(Callable.class, Functions.justCallable(1));
        defaultValues.put(Iterable.class, Collections.emptyList());
        defaultValues.put(Object.class, 1);
        defaultValues.put(Class.class, Integer.class);
        Object af = new AllFunctionals();
        for (Class<?> interfaces : AllFunctionals.class.getInterfaces()) {
            defaultValues.put(interfaces, af);
        }
        defaultValues.put(TimeUnit.class, TimeUnit.SECONDS);
        defaultValues.put(Scheduler.class, Schedulers.single());
        defaultValues.put(BackpressureStrategy.class, BackpressureStrategy.MISSING);
        defaultValues.put(BackpressureOverflowStrategy.class, BackpressureOverflowStrategy.ERROR);
        defaultValues.put(Throwable.class, new TestException());

        defaultValues.put(Publisher[].class, new Publisher[] { new NeverPublisher(), new NeverPublisher() });
        defaultValues.put(ObservableSource[].class, new ObservableSource[] { new NeverObservable(), new NeverObservable() });
        defaultValues.put(SingleSource[].class, new SingleSource[] { new NeverSingle(), new NeverSingle() });
        defaultValues.put(MaybeSource[].class, new MaybeSource[] { new NeverMaybe(), new NeverMaybe() });
        defaultValues.put(CompletableSource[].class, new CompletableSource[] { new NeverCompletable(), new NeverCompletable() });

        defaultValues.put(Object[].class, new Object[] { new Object(), new Object() });
        defaultValues.put(Future.class, new FutureTask<Object>(Functions.EMPTY_RUNNABLE, 1));

        defaultValues.put(ParallelFlowable.class, ParallelFlowable.from(Flowable.never()));
        defaultValues.put(Subscriber[].class, new Subscriber[] { new AllFunctionals() });

        defaultValues.put(ParallelFailureHandling.class, ParallelFailureHandling.ERROR);

        // -----------------------------------------------------------------------------------

        defaultInstances = new HashMap<Class<?>, List<Object>>();

//        addDefaultInstance(Flowable.class, Flowable.empty(), "Empty()");
//        addDefaultInstance(Flowable.class, Flowable.empty().hide(), "Empty().Hide()");
        addDefaultInstance(Flowable.class, Flowable.just(1), "Just(1)");
        addDefaultInstance(Flowable.class, Flowable.just(1).hide(), "Just(1).Hide()");
//        addDefaultInstance(Flowable.class, Flowable.range(1, 3), "Range(1, 3)");
//        addDefaultInstance(Flowable.class, Flowable.range(1, 3).hide(), "Range(1, 3).Hide()");

//        addDefaultInstance(Observable.class, Observable.empty(), "Empty()");
//        addDefaultInstance(Observable.class, Observable.empty().hide(), "Empty().Hide()");
        addDefaultInstance(Observable.class, Observable.just(1), "Just(1)");
        addDefaultInstance(Observable.class, Observable.just(1).hide(), "Just(1).Hide()");
//        addDefaultInstance(Observable.class, Observable.range(1, 3), "Range(1, 3)");
//        addDefaultInstance(Observable.class, Observable.range(1, 3).hide(), "Range(1, 3).Hide()");

        addDefaultInstance(Completable.class, Completable.complete(), "Complete()");
        addDefaultInstance(Completable.class, Completable.complete().hide(), "Complete().hide()");

        addDefaultInstance(Single.class, Single.just(1), "Just(1)");
        addDefaultInstance(Single.class, Single.just(1).hide(), "Just(1).Hide()");

        addDefaultInstance(Maybe.class, Maybe.just(1), "Just(1)");
        addDefaultInstance(Maybe.class, Maybe.just(1).hide(), "Just(1).Hide()");

        addDefaultInstance(ParallelFlowable.class, Flowable.just(1).parallel(), "Just(1)");
}

    static void addIgnore(ParamIgnore ignore) {
        String key = ignore.toString();
        List<ParamIgnore> list = ignores.get(key);
        if (list == null) {
            list = new ArrayList<ParamIgnore>();
            ignores.put(key, list);
        }
        list.add(ignore);
    }

    static void addOverride(ParamOverride ignore) {
        String key = ignore.toString();
        List<ParamOverride> list = overrides.get(key);
        if (list == null) {
            list = new ArrayList<ParamOverride>();
            overrides.put(key, list);
        }
        list.add(ignore);
    }

    static void addDefaultInstance(Class<?> clazz, Object o, String tag) {
        List<Object> list = defaultInstances.get(clazz);
        if (list == null) {
            list = new ArrayList<Object>();
            defaultInstances.put(clazz, list);
        }
        list.add(o);
        list.add(tag);
    }

    Object defaultPrimitive(Class<?> clazz, ParamOverride override) {
        if (Integer.TYPE == clazz) {
            if (override != null) {
                return 0;
            }
            return 1;
        }

        if (Long.TYPE == clazz) {
            if (override != null) {
                return 0L;
            }
            return 1L;
        }

        if (Boolean.TYPE == clazz) {
            return true;
        }

        return null;
    }

    void addCheckPrimitive(Class<?> clazz, ParamOverride override, List<Object> values) {
        if (Integer.TYPE == clazz) {
            values.add(-2);
            values.add(override != null && override.mode == ParamMode.ANY);
            values.add(-1);
            values.add(override != null && override.mode == ParamMode.ANY);
            values.add(0);
            values.add(override != null);
            values.add(1);
            values.add(true); // should succeed
            values.add(2);
            values.add(true);
        }

        if (Long.TYPE == clazz) {
            values.add(-2L);
            values.add(override != null && override.mode == ParamMode.ANY);
            values.add(-1L);
            values.add(override != null && override.mode == ParamMode.ANY);
            values.add(0L);
            values.add(override != null);
            values.add(1L);
            values.add(true); // should succeed
            values.add(2L);
            values.add(true);
        }

        if (Boolean.TYPE == clazz) {
            values.add(false);
            values.add(true);
            values.add(true);
            values.add(true);
        }
    }

    void checkClass(Class<?> clazz) {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            StringBuilder b = new StringBuilder();
            int fail = 0;

            outer:
            for (Method m : clazz.getMethods()) {
                if (m.getDeclaringClass() != clazz) {
                    continue;
                }

                String key = clazz.getName() + " " + m.getName();

                List<ParamIgnore> ignoreList = ignores.get(key);
                if (ignoreList != null) {
                    for (ParamIgnore e : ignoreList) {
                        if (Arrays.equals(e.arguments, m.getParameterTypes())) {
                            System.out.println("CheckClass - ignore: " + m);
                            continue outer;
                        }
                    }
                }

                List<ParamOverride> overrideList = overrides.get(key);

                List<Object> baseObjects = new ArrayList<Object>();

                if ((m.getModifiers() & Modifier.STATIC) != 0) {
                    baseObjects.add(null);
                    baseObjects.add("NULL");
                } else {
                    List<Object> defaultInstancesList = defaultInstances.get(clazz);
                    if (defaultInstancesList == null) {
                        b.append("\r\nNo default instances for " + clazz);
                        fail++;
                        continue outer;
                    }
                    baseObjects.addAll(defaultInstancesList);
                }

                for (int ii = 0; ii < baseObjects.size(); ii += 2) {
                    Object baseObject = baseObjects.get(ii);
                    Object tag = baseObjects.get(ii + 1);
                    Class<?>[] params = m.getParameterTypes();
                    int n = params.length;

                    for (int i = 0; i < n; i++) {
                        ParamOverride overrideEntry = null;
                        if (overrideList != null) {
                            for (ParamOverride e : overrideList) {
                                if (e.index == i && Arrays.equals(e.arguments, params)) {
                                    overrideEntry = e;
                                    break;
                                }
                            }
                        }

                        Class<?> entryClass = params[i];

                        Object[] callParams = new Object[n];

                        for (int j = 0; j < n; j++) {
                            if (j != i) {
                                if (params[j].isPrimitive()) {
                                    ParamOverride overrideParam = null;
                                    if (overrideList != null) {
                                        for (ParamOverride e : overrideList) {
                                            if (e.index == j && Arrays.equals(e.arguments, params)) {
                                                overrideParam = e;
                                                break;
                                            }
                                        }
                                    }
                                    Object def = defaultPrimitive(params[j], overrideParam);
                                    if (def == null) {
                                        b.append("\r\nMissing default non-null value for " + m + " # " + j + " (" + params[j] + ")");
                                        fail++;
                                        continue outer;
                                    }
                                    callParams[j] = def;
                                } else {
                                    Object def = defaultValues.get(params[j]);
                                    if (def == null) {
                                        b.append("\r\nMissing default non-null value for " + m + " # " + j + " (" + params[j] + ")");
                                        fail++;
                                        continue outer;
                                    }
                                    callParams[j] = def;
                                }
                            }
                        }

                        List<Object> entryValues = new ArrayList<Object>();

                        if (entryClass.isPrimitive()) {
                            addCheckPrimitive(params[i], overrideEntry, entryValues);
                        } else {
                            entryValues.add(null);
                            entryValues.add(overrideEntry != null && overrideEntry.mode == ParamMode.ANY);

                            Object def = defaultValues.get(params[i]);
                            if (def == null) {
                                b.append("\r\nMissing default non-null value for " + m + " # " + i + " (" + params[i] + ")");
                                fail++;
                                continue outer;
                            }
                            entryValues.add(def);
                            entryValues.add(true);
                        }

                        for (int k = 0; k < entryValues.size(); k += 2) {
                            Object[] callParams2 = callParams.clone();

                            Object p = entryValues.get(k);
                            callParams2[i] = p;
                            boolean shouldSucceed = (Boolean)entryValues.get(k + 1);

                            boolean success = false;
                            Throwable error = null;
                            errors.clear();
                            try {
                                m.invoke(baseObject, callParams2);
                                success = true;
                            } catch (Throwable ex) {
                                // let it fail
                                error = ex;
                            }

                            if (success != shouldSucceed) {
                                fail++;
                                if (shouldSucceed) {
                                    b.append("\r\nFailed (should have succeeded): " + m + " # " + i + " = " + p + ", tag = " + tag + ", params = " + Arrays.toString(callParams2));
                                    b.append("\r\n    ").append(error);
                                    if (error.getCause() != null) {
                                        b.append("\r\n    ").append(error.getCause());
                                    }
                                } else {
                                    b.append("\r\nNo failure (should have failed): " + m + " # " + i + " = " + p + ", tag = " + tag + ", params = " + Arrays.toString(callParams2));
                                }
                                continue outer;
                            }
                            if (!errors.isEmpty()) {
                                fail++;
                                b.append("\r\nUndeliverable errors:");
                                for (Throwable err : errors) {
                                    b.append("\r\n    ").append(err);
                                    if (err.getCause() != null) {
                                        b.append("\r\n    ").append(err.getCause());
                                    }
                                }
                                continue outer;
                            }
                        }
                    }
                }
            }

            if (fail != 0) {
                throw new AssertionError("Parameter validation problems: " + fail + b.toString());
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @SuppressWarnings("rawtypes")
    static final class AllFunctionals
    implements BiFunction, BiConsumer,
    Predicate, BiPredicate, BooleanSupplier,
    Function3, Function4, Function5, Function6, Function7, Function8, Function9,
    FlowableOnSubscribe, ObservableOnSubscribe, SingleOnSubscribe, MaybeOnSubscribe, CompletableOnSubscribe,
    FlowableTransformer, ObservableTransformer, SingleTransformer, MaybeTransformer, CompletableTransformer,
    Subscriber, FlowableSubscriber, Observer, SingleObserver, MaybeObserver, CompletableObserver,
    FlowableOperator, ObservableOperator, SingleOperator, MaybeOperator, CompletableOperator,
    Comparator, ParallelTransformer
    {

        @Override
        public ParallelFlowable apply(ParallelFlowable upstream) {
            return null;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7, Object t8,
                Object t9) throws Exception {
            return null;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7, Object t8)
                throws Exception {
            return null;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7)
                throws Exception {
            return null;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6) throws Exception {
            return null;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5) throws Exception {
            return null;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4) throws Exception {
            return null;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3) throws Exception {
            return null;
        }

        @Override
        public void accept(Object t1, Object t2) throws Exception {
        }

        @Override
        public Object apply(Object t1, Object t2) throws Exception {
            return null;
        }

        @Override
        public void subscribe(CompletableEmitter e) throws Exception {
        }

        @Override
        public void subscribe(MaybeEmitter e) throws Exception {
        }

        @Override
        public void subscribe(SingleEmitter e) throws Exception {
        }

        @Override
        public void subscribe(ObservableEmitter e) throws Exception {
        }

        @Override
        public void subscribe(FlowableEmitter e) throws Exception {
        }

        @Override
        public boolean test(Object t1, Object t2) throws Exception {
            return false;
        }

        @Override
        public boolean test(Object t) throws Exception {
            return false;
        }

        @Override
        public void onSuccess(Object t) {
        }

        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Object t) {
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onComplete() {
        }

        @Override
        public CompletableSource apply(Completable upstream) {
            return upstream;
        }

        @Override
        public MaybeSource apply(Maybe upstream) {
            return upstream;
        }

        @Override
        public SingleSource apply(Single upstream) {
            return upstream;
        }

        @Override
        public ObservableSource apply(Observable upstream) {
            return upstream;
        }

        @Override
        public Publisher apply(Flowable upstream) {
            return upstream;
        }

        @Override
        public CompletableObserver apply(CompletableObserver observer) throws Exception {
            return observer;
        }

        @Override
        public MaybeObserver apply(MaybeObserver observer) throws Exception {
            return observer;
        }

        @Override
        public SingleObserver apply(SingleObserver observer) throws Exception {
            return observer;
        }

        @Override
        public Observer apply(Observer observer) throws Exception {
            return observer;
        }

        @Override
        public Subscriber apply(Subscriber observer) throws Exception {
            return observer;
        }

        @Override
        public boolean getAsBoolean() throws Exception {
            return false;
        }

        @Override
        public int compare(Object o1, Object o2) {
            return 0;
        }
    }

    enum ParamMode {
        ANY,
        NON_NEGATIVE
    }

    static final class ParamIgnore {
        final Class<?> clazz;
        final String name;
        final Class<?>[] arguments;

        ParamIgnore(Class<?> clazz, String name, Class<?>... arguments) {
            this.clazz = clazz;
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public String toString() {
            return clazz.getName() + " " + name;
        }
    }

    static final class ParamOverride {
        final Class<?> clazz;
        final int index;
        final ParamMode mode;
        final String name;
        final Class<?>[] arguments;

        ParamOverride(Class<?> clazz, int index, ParamMode mode, String name, Class<?>... arguments) {
            this.clazz = clazz;
            this.index = index;
            this.mode = mode;
            this.name = name;
            this.arguments = arguments;

            try {
                clazz.getMethod(name, arguments);
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        }

        @Override
        public String toString() {
            return clazz.getName() + " " + name;
        }
    }

    static final class NeverPublisher extends Flowable<Object> {

        @Override
        public void subscribeActual(Subscriber<? super Object> s) {
            // not invoked, the class is a placeholder default value
        }

        @Override
        public String toString() {
            return "NeverFlowable";
        }
    }

    static final class NeverObservable extends Observable<Object> {

        @Override
        public void subscribeActual(Observer<? super Object> s) {
            // not invoked, the class is a placeholder default value
        }

        @Override
        public String toString() {
            return "NeverFlowable";
        }
    }

    static final class NeverSingle extends Single<Object> {

        @Override
        public void subscribeActual(SingleObserver<? super Object> s) {
            // not invoked, the class is a placeholder default value
        }

        @Override
        public String toString() {
            return "NeverSingle";
        }
    }

    static final class NeverMaybe extends Maybe<Object> {

        @Override
        public void subscribeActual(MaybeObserver<? super Object> s) {
            // not invoked, the class is a placeholder default value
        }

        @Override
        public String toString() {
            return "NeverMaybe";
        }
    }
    static final class NeverCompletable extends Completable {

        @Override
        public void subscribeActual(CompletableObserver s) {
            // not invoked, the class is a placeholder default value
        }

        @Override
        public String toString() {
            return "NeverCompletable";
        }
    }
}
