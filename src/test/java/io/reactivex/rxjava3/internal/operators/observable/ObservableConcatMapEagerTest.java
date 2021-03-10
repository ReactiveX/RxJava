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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.annotations.NonNull;
import org.junit.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableConcatMapEagerTest extends RxJavaTest {

    @Test
    public void normal() {
        Observable.range(1, 5)
        .concatMapEager((Function<Integer, ObservableSource<Integer>>) t -> Observable.range(t, 2))
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }

    @Test
    public void normalDelayBoundary() {
        Observable.range(1, 5)
        .concatMapEagerDelayError((Function<Integer, ObservableSource<Integer>>) t -> Observable.range(t, 2), false)
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }

    @Test
    public void normalDelayEnd() {
        Observable.range(1, 5)
        .concatMapEagerDelayError((Function<Integer, ObservableSource<Integer>>) t -> Observable.range(t, 2), true)
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }

    @Test
    public void mainErrorsDelayBoundary() {
        PublishSubject<Integer> main = PublishSubject.create();
        final PublishSubject<Integer> inner = PublishSubject.create();

        TestObserverEx<Integer> to = main.concatMapEagerDelayError(
                (Function<Integer, ObservableSource<Integer>>) t -> inner, false).to(TestHelper.testConsumer());

        main.onNext(1);

        inner.onNext(2);

        to.assertValue(2);

        main.onError(new TestException("Forced failure"));

        to.assertNoErrors();

        inner.onNext(3);
        inner.onComplete();

        to.assertFailureAndMessage(TestException.class, "Forced failure", 2, 3);
    }

    @Test
    public void mainErrorsDelayEnd() {
        PublishSubject<Integer> main = PublishSubject.create();
        final PublishSubject<Integer> inner = PublishSubject.create();

        TestObserverEx<Integer> to = main.concatMapEagerDelayError(
                (Function<Integer, ObservableSource<Integer>>) t -> inner, true).to(TestHelper.testConsumer());

        main.onNext(1);
        main.onNext(2);

        inner.onNext(2);

        to.assertValue(2);

        main.onError(new TestException("Forced failure"));

        to.assertNoErrors();

        inner.onNext(3);
        inner.onComplete();

        to.assertFailureAndMessage(TestException.class, "Forced failure", 2, 3, 2, 3);
    }

    @Test
    public void mainErrorsImmediate() {
        PublishSubject<Integer> main = PublishSubject.create();
        final PublishSubject<Integer> inner = PublishSubject.create();

        TestObserverEx<Integer> to = main.concatMapEager(
                (Function<Integer, ObservableSource<Integer>>) t -> inner).to(TestHelper.testConsumer());

        main.onNext(1);
        main.onNext(2);

        inner.onNext(2);

        to.assertValue(2);

        main.onError(new TestException("Forced failure"));

        assertFalse("inner has subscribers?", inner.hasObservers());

        inner.onNext(3);
        inner.onComplete();

        to.assertFailureAndMessage(TestException.class, "Forced failure", 2);
    }

    @Test
    public void longEager() {

        Observable.range(1, 2 * Observable.bufferSize())
        .concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> Observable.just(1))
        .test()
        .assertValueCount(2 * Observable.bufferSize())
        .assertNoErrors()
        .assertComplete();
    }

    TestObserver<Object> to;

    Function<Integer, Observable<Integer>> toJust = Observable::just;

    Function<Integer, Observable<Integer>> toRange = t -> Observable.range(t, 2);

    @Before
    public void before() {
        to = new TestObserver<>();
    }

    @Test
    public void simple() {
        Observable.range(1, 100).concatMapEager(toJust).subscribe(to);

        to.assertNoErrors();
        to.assertValueCount(100);
        to.assertComplete();
    }

    @Test
    public void simple2() {
        Observable.range(1, 100).concatMapEager(toRange).subscribe(to);

        to.assertNoErrors();
        to.assertValueCount(200);
        to.assertComplete();
    }

    @Test
    public void eagerness2() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(t -> count.getAndIncrement()).hide();

        Observable.concatArrayEager(source, source).subscribe(to);

        Assert.assertEquals(2, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness3() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(t -> count.getAndIncrement()).hide();

        Observable.concatArrayEager(source, source, source).subscribe(to);

        Assert.assertEquals(3, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness4() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(t -> count.getAndIncrement()).hide();

        Observable.concatArrayEager(source, source, source, source).subscribe(to);

        Assert.assertEquals(4, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness5() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(t -> count.getAndIncrement()).hide();

        Observable.concatArrayEager(source, source, source, source, source).subscribe(to);

        Assert.assertEquals(5, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness6() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(t -> count.getAndIncrement()).hide();

        Observable.concatArrayEager(source, source, source, source, source, source).subscribe(to);

        Assert.assertEquals(6, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness7() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(t -> count.getAndIncrement()).hide();

        Observable.concatArrayEager(source, source, source, source, source, source, source).subscribe(to);

        Assert.assertEquals(7, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness8() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(t -> count.getAndIncrement()).hide();

        Observable.concatArrayEager(source, source, source, source, source, source, source, source).subscribe(to);

        Assert.assertEquals(8, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness9() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(t -> count.getAndIncrement()).hide();

        Observable.concatArrayEager(source, source, source, source, source, source, source, source, source).subscribe(to);

        Assert.assertEquals(9, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void mainError() {
        Observable.<Integer>error(new TestException()).concatMapEager(toJust).subscribe(to);

        to.assertNoValues();
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerError() {
        // TODO verify: concatMapEager subscribes first then consumes the sources is okay

        PublishSubject<Integer> ps = PublishSubject.create();

        Observable.concatArrayEager(Observable.just(1), ps)
        .subscribe(to);

        ps.onError(new TestException());

        to.assertValue(1);
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerEmpty() {
        Observable.concatArrayEager(Observable.empty(), Observable.empty()).subscribe(to);

        to.assertNoValues();
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void mapperThrows() {
        Observable.just(1).concatMapEager((Function<Integer, Observable<Integer>>) t -> {
            throw new TestException();
        }).subscribe(to);

        to.assertNoValues();
        to.assertNotComplete();
        to.assertError(TestException.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidMaxConcurrent() {
        Observable.just(1).concatMapEager(toJust, 0, Observable.bufferSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCapacityHint() {
        Observable.just(1).concatMapEager(toJust, Observable.bufferSize(), 0);
    }

    @Test
    public void asynchronousRun() {
        Observable.range(1, 2).concatMapEager((Function<Integer, Observable<Integer>>) t -> Observable.range(1, 1000).subscribeOn(Schedulers.computation())).observeOn(Schedulers.newThread()).subscribe(to);

        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        to.assertValueCount(2000);
    }

    @Test
    public void reentrantWork() {
        final PublishSubject<Integer> subject = PublishSubject.create();

        final AtomicBoolean once = new AtomicBoolean();

        subject.concatMapEager((Function<Integer, Observable<Integer>>) Observable::just)
        .doOnNext(t -> {
            if (once.compareAndSet(false, true)) {
                subject.onNext(2);
            }
        })
        .subscribe(to);

        subject.onNext(1);

        to.assertNoErrors();
        to.assertNotComplete();
        to.assertValues(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayEager() throws Exception {
        for (int i = 2; i < 10; i++) {
            Observable<Integer>[] obs = new Observable[i];
            Arrays.fill(obs, Observable.just(1));

            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);

            Method m = Observable.class.getMethod("concatArrayEager", ObservableSource[].class);

            TestObserver<Integer> to = TestObserver.create();

            ((Observable<Integer>)m.invoke(null, new Object[]{obs})).subscribe(to);

            to.assertValues(expected);
            to.assertNoErrors();
            to.assertComplete();
        }
    }

    @Test
    public void capacityHint() {
        Observable<Integer> source = Observable.just(1);
        TestObserver<Integer> to = TestObserver.create();

        Observable.concatEager(Arrays.asList(source, source, source), 1, 1).subscribe(to);

        to.assertValues(1, 1, 1);
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void Observable() {
        Observable<Integer> source = Observable.just(1);
        TestObserver<Integer> to = TestObserver.create();

        Observable.concatEager(Observable.just(source, source, source)).subscribe(to);

        to.assertValues(1, 1, 1);
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void ObservableCapacityHint() {
        Observable<Integer> source = Observable.just(1);
        TestObserver<Integer> to = TestObserver.create();

        Observable.concatEager(Observable.just(source, source, source), 1, 1).subscribe(to);

        to.assertValues(1, 1, 1);
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void badCapacityHint() throws Exception {
        Observable<Integer> source = Observable.just(1);
        try {
            Observable.concatEager(Arrays.asList(source, source, source), 1, -99);
        } catch (IllegalArgumentException ex) {
            assertEquals("bufferSize > 0 required but it was -99", ex.getMessage());
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void mappingBadCapacityHint() throws Exception {
        Observable<Integer> source = Observable.just(1);
        try {
            Observable.just(source, source, source).concatMapEager((Function)Functions.identity(), 10, -99);
        } catch (IllegalArgumentException ex) {
            assertEquals("bufferSize > 0 required but it was -99", ex.getMessage());
        }

    }

    @Test
    public void concatEagerIterable() {
        Observable.concatEager(Arrays.asList(Observable.just(1), Observable.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).hide().concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> Observable.range(1, 2)));
    }

    @Test
    public void empty() {
        Observable.<Integer>empty().hide().concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> Observable.range(1, 2))
        .test()
        .assertResult();
    }

    @Test
    public void innerError2() {
        Observable.just(1).hide().concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> Observable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerErrorMaxConcurrency() {
        Observable.just(1).hide().concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> Observable.error(new TestException()), 1, 128)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerCallableThrows() {
        Observable.just(1).hide().concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> Observable.fromCallable(() -> {
            throw new TestException();
        }))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerOuterRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final PublishSubject<Integer> ps2 = PublishSubject.create();

                TestObserverEx<Integer> to = ps1.concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> ps2).to(TestHelper.testConsumer());

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                ps1.onNext(1);

                Runnable r1 = () -> ps1.onError(ex1);
                Runnable r2 = () -> ps2.onError(ex2);

                TestHelper.race(r1, r2);

                to.assertSubscribed().assertNoValues().assertNotComplete();

                Throwable ex = to.errors().get(0);

                if (ex instanceof CompositeException) {
                    List<Throwable> es = TestHelper.errorList(to);
                    TestHelper.assertError(es, 0, TestException.class);
                    TestHelper.assertError(es, 1, TestException.class);
                } else {
                    to.assertError(TestException.class);
                    if (!errors.isEmpty()) {
                        TestHelper.assertUndeliverable(errors, 0, TestException.class);
                    }
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps1 = PublishSubject.create();

            final TestObserver<Integer> to = ps1.concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> Observable.never()).test();

            Runnable r1 = () -> ps1.onNext(1);
            Runnable r2 = to::dispose;

            TestHelper.race(r1, r2);

            to.assertEmpty();
        }
    }

    @Test
    public void mapperCancels() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.just(1).hide()
        .concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> {
            to.dispose();
            return Observable.never();
        }, 1, 128)
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void innerErrorFused() {
        Observable.just(1).hide().concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> Observable.range(1, 2).map(v1 -> {
            throw new TestException();
        }))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerErrorAfterPoll() {
        final UnicastSubject<Integer> us = UnicastSubject.create();
        us.onNext(1);

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(@NonNull Integer t) {
                super.onNext(t);
                us.onError(new TestException());
            }
        };

        Observable.just(1).hide()
        .concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> us, 1, 128)
        .subscribe(to);

        to
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void fuseAndTake() {
        UnicastSubject<Integer> us = UnicastSubject.create();

        us.onNext(1);
        us.onComplete();

        us.concatMapEager((Function<Integer, ObservableSource<Integer>>) v -> Observable.just(1))
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(o -> o.concatMapEager((Function<Object, ObservableSource<Object>>) Observable::just));
    }

    @Test
    public void oneDelayed() {
        Observable.just(1, 2, 3, 4, 5)
        .concatMapEager((Function<Integer, ObservableSource<Integer>>) i -> i == 3 ? Observable.just(i) : Observable
                .just(i)
                .delay(1, TimeUnit.MILLISECONDS, Schedulers.io()))
        .observeOn(Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5)
        ;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void maxConcurrencyOf2() {
        List<Integer>[] list = new ArrayList[100];
        for (int i = 0; i < 100; i++) {
            List<Integer> lst = new ArrayList<>();
            list[i] = lst;
            for (int k = 1; k <= 10; k++) {
                lst.add((i) * 10 + k);
            }
        }

        Observable.range(1, 1000)
        .buffer(10)
        .concatMapEager((Function<List<Integer>, ObservableSource<List<Integer>>>) v -> Observable.just(v)
                .subscribeOn(Schedulers.io())
                .doOnNext(v1 -> Thread.sleep(new Random().nextInt(20)))
                , 2, 3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(list);
    }

    @Test
    public void arrayDelayErrorDefault() {
        PublishSubject<Integer> ps1 = PublishSubject.create();
        PublishSubject<Integer> ps2 = PublishSubject.create();
        PublishSubject<Integer> ps3 = PublishSubject.create();

        TestObserver<Integer> to = Observable.concatArrayEagerDelayError(ps1, ps2, ps3)
        .test();

        to.assertEmpty();

        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        assertTrue(ps3.hasObservers());

        ps2.onNext(2);
        ps2.onComplete();

        to.assertEmpty();

        ps1.onNext(1);

        to.assertValuesOnly(1);

        ps1.onComplete();

        to.assertValuesOnly(1, 2);

        ps3.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void arrayDelayErrorMaxConcurrency() {
        PublishSubject<Integer> ps1 = PublishSubject.create();
        PublishSubject<Integer> ps2 = PublishSubject.create();
        PublishSubject<Integer> ps3 = PublishSubject.create();

        TestObserver<Integer> to = Observable.concatArrayEagerDelayError(2, 2, ps1, ps2, ps3)
        .test();

        to.assertEmpty();

        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        assertFalse(ps3.hasObservers());

        ps2.onNext(2);
        ps2.onComplete();

        to.assertEmpty();

        ps1.onNext(1);

        to.assertValuesOnly(1);

        ps1.onComplete();

        assertTrue(ps3.hasObservers());

        to.assertValuesOnly(1, 2);

        ps3.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void arrayDelayErrorMaxConcurrencyErrorDelayed() {
        PublishSubject<Integer> ps1 = PublishSubject.create();
        PublishSubject<Integer> ps2 = PublishSubject.create();
        PublishSubject<Integer> ps3 = PublishSubject.create();

        TestObserver<Integer> to = Observable.concatArrayEagerDelayError(2, 2, ps1, ps2, ps3)
        .test();

        to.assertEmpty();

        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        assertFalse(ps3.hasObservers());

        ps2.onNext(2);
        ps2.onError(new TestException());

        to.assertEmpty();

        ps1.onNext(1);

        to.assertValuesOnly(1);

        ps1.onComplete();

        assertTrue(ps3.hasObservers());

        to.assertValuesOnly(1, 2);

        ps3.onComplete();

        to.assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void cancelActive() {
        PublishSubject<Integer> ps1 = PublishSubject.create();
        PublishSubject<Integer> ps2 = PublishSubject.create();

        TestObserver<Integer> to = Observable
                .concatEager(Observable.just(ps1, ps2))
                .test();

        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());

        to.dispose();

        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
    }

    @Test
    public void cancelNoInnerYet() {
        PublishSubject<Observable<Integer>> ps1 = PublishSubject.create();

        TestObserver<Integer> to = Observable
                .concatEager(ps1)
                .test();

        assertTrue(ps1.hasObservers());

        to.dispose();

        assertFalse(ps1.hasObservers());
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel((ObservableConverter<Integer, Observable<Integer>>) upstream -> upstream.concatMapEager((Function<Integer, Observable<Integer>>) v -> Observable.just(v).hide()));
    }

    @Test
    public void undeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel((ObservableConverter<Integer, Observable<Integer>>) upstream -> upstream.concatMapEagerDelayError((Function<Integer, Observable<Integer>>) v -> Observable.just(v).hide(), false));
    }

    @Test
    public void undeliverableUponCancelDelayErrorTillEnd() {
        TestHelper.checkUndeliverableUponCancel((ObservableConverter<Integer, Observable<Integer>>) upstream -> upstream.concatMapEagerDelayError((Function<Integer, Observable<Integer>>) v -> Observable.just(v).hide(), true));
    }

    @Test
    public void iterableDelayError() {
        Observable.concatEagerDelayError(Arrays.asList(
                Observable.range(1, 2),
                Observable.error(new TestException()),
                Observable.range(3, 3)
        ))
        .test()
        .assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void iterableDelayErrorMaxConcurrency() {
        Observable.concatEagerDelayError(Arrays.asList(
                Observable.range(1, 2),
                Observable.error(new TestException()),
                Observable.range(3, 3)
        ), 1, 1)
        .test()
        .assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void observerDelayError() {
        Observable.concatEagerDelayError(Observable.fromArray(
                Observable.range(1, 2),
                Observable.error(new TestException()),
                Observable.range(3, 3)
        ))
        .test()
        .assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void observerDelayErrorMaxConcurrency() {
        Observable.concatEagerDelayError(Observable.fromArray(
                Observable.range(1, 2),
                Observable.error(new TestException()),
                Observable.range(3, 3)
        ), 1, 1)
        .test()
        .assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void innerFusionRejected() {
        Observable.just(1)
        .hide()
        .concatMapEager(v -> TestHelper.rejectObservableFusion())
        .test()
        .assertEmpty();
    }
}
