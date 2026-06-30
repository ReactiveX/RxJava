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

package io.reactivex.rxjava4.internal.operators.observable;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.core.Observer;
import io.reactivex.rxjava4.core.config.StandardBufferedConfig;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.rxjava4.observers.*;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.subjects.*;
import io.reactivex.rxjava4.testsupport.*;

public class ObservableConcatMapSchedulerTest {

    @Test
    public void boundaryFusion() {
        Observable.range(1, 10000)
        .observeOn(Schedulers.single())
        .map(_ -> {
            String name = Thread.currentThread().getName();
            if (name.contains("RxSingleScheduler")) {
                return "RxSingleScheduler";
            }
            return name;
        })
        .concatMap((Function<String, ObservableSource<? extends Object>>) Observable::just, ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .observeOn(Schedulers.computation())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("RxSingleScheduler");
    }

    @Test
    public void boundaryFusionDelayError() {
        Observable.range(1, 10000)
        .observeOn(Schedulers.single())
        .map(_ -> {
            String name = Thread.currentThread().getName();
            if (name.contains("RxSingleScheduler")) {
                return "RxSingleScheduler";
            }
            return name;
        })
        .concatMap((Function<String, ObservableSource<? extends Object>>) Observable::just, ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .observeOn(Schedulers.computation())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("RxSingleScheduler");
    }

    @Test
    public void pollThrows() {
        Observable.just(1)
        .map((Function<Integer, Integer>) _ -> {
            throw new TestException();
        })
        .compose(TestHelper.<Integer>observableStripBoundary())
        .concatMap((Function<Integer, ObservableSource<Integer>>) Observable::just, ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void pollThrowsDelayError() {
        Observable.just(1)
        .map((Function<Integer, Integer>) _ -> {
            throw new TestException();
        })
        .compose(TestHelper.<Integer>observableStripBoundary())
        .concatMap((Function<Integer, ObservableSource<Integer>>) Observable::just, ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void noCancelPrevious() {
        final AtomicInteger counter = new AtomicInteger();

        Observable.range(1, 5)
        .concatMap((Function<Integer, Observable<Integer>>) v ->
            Observable.just(v).doOnDispose(counter::getAndIncrement), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(0, counter.get());
    }

    @Test
    public void delayErrorCallableTillTheEnd() {
        Observable.just(1, 2, 3, 101, 102, 23, 890, 120, 32)
        .concatMap((Function<Integer, Observable<Integer>>) integer -> Observable.fromCallable(() -> {
            if (integer >= 100) {
                throw new NullPointerException("test null exp");
            }
            return integer;
        }), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .test()
        .assertFailure(CompositeException.class, 1, 2, 3, 23, 32);
    }

    @Test
    public void delayErrorCallableEager() {
        Observable.just(1, 2, 3, 101, 102, 23, 890, 120, 32)
        .concatMap((Function<Integer, Observable<Integer>>) integer -> Observable.fromCallable(() -> {
            if (integer >= 100) {
                throw new NullPointerException("test null exp");
            }
            return integer;
        }), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS_BOUNDARY)
        .test()
        .assertFailure(NullPointerException.class, 1, 2, 3);
    }

    @Test
    public void mapperScheduled() {
        TestObserver<String> to = Observable.just(1)
        .concatMap((Function<Integer, Observable<String>>) _ -> Observable.just(Thread.currentThread().getName()), Schedulers.single(), StandardBufferedConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().getFirst().startsWith("RxSingleScheduler-"), to.values().toString());
    }

    @Test
    public void mapperScheduledHidden() {
        TestObserver<String> to = Observable.just(1)
        .concatMap((Function<Integer, Observable<String>>) _ ->
            Observable.just(Thread.currentThread().getName()).hide(), Schedulers.single(), StandardBufferedConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().getFirst().startsWith("RxSingleScheduler-"), to.values().toString());
    }

    @Test
    public void mapperDelayErrorScheduled() {
        TestObserver<String> to = Observable.just(1)
        .concatMap((Function<Integer, Observable<String>>) _ ->
            Observable.just(Thread.currentThread().getName()), Schedulers.single(), StandardBufferedConfig.DELAY_ERRORS_BOUNDARY)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().getFirst().startsWith("RxSingleScheduler-"), to.values().toString());
    }

    @Test
    public void mapperDelayErrorScheduledHidden() {
        TestObserver<String> to = Observable.just(1)
        .concatMap((Function<Integer, Observable<String>>) _ ->
            Observable.just(Thread.currentThread().getName()).hide(), Schedulers.single(), StandardBufferedConfig.DELAY_ERRORS_BOUNDARY)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().getFirst().startsWith("RxSingleScheduler-"), to.values().toString());
    }

    @Test
    public void mapperDelayError2Scheduled() {
        TestObserver<String> to = Observable.just(1)
        .concatMap((Function<Integer, Observable<String>>) _ -> Observable.just(Thread.currentThread().getName()), Schedulers.single(), StandardBufferedConfig.DELAY_ERRORS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().getFirst().startsWith("RxSingleScheduler-"), to.values().toString());
    }

    @Test
    public void mapperDelayError2ScheduledHidden() {
        TestObserver<String> to = Observable.just(1)
        .concatMap((Function<Integer, Observable<String>>) _ ->
            Observable.just(Thread.currentThread().getName()).hide(), Schedulers.single(), StandardBufferedConfig.DELAY_ERRORS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().getFirst().startsWith("RxSingleScheduler-"), to.values().toString());
    }

    @Test
    public void issue2890NoStackoverflow() throws InterruptedException, TimeoutException {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final Scheduler sch = Schedulers.from(executor);

        Function<Integer, Observable<Integer>> func = t -> {
            Observable<Integer> flowable = Observable.just(t)
                    .subscribeOn(sch)
            ;
            Subject<Integer> processor = UnicastSubject.create();
            flowable.subscribe(processor);
            return processor;
        };

        int n = 5000;
        final AtomicInteger counter = new AtomicInteger();

        Observable.range(1, n)
        .concatMap(func, ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .subscribe(new DefaultObserver<>() /* NFI */ {
            @Override
            public void onNext(Integer t) {
                // Consume after sleep for 1 ms
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // ignored
                }
                if (counter.getAndIncrement() % 100 == 0) {
                    System.out.print("testIssue2890NoStackoverflow -> ");
                    System.out.println(counter.get());
                }
            }

            @Override
            public void onComplete() {
                executor.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                executor.shutdown();
            }
        });

        long awaitTerminationTimeout = 100_000;
        if (!executor.awaitTermination(awaitTerminationTimeout, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Completed " + counter.get() + "/" + n + " before timed out after "
                + awaitTerminationTimeout + " milliseconds.");
        }

        assertEquals(n, counter.get());
    }

    @Test
    public void concatMapRangeAsyncLoopIssue2876() {
        final long durationSeconds = 2;
        final long startTime = System.currentTimeMillis();
        for (int i = 0;; i++) {
            //only run this for a max of ten seconds
            if (System.currentTimeMillis() - startTime > TimeUnit.SECONDS.toMillis(durationSeconds)) {
                return;
            }
            if (i % 1000 == 0) {
                System.out.println("concatMapRangeAsyncLoop > " + i);
            }
            TestObserverEx<Integer> to = new TestObserverEx<>();
            Observable.range(0, 1000)
            .concatMap((Function<Integer, Observable<Integer>>) t ->
                Observable.fromIterable(Collections.singletonList(t)), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
            .observeOn(Schedulers.computation()).subscribe(to);

            to.awaitDone(2500, TimeUnit.MILLISECONDS);
            to.assertTerminated();
            to.assertNoErrors();
            assertEquals(1000, to.values().size());
            assertEquals((Integer)999, to.values().get(999));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArray() throws Exception {
        for (int i = 2; i < 10; i++) {
            Observable<Integer>[] obs = new Observable[i];
            Arrays.fill(obs, Observable.just(1));

            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);

            Method m = Observable.class.getMethod("concatArray", ObservableSource[].class);

            TestObserver<Integer> to = TestObserver.create();

            ((Observable<Integer>)m.invoke(null, new Object[]{obs})).subscribe(to);

            to.assertValues(expected);
            to.assertNoErrors();
            to.assertComplete();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapJustJust() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(Observable.just(1)).concatMap((Function)Functions.identity(), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT).subscribe(to);

        to.assertValue(1);
        to.assertNoErrors();
        to.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapJustRange() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(Observable.range(1, 5)).concatMap((Function)Functions.identity(), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT).subscribe(to);

        to.assertValues(1, 2, 3, 4, 5);
        to.assertNoErrors();
        to.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapDelayErrorJustJust() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(Observable.just(1)).concatMap((Function)Functions.identity(), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .subscribe(to);

        to.assertValue(1);
        to.assertNoErrors();
        to.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapDelayErrorJustRange() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(Observable.range(1, 5)).concatMap((Function)Functions.identity(), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .subscribe(to);

        to.assertValues(1, 2, 3, 4, 5);
        to.assertNoErrors();
        to.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void startWithArray() throws Exception {
        for (int i = 2; i < 10; i++) {
            Object[] obs = new Object[i];
            Arrays.fill(obs, 1);

            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);

            Method m = Observable.class.getMethod("startWithArray", Object[].class);

            TestObserver<Integer> to = TestObserver.create();

            ((Observable<Integer>)m.invoke(Observable.empty(), new Object[]{obs})).subscribe(to);

            to.assertValues(expected);
            to.assertNoErrors();
            to.assertComplete();
        }
    }

    static final class InfiniteIterator implements Iterator<Integer>, Iterable<Integer> {

        int count;

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Integer next() {
            return count++;
        }

        @Override
        public void remove() {
        }

        @Override
        public Iterator<Integer> iterator() {
            return this;
        }
    }

    @Test
    public void concatMapDelayError() {
        Observable.just(Observable.just(1), Observable.just(2))
        .concatMap(Functions.<Observable<Integer>>identity(), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatMapDelayErrorJustSource() {
        Observable.just(0)
        .concatMap((Function<Object, Observable<Integer>>) _ ->
            Observable.just(1), ImmediateThinScheduler.INSTANCE, new StandardBufferedConfig(ErrorMode.END, 16))
        .test()
        .assertResult(1);

    }

    @Test
    public void concatMapJustSource() {
        Observable.just(0).hide()
        .concatMap((Function<Object, Observable<Integer>>) _ -> Observable.just(1), ImmediateThinScheduler.INSTANCE, new StandardBufferedConfig(16))
        .test()
        .assertResult(1);
    }

    @Test
    public void concatMapJustSourceDelayError() {
        Observable.just(0).hide()
        .concatMap((Function<Object, Observable<Integer>>) _ -> Observable.just(1), ImmediateThinScheduler.INSTANCE, new StandardBufferedConfig(ErrorMode.BOUNDARY, 16))
        .test()
        .assertResult(1);
    }

    @Test
    public void concatMapEmpty() {
        Observable.just(1).hide()
        .concatMap(Functions.justFunction(Observable.empty()), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .test()
        .assertResult();
    }

    @Test
    public void concatMapEmptyDelayError() {
        Observable.just(1).hide()
        .concatMap(Functions.justFunction(Observable.empty()), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .test()
        .assertResult();
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(f ->
        f.concatMap(Functions.justFunction(Observable.just(2)), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT));
        TestHelper.checkDoubleOnSubscribeObservable(f ->
        f.concatMap(Functions.justFunction(Observable.just(2)), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS));
    }

    @Test
    public void immediateInnerNextOuterError() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        var to = new TestObserverEx<Integer>() /* NFI */ {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onError(new TestException("First"));
                }
            }
        };

        ps.concatMap(Functions.justFunction(Observable.just(1)), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .subscribe(to);

        ps.onNext(1);

        assertFalse(ps.hasObservers());

        to.assertFailureAndMessage(TestException.class, "First", 1);
    }

    @Test
    public void immediateInnerNextOuterError2() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        var to = new TestObserverEx<Integer>() /* NFI */ {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onError(new TestException("First"));
                }
            }
        };

        ps.concatMap(Functions.justFunction(Observable.just(1).hide()), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .subscribe(to);

        ps.onNext(1);

        assertFalse(ps.hasObservers());

        to.assertFailureAndMessage(TestException.class, "First", 1);
    }

    @Test
    public void concatMapInnerError() {
        Observable.just(1).hide()
        .concatMap(Functions.justFunction(Observable.error(new TestException())), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void concatMapInnerErrorDelayError() {
        Observable.just(1).hide()
        .concatMap(Functions.justFunction(Observable.error(new TestException())), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(f ->
        f.concatMap(Functions.justFunction(Observable.just(1).hide()), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT), true, 1, 1, 1);
    }

    @Test
    public void badInnerSource() {
        @SuppressWarnings("rawtypes")
        final Observer[] ts0 = { null };
        TestObserverEx<Integer> to = Observable.just(1).hide()
                .concatMap(Functions.justFunction(new Observable<Integer>() /* NFI */ {
            @Override
            protected void subscribeActual(Observer<? super Integer> o) {
                ts0[0] = o;
                o.onSubscribe(Disposable.empty());
                o.onError(new TestException("First"));
            }
        }), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .to(TestHelper.<Integer>testConsumer());

        to.assertFailureAndMessage(TestException.class, "First");

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ts0[0].onError(new TestException("Second"));

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badInnerSourceDelayError() {
        @SuppressWarnings("rawtypes")
        final Observer[] ts0 = { null };
        TestObserverEx<Integer> to = Observable.just(1).hide()
                .concatMap(Functions.justFunction(new Observable<Integer>() /* NFI */ {
            @Override
            protected void subscribeActual(Observer<? super Integer> o) {
                ts0[0] = o;
                o.onSubscribe(Disposable.empty());
                o.onError(new TestException("First"));
            }
        }), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .to(TestHelper.<Integer>testConsumer());

        to.assertFailureAndMessage(TestException.class, "First");

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ts0[0].onError(new TestException("Second"));

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSourceDelayError() {
        TestHelper.checkBadSourceObservable(f -> f.concatMap(Functions.justFunction(Observable.just(1).hide()),
                ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS), true, 1, 1, 1);
    }

    @Test
    public void fusedCrash() {
        Observable.range(1, 2)
        .map(_ -> { throw new TestException(); })
        .concatMap(Functions.justFunction(Observable.just(1)), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedCrashDelayError() {
        Observable.range(1, 2)
        .map(_ -> { throw new TestException(); })
        .concatMap(Functions.justFunction(Observable.just(1)), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void callableCrash() {
        Observable.just(1).hide()
        .concatMap(Functions.justFunction(Observable.fromCallable(() -> {
            throw new TestException();
        })), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void callableCrashDelayError() {
        Observable.just(1).hide()
        .concatMap(Functions.justFunction(Observable.fromCallable(() -> {
            throw new TestException();
        })), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.range(1, 2)
        .concatMap(Functions.justFunction(Observable.just(1)), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT));

        TestHelper.checkDisposed(Observable.range(1, 2)
        .concatMap(Functions.justFunction(Observable.just(1)), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS));
    }

    @Test
    public void notVeryEnd() {
        Observable.range(1, 2)
        .concatMap(Functions.justFunction(Observable.error(new TestException())), ImmediateThinScheduler.INSTANCE, new StandardBufferedConfig(ErrorMode.BOUNDARY, 16))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .concatMap(Functions.justFunction(Observable.just(2)), ImmediateThinScheduler.INSTANCE, new StandardBufferedConfig(ErrorMode.BOUNDARY, 16))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperThrows() {
        Observable.range(1, 2)
        .concatMap((Function<Integer, ObservableSource<Object>>) _ -> {
            throw new TestException();
        }, ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainErrors() {
        PublishSubject<Integer> source = PublishSubject.create();

        TestObserver<Integer> to = TestObserver.create();

        source.concatMap((Function<Integer, Observable<Integer>>) v ->
            Observable.range(v, 2), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS).subscribe(to);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        to.assertValues(1, 2, 2, 3);
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerErrors() {
        final Observable<Integer> inner = Observable.range(1, 2)
                .concatWith(Observable.<Integer>error(new TestException()));

        TestObserver<Integer> to = TestObserver.create();

        Observable.range(1, 3).concatMap((Function<Integer, Observable<Integer>>) _ ->
            inner, ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS).subscribe(to);

        to.assertValues(1, 2, 1, 2, 1, 2);
        to.assertError(CompositeException.class);
        to.assertNotComplete();
    }

    @Test
    public void singleInnerErrors() {
        final Observable<Integer> inner = Observable.range(1, 2).concatWith(Observable.<Integer>error(new TestException()));

        TestObserver<Integer> to = TestObserver.create();

        Observable.just(1)
        .hide() // prevent scalar optimization
        .concatMap((Function<Integer, Observable<Integer>>) _ -> inner, ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS).subscribe(to);

        to.assertValues(1, 2);
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerNull() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(1)
        .hide() // prevent scalar optimization
        .concatMap((Function<Integer, Observable<Integer>>) _ -> null, ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS).subscribe(to);

        to.assertNoValues();
        to.assertError(NullPointerException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerThrows() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(1)
        .hide() // prevent scalar optimization
        .concatMap((Function<Integer, Observable<Integer>>) _ -> {
            throw new TestException();
        }, ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS).subscribe(to);

        to.assertNoValues();
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerWithEmpty() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.range(1, 3)
        .concatMap(v -> v == 2 ? Observable.<Integer>empty() : Observable.range(1, 2), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS).subscribe(to);

        to.assertValues(1, 2, 1, 2);
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void innerWithScalar() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.range(1, 3)
        .concatMap(v -> v == 2 ? Observable.just(3) : Observable.range(1, 2), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .subscribe(to);

        to.assertValues(1, 2, 3, 1, 2);
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void mapperScheduledLong() {
        TestObserver<String> to = Observable.range(1, 1000)
        .hide()
        .observeOn(Schedulers.computation())
        .concatMap((Function<Integer, Observable<String>>) _ -> Observable.just(Thread.currentThread().getName())
                .repeat(1000)
                .observeOn(Schedulers.cached()), Schedulers.single(), StandardBufferedConfig.DEFAULT)
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().getFirst().startsWith("RxSingleScheduler-"), to.values().toString());
    }

    @Test
    public void mapperDelayErrorScheduledLong() {
        TestObserver<String> to = Observable.range(1, 1000)
        .hide()
        .observeOn(Schedulers.computation())
        .concatMap((Function<Integer, Observable<String>>) _ -> Observable.just(Thread.currentThread().getName())
                .repeat(1000)
                .observeOn(Schedulers.cached()), Schedulers.single(), StandardBufferedConfig.DELAY_ERRORS_BOUNDARY)
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().getFirst().startsWith("RxSingleScheduler-"), to.values().toString());
    }

    @Test
    public void mapperDelayError2ScheduledLong() {
        TestObserver<String> to = Observable.range(1, 1000)
        .hide()
        .observeOn(Schedulers.computation())
        .concatMap((Function<Integer, Observable<String>>) _ -> Observable.just(Thread.currentThread().getName())
                .repeat(1000)
                .observeOn(Schedulers.cached()), Schedulers.single(), StandardBufferedConfig.DELAY_ERRORS)
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().getFirst().startsWith("RxSingleScheduler-"), to.values().toString());
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel((ObservableConverter<Integer, Observable<Integer>>) upstream ->
            upstream.concatMap((Function<Integer, Observable<Integer>>) v -> Observable.just(v).hide(), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT));
    }

    @Test
    public void undeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel((ObservableConverter<Integer, Observable<Integer>>) upstream ->
            upstream.concatMap((Function<Integer, Observable<Integer>>) v ->
                Observable.just(v).hide(), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS_BOUNDARY));
    }

    @Test
    public void undeliverableUponCancelDelayErrorTillEnd() {
        TestHelper.checkUndeliverableUponCancel((ObservableConverter<Integer, Observable<Integer>>) upstream ->
            upstream.concatMap((Function<Integer, Observable<Integer>>) v ->
                Observable.just(v).hide(), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS));
    }

    @Test
    public void fusionRejected() {
        TestObserverEx<Object> to = new TestObserverEx<>();

        TestHelper.rejectObservableFusion()
        .concatMap(_ -> Observable.never(), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DEFAULT)
        .subscribe(to);
    }

    @Test
    public void fusionRejectedDelayErrorr() {
        TestObserverEx<Object> to = new TestObserverEx<>();

        TestHelper.rejectObservableFusion()
        .concatMap(_ -> Observable.never(), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .subscribe(to);
    }

    @Test
    public void scalarInnerJustDisposeDelayError() {
        TestObserver<Integer> to = new TestObserver<>();

        Observable.just(1)
        .hide()
        .concatMap(_ -> Observable.fromCallable(() -> {
            to.dispose();
            return 1;
        }), ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS)
        .subscribe(to);

        to.assertEmpty();
    }

    static final class EmptyDisposingObservable extends Observable<Object>
    implements Supplier<Object> {
        final TestObserver<Object> to;
        EmptyDisposingObservable(TestObserver<Object> to) {
            this.to = to;
        }

        @Override
        protected void subscribeActual(@NonNull Observer<? super @NonNull Object> observer) {
            EmptyDisposable.complete(observer);
        }

        @Override
        public @NonNull Object get() throws Throwable {
            to.dispose();
            return null;
        }
    }

    @Test
    public void scalarInnerEmptyDisposeDelayError() {
        TestObserver<Object> to = new TestObserver<>();

        Observable.just(1)
        .hide()
        .concatMap(_ -> new EmptyDisposingObservable(to),
                ImmediateThinScheduler.INSTANCE, StandardBufferedConfig.DELAY_ERRORS
        )
        .subscribe(to);

        to.assertEmpty();
    }
}
