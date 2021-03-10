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

package io.reactivex.rxjava3.plugins;

import static org.junit.Assert.*;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.annotations.NonNull;
import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.completable.CompletableError;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableRange;
import io.reactivex.rxjava3.internal.operators.maybe.MaybeError;
import io.reactivex.rxjava3.internal.operators.observable.ObservableRange;
import io.reactivex.rxjava3.internal.operators.parallel.ParallelFromPublisher;
import io.reactivex.rxjava3.internal.operators.single.SingleJust;
import io.reactivex.rxjava3.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.rxjava3.internal.subscriptions.ScalarSubscription;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class RxJavaPluginsTest extends RxJavaTest {

    @Test
    public void constructorShouldBePrivate() {
        TestHelper.checkUtilityClass(RxJavaPlugins.class);
    }

    @SuppressWarnings({ "rawtypes" })
    @Test
    public void lockdown() throws Exception {
        RxJavaPlugins.reset();
        RxJavaPlugins.lockdown();
        try {
            assertTrue(RxJavaPlugins.isLockdown());
            Consumer a1 = Functions.emptyConsumer();
            Supplier f0 = () -> null;
            Function f1 = Functions.identity();
            BiFunction f2 = (t1, t2) -> t2;

            BooleanSupplier bs = () -> true;

            for (Method m : RxJavaPlugins.class.getMethods()) {
                if (m.getName().startsWith("set")) {

                    Method getter;

                    Class<?> paramType = m.getParameterTypes()[0];

                    if (paramType == Boolean.TYPE) {
                        getter = RxJavaPlugins.class.getMethod("is" + m.getName().substring(3));
                    } else {
                        getter = RxJavaPlugins.class.getMethod("get" + m.getName().substring(3));
                    }

                    Object before = getter.invoke(null);

                    try {
                        if (paramType.isAssignableFrom(Boolean.TYPE)) {
                            m.invoke(null, true);
                        } else
                        if (paramType.isAssignableFrom(Supplier.class)) {
                            m.invoke(null, f0);
                        } else
                        if (paramType.isAssignableFrom(Function.class)) {
                            m.invoke(null, f1);
                        } else
                        if (paramType.isAssignableFrom(Consumer.class)) {
                            m.invoke(null, a1);
                        } else
                        if (paramType.isAssignableFrom(BooleanSupplier.class)) {
                            m.invoke(null, bs);
                        } else {
                            m.invoke(null, f2);
                        }
                        fail("Should have thrown InvocationTargetException(IllegalStateException)");
                    } catch (InvocationTargetException ex) {
                        if (ex.getCause() instanceof IllegalStateException) {
                            assertEquals("Plugins can't be changed anymore", ex.getCause().getMessage());
                        } else {
                            fail("Should have thrown InvocationTargetException(IllegalStateException)");
                        }
                    }

                    Object after = getter.invoke(null);

                    if (paramType.isPrimitive()) {
                        assertEquals(m.toString(), before, after);
                    } else {
                        assertSame(m.toString(), before, after);
                    }
                }
            }
        } finally {
            RxJavaPlugins.unlock();
            RxJavaPlugins.reset();
            assertFalse(RxJavaPlugins.isLockdown());
        }
    }

    Function<Scheduler, Scheduler> replaceWithImmediate = t -> ImmediateThinScheduler.INSTANCE;

    @Test
    public void overrideSingleScheduler() {
        try {
            RxJavaPlugins.setSingleSchedulerHandler(replaceWithImmediate);

            assertSame(ImmediateThinScheduler.INSTANCE, Schedulers.single());
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.single());
    }

    @Test
    public void overrideComputationScheduler() {
        try {
            RxJavaPlugins.setComputationSchedulerHandler(replaceWithImmediate);

            assertSame(ImmediateThinScheduler.INSTANCE, Schedulers.computation());
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.computation());
    }

    @Test
    public void overrideIoScheduler() {
        try {
            RxJavaPlugins.setIoSchedulerHandler(replaceWithImmediate);

            assertSame(ImmediateThinScheduler.INSTANCE, Schedulers.io());
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.io());
    }

    @Test
    public void overrideNewThreadScheduler() {
        try {
            RxJavaPlugins.setNewThreadSchedulerHandler(replaceWithImmediate);

            assertSame(ImmediateThinScheduler.INSTANCE, Schedulers.newThread());
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.newThread());
    }

    Function<Supplier<Scheduler>, Scheduler> initReplaceWithImmediate = t -> ImmediateThinScheduler.INSTANCE;

    @Test
    public void overrideInitSingleScheduler() {
        final Scheduler s = Schedulers.single(); // make sure the Schedulers is initialized
        Supplier<Scheduler> c = () -> s;
        try {
            RxJavaPlugins.setInitSingleSchedulerHandler(initReplaceWithImmediate);

            assertSame(ImmediateThinScheduler.INSTANCE, RxJavaPlugins.initSingleScheduler(c));
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertSame(s, RxJavaPlugins.initSingleScheduler(c));
    }

    @Test
    public void overrideInitComputationScheduler() {
        final Scheduler s = Schedulers.computation(); // make sure the Schedulers is initialized
        Supplier<Scheduler> c = () -> s;
        try {
            RxJavaPlugins.setInitComputationSchedulerHandler(initReplaceWithImmediate);

            assertSame(ImmediateThinScheduler.INSTANCE, RxJavaPlugins.initComputationScheduler(c));
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertSame(s, RxJavaPlugins.initComputationScheduler(c));
    }

    @Test
    public void overrideInitIoScheduler() {
        final Scheduler s = Schedulers.io(); // make sure the Schedulers is initialized;
        Supplier<Scheduler> c = () -> s;
        try {
            RxJavaPlugins.setInitIoSchedulerHandler(initReplaceWithImmediate);

            assertSame(ImmediateThinScheduler.INSTANCE, RxJavaPlugins.initIoScheduler(c));
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertSame(s, RxJavaPlugins.initIoScheduler(c));
    }

    @Test
    public void overrideInitNewThreadScheduler() {
        final Scheduler s = Schedulers.newThread(); // make sure the Schedulers is initialized;
        Supplier<Scheduler> c = () -> s;
        try {
            RxJavaPlugins.setInitNewThreadSchedulerHandler(initReplaceWithImmediate);

            assertSame(ImmediateThinScheduler.INSTANCE, RxJavaPlugins.initNewThreadScheduler(c));
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertSame(s, RxJavaPlugins.initNewThreadScheduler(c));
    }

    Supplier<Scheduler> nullResultSupplier = () -> null;

    @Test
    public void overrideInitSingleSchedulerCrashes() {
        // fail when Supplier is null
        try {
            RxJavaPlugins.initSingleScheduler(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException npe) {
            assertEquals("Scheduler Supplier can't be null", npe.getMessage());
        }

        // fail when Supplier result is null
        try {
            RxJavaPlugins.initSingleScheduler(nullResultSupplier);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException npe) {
            assertEquals("Scheduler Supplier result can't be null", npe.getMessage());
        }
    }

    @Test
    public void overrideInitComputationSchedulerCrashes() {
        // fail when Supplier is null
        try {
            RxJavaPlugins.initComputationScheduler(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException npe) {
            assertEquals("Scheduler Supplier can't be null", npe.getMessage());
        }

        // fail when Supplier result is null
        try {
            RxJavaPlugins.initComputationScheduler(nullResultSupplier);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException npe) {
            assertEquals("Scheduler Supplier result can't be null", npe.getMessage());
        }
    }

    @Test
    public void overrideInitIoSchedulerCrashes() {
        // fail when Supplier is null
        try {
            RxJavaPlugins.initIoScheduler(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException npe) {
            assertEquals("Scheduler Supplier can't be null", npe.getMessage());
        }

        // fail when Supplier result is null
        try {
            RxJavaPlugins.initIoScheduler(nullResultSupplier);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException npe) {
            assertEquals("Scheduler Supplier result can't be null", npe.getMessage());
        }
    }

    @Test
    public void overrideInitNewThreadSchedulerCrashes() {
        // fail when Supplier is null
        try {
            RxJavaPlugins.initNewThreadScheduler(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException npe) {
            // expected
            assertEquals("Scheduler Supplier can't be null", npe.getMessage());
        }

        // fail when Supplier result is null
        try {
            RxJavaPlugins.initNewThreadScheduler(nullResultSupplier);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException npe) {
            assertEquals("Scheduler Supplier result can't be null", npe.getMessage());
        }
    }

    Supplier<Scheduler> unsafeDefault = () -> {
        throw new AssertionError("Default Scheduler instance should not have been evaluated");
    };

    @Test
    public void defaultSingleSchedulerIsInitializedLazily() {
        // unsafe default Scheduler Supplier should not be evaluated
        try {
            RxJavaPlugins.setInitSingleSchedulerHandler(initReplaceWithImmediate);
            RxJavaPlugins.initSingleScheduler(unsafeDefault);
        } finally {
            RxJavaPlugins.reset();
        }

        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.single());
    }

    @Test
    public void defaultIoSchedulerIsInitializedLazily() {
        // unsafe default Scheduler Supplier should not be evaluated
        try {
            RxJavaPlugins.setInitIoSchedulerHandler(initReplaceWithImmediate);
            RxJavaPlugins.initIoScheduler(unsafeDefault);
        } finally {
            RxJavaPlugins.reset();
        }

        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.io());
    }

    @Test
    public void defaultComputationSchedulerIsInitializedLazily() {
        // unsafe default Scheduler Supplier should not be evaluated
        try {
            RxJavaPlugins.setInitComputationSchedulerHandler(initReplaceWithImmediate);
            RxJavaPlugins.initComputationScheduler(unsafeDefault);
        } finally {
            RxJavaPlugins.reset();
        }

        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.computation());
    }

    @Test
    public void defaultNewThreadSchedulerIsInitializedLazily() {
        // unsafe default Scheduler Supplier should not be evaluated
        try {
            RxJavaPlugins.setInitNewThreadSchedulerHandler(initReplaceWithImmediate);
            RxJavaPlugins.initNewThreadScheduler(unsafeDefault);
        } finally {
            RxJavaPlugins.reset();
        }

        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.newThread());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void observableCreate() {
        try {
            RxJavaPlugins.setOnObservableAssembly(t -> new ObservableRange(1, 2));

            Observable.range(10, 3)
            .test()
            .assertValues(1, 2)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Observable.range(10, 3)
        .test()
        .assertValues(10, 11, 12)
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void flowableCreate() {
        try {
            RxJavaPlugins.setOnFlowableAssembly(t -> new FlowableRange(1, 2));

            Flowable.range(10, 3)
            .test()
            .assertValues(1, 2)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Flowable.range(10, 3)
        .test()
        .assertValues(10, 11, 12)
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void observableStart() {
        try {
            RxJavaPlugins.setOnObservableSubscribe((o, t) -> new Observer() {

                @Override
                public void onSubscribe(@NonNull Disposable d) {
                    t.onSubscribe(d);
                }

                @SuppressWarnings("unchecked")
                @Override
                public void onNext(@NonNull Object value) {
                    t.onNext((Integer)value - 9);
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    t.onError(e);
                }

                @Override
                public void onComplete() {
                    t.onComplete();
                }

            });

            Observable.range(10, 3)
            .test()
            .assertValues(1, 2, 3)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Observable.range(10, 3)
        .test()
        .assertValues(10, 11, 12)
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void flowableStart() {
        try {
            RxJavaPlugins.setOnFlowableSubscribe((f, t) -> new Subscriber() {

                @Override
                public void onSubscribe(Subscription s) {
                    t.onSubscribe(s);
                }

                @SuppressWarnings("unchecked")
                @Override
                public void onNext(Object value) {
                    t.onNext((Integer)value - 9);
                }

                @Override
                public void onError(Throwable e) {
                    t.onError(e);
                }

                @Override
                public void onComplete() {
                    t.onComplete();
                }

            });

            Flowable.range(10, 3)
            .test()
            .assertValues(1, 2, 3)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Flowable.range(10, 3)
        .test()
        .assertValues(10, 11, 12)
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void parallelFlowableStart() {
        try {
            RxJavaPlugins.setOnParallelSubscribe((f, t) -> new Subscriber[] { new Subscriber() {

                    @Override
                    public void onSubscribe(Subscription s) {
                        t[0].onSubscribe(s);
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public void onNext(Object value) {
                        t[0].onNext((Integer)value - 9);
                    }

                    @Override
                    public void onError(Throwable e) {
                        t[0].onError(e);
                    }

                    @Override
                    public void onComplete() {
                        t[0].onComplete();
                    }

                }
            });

            Flowable.range(10, 3)
            .parallel(1)
            .sequential()
            .test()
            .assertValues(1, 2, 3)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Flowable.range(10, 3)
        .parallel(1)
        .sequential()
        .test()
        .assertValues(10, 11, 12)
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void singleCreate() {
        try {
            RxJavaPlugins.setOnSingleAssembly(t -> new SingleJust<>(10));

            Single.just(1)
            .test()
            .assertValue(10)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Single.just(1)
        .test()
        .assertValue(1)
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void singleStart() {
        try {
            RxJavaPlugins.setOnSingleSubscribe((o, t) -> new SingleObserver<Object>() {

                @Override
                public void onSubscribe(@NonNull Disposable d) {
                    t.onSubscribe(d);
                }

                @SuppressWarnings("unchecked")
                @Override
                public void onSuccess(@NonNull Object value) {
                    t.onSuccess(10);
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    t.onError(e);
                }

            });

            Single.just(1)
            .test()
            .assertValue(10)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Single.just(1)
        .test()
        .assertValue(1)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void completableCreate() {
        try {
            RxJavaPlugins.setOnCompletableAssembly(t -> new CompletableError(new TestException()));

            Completable.complete()
            .test()
            .assertNoValues()
            .assertNotComplete()
            .assertError(TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Completable.complete()
        .test()
        .assertNoValues()
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void completableStart() {
        try {
            RxJavaPlugins.setOnCompletableSubscribe((o, t) -> new CompletableObserver() {
                @Override
                public void onSubscribe(@NonNull Disposable d) {
                    t.onSubscribe(d);
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    t.onError(e);
                }

                @Override
                public void onComplete() {
                    t.onError(new TestException());
                }
            });

            Completable.complete()
            .test()
            .assertNoValues()
            .assertNotComplete()
            .assertError(TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked

        Completable.complete()
        .test()
        .assertNoValues()
        .assertNoErrors()
        .assertComplete();
    }

    void onSchedule(Worker w) throws InterruptedException {
        try {
            try {
                final AtomicInteger value = new AtomicInteger();
                final CountDownLatch cdl = new CountDownLatch(1);

                RxJavaPlugins.setScheduleHandler(t -> () -> {
                    value.set(10);
                    cdl.countDown();
                });

                w.schedule(() -> {
                    value.set(1);
                    cdl.countDown();
                });

                cdl.await();

                assertEquals(10, value.get());

            } finally {

                RxJavaPlugins.reset();
            }

            // make sure the reset worked
            final AtomicInteger value = new AtomicInteger();
            final CountDownLatch cdl = new CountDownLatch(1);

            w.schedule(() -> {
                value.set(1);
                cdl.countDown();
            });

            cdl.await();

            assertEquals(1, value.get());
        } finally {
            w.dispose();
        }
    }

    @Test
    public void onScheduleComputation() throws InterruptedException {
        onSchedule(Schedulers.computation().createWorker());
    }

    @Test
    public void onScheduleIO() throws InterruptedException {
        onSchedule(Schedulers.io().createWorker());
    }

    @Test
    public void onScheduleNewThread() throws InterruptedException {
        onSchedule(Schedulers.newThread().createWorker());
    }

    @Test
    public void onError() {
        try {
            final List<Throwable> list = new ArrayList<>();

            RxJavaPlugins.setErrorHandler(list::add);

            RxJavaPlugins.onError(new TestException("Forced failure"));

            assertEquals(1, list.size());
            assertUndeliverableTestException(list, 0, "Forced failure");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorNoHandler() {
        try {
            final List<Throwable> list = new ArrayList<>();

            RxJavaPlugins.setErrorHandler(null);

            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> list.add(e));

            RxJavaPlugins.onError(new TestException("Forced failure"));

            Thread.currentThread().setUncaughtExceptionHandler(null);

            // this will be printed on the console and should not crash
            RxJavaPlugins.onError(new TestException("Forced failure 3"));

            assertEquals(1, list.size());
            assertUndeliverableTestException(list, 0, "Forced failure");
        } finally {
            RxJavaPlugins.reset();
            Thread.currentThread().setUncaughtExceptionHandler(null);
        }
    }

    @Test
    public void onErrorCrashes() {
        try {
            final List<Throwable> list = new ArrayList<>();

            RxJavaPlugins.setErrorHandler(t -> {
                throw new TestException("Forced failure 2");
            });

            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> list.add(e));

            RxJavaPlugins.onError(new TestException("Forced failure"));

            assertEquals(2, list.size());
            assertTestException(list, 0, "Forced failure 2");
            assertUndeliverableTestException(list, 1, "Forced failure");

            Thread.currentThread().setUncaughtExceptionHandler(null);

        } finally {
            RxJavaPlugins.reset();
            Thread.currentThread().setUncaughtExceptionHandler(null);
        }
    }

    @Test
    public void onErrorWithNull() {
        try {
            final List<Throwable> list = new ArrayList<>();

            RxJavaPlugins.setErrorHandler(t -> {
                throw new TestException("Forced failure 2");
            });

            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> list.add(e));

            RxJavaPlugins.onError(null);

            assertEquals(2, list.size());
            assertTestException(list, 0, "Forced failure 2");
            assertNPE(list, 1);

            RxJavaPlugins.reset();

            RxJavaPlugins.onError(null);

            assertNPE(list, 2);

        } finally {
            RxJavaPlugins.reset();

            Thread.currentThread().setUncaughtExceptionHandler(null);
        }
    }

    /**
     * Ensure set*() accepts a consumers/functions with wider bounds.
     * @throws Exception on error
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void onErrorWithSuper() throws Exception {
        try {
            Consumer<? super Throwable> errorHandler = (Consumer<Throwable>) t -> {
                throw new TestException("Forced failure 2");
            };
            RxJavaPlugins.setErrorHandler(errorHandler);

            Consumer<? super Throwable> errorHandler1 = RxJavaPlugins.getErrorHandler();
            assertSame(errorHandler, errorHandler1);

            Function<? super Scheduler, ? extends Scheduler> scheduler2scheduler = (Function<Scheduler, Scheduler>) scheduler -> scheduler;
            Function<? super Supplier<Scheduler>, ? extends Scheduler> callable2scheduler = (Function<Supplier<Scheduler>, Scheduler>) Supplier::get;
            Function<? super ConnectableFlowable, ? extends ConnectableFlowable> connectableFlowable2ConnectableFlowable = (Function<ConnectableFlowable, ConnectableFlowable>) connectableFlowable -> connectableFlowable;
            Function<? super ConnectableObservable, ? extends ConnectableObservable> connectableObservable2ConnectableObservable = (Function<ConnectableObservable, ConnectableObservable>) connectableObservable -> connectableObservable;
            Function<? super Flowable, ? extends Flowable> flowable2Flowable = (Function<Flowable, Flowable>) flowable -> flowable;
            BiFunction<? super Flowable, ? super Subscriber, ? extends Subscriber> flowable2subscriber = (BiFunction<Flowable, Subscriber, Subscriber>) (flowable, subscriber) -> subscriber;
            Function<Maybe, Maybe> maybe2maybe = maybe -> maybe;
            BiFunction<Maybe, MaybeObserver, MaybeObserver> maybe2observer = (maybe, maybeObserver) -> maybeObserver;
            Function<Observable, Observable> observable2observable = observable -> observable;
            BiFunction<? super Observable, ? super Observer, ? extends Observer> observable2observer = (BiFunction<Observable, Observer, Observer>) (observable, observer) -> observer;
            Function<? super ParallelFlowable, ? extends ParallelFlowable> parallelFlowable2parallelFlowable = (Function<ParallelFlowable, ParallelFlowable>) parallelFlowable -> parallelFlowable;
            Function<Single, Single> single2single = single -> single;
            BiFunction<? super Single, ? super SingleObserver, ? extends SingleObserver> single2observer = (BiFunction<Single, SingleObserver, SingleObserver>) (single, singleObserver) -> singleObserver;
            Function<? super Runnable, ? extends Runnable> runnable2runnable = (Function<Runnable, Runnable>) runnable -> runnable;
            BiFunction<? super Completable, ? super CompletableObserver, ? extends CompletableObserver> completableObserver2completableObserver = (BiFunction<Completable, CompletableObserver, CompletableObserver>) (completable, completableObserver) -> completableObserver;
            Function<? super Completable, ? extends Completable> completable2completable = (Function<Completable, Completable>) completable -> completable;

            RxJavaPlugins.setInitComputationSchedulerHandler(callable2scheduler);
            RxJavaPlugins.setComputationSchedulerHandler(scheduler2scheduler);
            RxJavaPlugins.setIoSchedulerHandler(scheduler2scheduler);
            RxJavaPlugins.setNewThreadSchedulerHandler(scheduler2scheduler);
            RxJavaPlugins.setOnConnectableFlowableAssembly(connectableFlowable2ConnectableFlowable);
            RxJavaPlugins.setOnConnectableObservableAssembly(connectableObservable2ConnectableObservable);
            RxJavaPlugins.setOnFlowableAssembly(flowable2Flowable);
            RxJavaPlugins.setOnFlowableSubscribe(flowable2subscriber);
            RxJavaPlugins.setOnMaybeAssembly(maybe2maybe);
            RxJavaPlugins.setOnMaybeSubscribe(maybe2observer);
            RxJavaPlugins.setOnObservableAssembly(observable2observable);
            RxJavaPlugins.setOnObservableSubscribe(observable2observer);
            RxJavaPlugins.setOnParallelAssembly(parallelFlowable2parallelFlowable);
            RxJavaPlugins.setOnSingleAssembly(single2single);
            RxJavaPlugins.setOnSingleSubscribe(single2observer);
            RxJavaPlugins.setScheduleHandler(runnable2runnable);
            RxJavaPlugins.setSingleSchedulerHandler(scheduler2scheduler);
            RxJavaPlugins.setOnCompletableSubscribe(completableObserver2completableObserver);
            RxJavaPlugins.setOnCompletableAssembly(completable2completable);
            RxJavaPlugins.setInitSingleSchedulerHandler(callable2scheduler);
            RxJavaPlugins.setInitNewThreadSchedulerHandler(callable2scheduler);
            RxJavaPlugins.setInitIoSchedulerHandler(callable2scheduler);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked" })
    @Test
    public void clearIsPassthrough() {
        try {
            RxJavaPlugins.reset();

            assertNull(RxJavaPlugins.onAssembly((Observable)null));

            assertNull(RxJavaPlugins.onAssembly((ConnectableObservable)null));

            assertNull(RxJavaPlugins.onAssembly((Flowable)null));

            assertNull(RxJavaPlugins.onAssembly((ConnectableFlowable)null));

            Observable oos = new Observable() {
                @Override
                public void subscribeActual(@NonNull Observer t) {

                }
            };

            Flowable fos = new Flowable() {
                @Override
                public void subscribeActual(@NonNull Subscriber t) {

                }
            };

            assertSame(oos, RxJavaPlugins.onAssembly(oos));

            assertSame(fos, RxJavaPlugins.onAssembly(fos));

            assertNull(RxJavaPlugins.onAssembly((Single)null));

            Single sos = new Single() {
                @Override
                public void subscribeActual(@NonNull SingleObserver t) {

                }
            };

            assertSame(sos, RxJavaPlugins.onAssembly(sos));

            assertNull(RxJavaPlugins.onAssembly((Completable)null));

            Completable cos = new Completable() {
                @Override
                public void subscribeActual(@NonNull CompletableObserver t) {

                }
            };

            assertSame(cos, RxJavaPlugins.onAssembly(cos));

            assertNull(RxJavaPlugins.onAssembly((Maybe)null));

            Maybe myb = new Maybe() {
                @Override
                public void subscribeActual(@NonNull MaybeObserver t) {

                }
            };

            assertSame(myb, RxJavaPlugins.onAssembly(myb));

            Runnable action = Functions.EMPTY_RUNNABLE;
            assertSame(action, RxJavaPlugins.onSchedule(action));

            class AllSubscriber implements Subscriber, Observer, SingleObserver, CompletableObserver, MaybeObserver {

                @Override
                public void onSuccess(@NonNull Object value) {

                }

                @Override
                public void onSubscribe(@NonNull Disposable d) {

                }

                @Override
                public void onSubscribe(Subscription s) {

                }

                @Override
                public void onNext(@NonNull Object t) {

                }

                @Override
                public void onError(@NonNull Throwable t) {

                }

                @Override
                public void onComplete() {

                }

            }

            AllSubscriber all = new AllSubscriber();
            Subscriber[] allArray = { all };

            assertNull(RxJavaPlugins.onSubscribe(Observable.never(), null));

            assertSame(all, RxJavaPlugins.onSubscribe(Observable.never(), all));

            assertNull(RxJavaPlugins.onSubscribe(Flowable.never(), null));

            assertSame(all, RxJavaPlugins.onSubscribe(Flowable.never(), all));

            assertNull(RxJavaPlugins.onSubscribe(Single.just(1), null));

            assertSame(all, RxJavaPlugins.onSubscribe(Single.just(1), all));

            assertNull(RxJavaPlugins.onSubscribe(Completable.never(), null));

            assertSame(all, RxJavaPlugins.onSubscribe(Completable.never(), all));

            assertNull(RxJavaPlugins.onSubscribe(Maybe.never(), null));

            assertSame(all, RxJavaPlugins.onSubscribe(Maybe.never(), all));

            assertNull(RxJavaPlugins.onSubscribe(Flowable.never().parallel(), null));

            assertSame(allArray, RxJavaPlugins.onSubscribe(Flowable.never().parallel(), allArray));

            final Scheduler s = ImmediateThinScheduler.INSTANCE;
            Supplier<Scheduler> c = () -> s;
            assertSame(s, RxJavaPlugins.onComputationScheduler(s));

            assertSame(s, RxJavaPlugins.onIoScheduler(s));

            assertSame(s, RxJavaPlugins.onNewThreadScheduler(s));

            assertSame(s, RxJavaPlugins.onSingleScheduler(s));

            assertSame(s, RxJavaPlugins.initComputationScheduler(c));

            assertSame(s, RxJavaPlugins.initIoScheduler(c));

            assertSame(s, RxJavaPlugins.initNewThreadScheduler(c));

            assertSame(s, RxJavaPlugins.initSingleScheduler(c));

        } finally {
            RxJavaPlugins.reset();
        }
    }

    static void assertTestException(List<Throwable> list, int index, String message) {
        assertTrue(list.get(index).toString(), list.get(index) instanceof TestException);
        assertEquals(message, list.get(index).getMessage());
    }

    static void assertUndeliverableTestException(List<Throwable> list, int index, String message) {
        assertTrue(list.get(index).toString(), list.get(index).getCause() instanceof TestException);
        assertEquals(message, list.get(index).getCause().getMessage());
    }

    static void assertNPE(List<Throwable> list, int index) {
        assertTrue(list.get(index).toString(), list.get(index) instanceof NullPointerException);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void overrideConnectableObservable() {
        try {
            RxJavaPlugins.setOnConnectableObservableAssembly(co -> new ConnectableObservable() {

                @Override
                public void connect(@NonNull Consumer connection) {

                }

                @Override
                public void reset() {
                    // nothing to do in this test
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void subscribeActual(@NonNull Observer observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(10);
                    observer.onComplete();
                }
            });

            Observable
            .just(1)
            .publish()
            .autoConnect()
            .test()
            .assertResult(10);

        } finally {
            RxJavaPlugins.reset();
        }

        Observable
        .just(1)
        .publish()
        .autoConnect()
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void overrideConnectableFlowable() {
        try {
            RxJavaPlugins.setOnConnectableFlowableAssembly(co -> new ConnectableFlowable() {

                @Override
                public void connect(@NonNull Consumer connection) {

                }

                @Override
                public void reset() {
                    // nothing to do in this test
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void subscribeActual(@NonNull Subscriber subscriber) {
                    subscriber.onSubscribe(new ScalarSubscription(subscriber, 10));
                }
            });

            Flowable
            .just(1)
            .publish()
            .autoConnect()
            .test()
            .assertResult(10);

        } finally {
            RxJavaPlugins.reset();
        }

        Flowable
        .just(1)
        .publish()
        .autoConnect()
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void assemblyHookCrashes() {
        try {
            RxJavaPlugins.setOnFlowableAssembly(f -> {
                throw new IllegalArgumentException();
            });

            try {
                Flowable.empty();
                fail("Should have thrown!");
            } catch (IllegalArgumentException ex) {
                // expected
            }

            RxJavaPlugins.setOnFlowableAssembly(f -> {
                throw new InternalError();
            });

            try {
                Flowable.empty();
                fail("Should have thrown!");
            } catch (InternalError ex) {
                // expected
            }

            RxJavaPlugins.setOnFlowableAssembly(f -> {
                throw new IOException();
            });

            try {
                Flowable.empty();
                fail("Should have thrown!");
            } catch (RuntimeException ex) {
                if (!(ex.getCause() instanceof IOException)) {
                    fail(ex.getCause().toString() + ": Should have thrown RuntimeException(IOException)");
                }
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void subscribeHookCrashes() {
        try {
            RxJavaPlugins.setOnFlowableSubscribe((f, s) -> {
                throw new IllegalArgumentException();
            });

            try {
                Flowable.empty().test();
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                if (!(ex.getCause() instanceof IllegalArgumentException)) {
                    fail(ex.getCause().toString() + ": Should have thrown NullPointerException(IllegalArgumentException)");
                }
            }

            RxJavaPlugins.setOnFlowableSubscribe((f, s) -> {
                throw new InternalError();
            });

            try {
                Flowable.empty().test();
                fail("Should have thrown!");
            } catch (InternalError ex) {
                // expected
            }

            RxJavaPlugins.setOnFlowableSubscribe((f, s) -> {
                throw new IOException();
            });

            try {
                Flowable.empty().test();
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                if (!(ex.getCause() instanceof RuntimeException)) {
                    fail(ex.getCause().toString() + ": Should have thrown NullPointerException(RuntimeException(IOException))");
                }
                if (!(ex.getCause().getCause() instanceof IOException)) {
                    fail(ex.getCause().toString() + ": Should have thrown NullPointerException(RuntimeException(IOException))");
                }
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void maybeCreate() {
        try {
            RxJavaPlugins.setOnMaybeAssembly(t -> new MaybeError(new TestException()));

            Maybe.empty()
            .test()
            .assertNoValues()
            .assertNotComplete()
            .assertError(TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Maybe.empty()
        .test()
        .assertNoValues()
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void maybeStart() {
        try {
            RxJavaPlugins.setOnMaybeSubscribe((o, t) -> new MaybeObserver() {
                @Override
                public void onSubscribe(@NonNull Disposable d) {
                    t.onSubscribe(d);
                }

                @SuppressWarnings("unchecked")
                @Override
                public void onSuccess(@NonNull Object value) {
                    t.onSuccess(value);
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    t.onError(e);
                }

                @Override
                public void onComplete() {
                    t.onError(new TestException());
                }
            });

            Maybe.empty()
            .test()
            .assertNoValues()
            .assertNotComplete()
            .assertError(TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked

        Maybe.empty()
        .test()
        .assertNoValues()
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void onErrorNull() {
        try {
            final AtomicReference<Throwable> t = new AtomicReference<>();

            RxJavaPlugins.setErrorHandler(t::set);

            RxJavaPlugins.onError(null);

            final Throwable throwable = t.get();
            assertEquals(ExceptionHelper.nullWarning("onError called with a null Throwable."), throwable.getMessage());
            assertTrue(throwable instanceof NullPointerException);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    private static void verifyThread(Scheduler scheduler, String expectedThreadName)
            throws AssertionError {
        assertNotNull(scheduler);
        Worker w = scheduler.createWorker();
        try {
            final AtomicReference<Thread> value = new AtomicReference<>();
            final CountDownLatch cdl = new CountDownLatch(1);

            w.schedule(() -> {
                value.set(Thread.currentThread());
                cdl.countDown();
            });

            cdl.await();

            Thread t = value.get();
            assertNotNull(t);
            assertEquals(expectedThreadName, t.getName());
        } catch (Exception e) {
            fail();
        } finally {
            w.dispose();
        }
    }

    @Test
    public void createComputationScheduler() {
        final String name = "ComputationSchedulerTest";
        ThreadFactory factory = r -> new Thread(r, name);

        final Scheduler customScheduler = RxJavaPlugins.createComputationScheduler(factory);
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> customScheduler);

        try {
            verifyThread(Schedulers.computation(), name);
        } finally {
            customScheduler.shutdown();
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void createIoScheduler() {
        final String name = "IoSchedulerTest";
        ThreadFactory factory = r -> new Thread(r, name);

        final Scheduler customScheduler = RxJavaPlugins.createIoScheduler(factory);
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> customScheduler);

        try {
            verifyThread(Schedulers.io(), name);
        } finally {
            customScheduler.shutdown();
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void createNewThreadScheduler() {
        final String name = "NewThreadSchedulerTest";
        ThreadFactory factory = r -> new Thread(r, name);

        final Scheduler customScheduler = RxJavaPlugins.createNewThreadScheduler(factory);
        RxJavaPlugins.setNewThreadSchedulerHandler(scheduler -> customScheduler);

        try {
            verifyThread(Schedulers.newThread(), name);
        } finally {
            customScheduler.shutdown();
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void createSingleScheduler() {
        final String name = "SingleSchedulerTest";
        ThreadFactory factory = r -> new Thread(r, name);

        final Scheduler customScheduler = RxJavaPlugins.createSingleScheduler(factory);

        RxJavaPlugins.setSingleSchedulerHandler(scheduler -> customScheduler);

        try {
            verifyThread(Schedulers.single(), name);
        } finally {
            customScheduler.shutdown();
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onBeforeBlocking() {
        try {
            RxJavaPlugins.setOnBeforeBlocking(() -> {
                throw new IllegalArgumentException();
            });

            try {
                RxJavaPlugins.onBeforeBlocking();
                fail("Should have thrown");
            } catch (IllegalArgumentException ex) {
                // expected
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void onParallelAssembly() {
        try {
            RxJavaPlugins.setOnParallelAssembly(pf -> new ParallelFromPublisher<>(Flowable.just(2), 2, 2));

            Flowable.just(1)
            .parallel()
            .sequential()
            .test()
            .assertResult(2);
        } finally {
            RxJavaPlugins.reset();
        }

        Flowable.just(1)
        .parallel()
        .sequential()
        .test()
        .assertResult(1);
    }

    @Test
    public void isBug() {
        assertFalse(RxJavaPlugins.isBug(new RuntimeException()));
        assertFalse(RxJavaPlugins.isBug(new IOException()));
        assertFalse(RxJavaPlugins.isBug(new InterruptedException()));
        assertFalse(RxJavaPlugins.isBug(new InterruptedIOException()));

        assertTrue(RxJavaPlugins.isBug(new NullPointerException()));
        assertTrue(RxJavaPlugins.isBug(new IllegalArgumentException()));
        assertTrue(RxJavaPlugins.isBug(new IllegalStateException()));
        assertTrue(RxJavaPlugins.isBug(new MissingBackpressureException()));
        assertTrue(RxJavaPlugins.isBug(new ProtocolViolationException("")));
        assertTrue(RxJavaPlugins.isBug(new UndeliverableException(new TestException())));
        assertTrue(RxJavaPlugins.isBug(new CompositeException(new TestException())));
        assertTrue(RxJavaPlugins.isBug(new OnErrorNotImplementedException(new TestException())));
    }
}
