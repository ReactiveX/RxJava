/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.BiPredicate;
import io.reactivex.observers.TestObserver;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableSequenceEqualTest {

    @Test
    public void test1Flowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.just("one", "two", "three")).toFlowable();
        verifyResult(observable, true);
    }

    @Test
    public void test2Flowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.just("one", "two", "three", "four")).toFlowable();
        verifyResult(observable, false);
    }

    @Test
    public void test3Flowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three", "four"),
                Flowable.just("one", "two", "three")).toFlowable();
        verifyResult(observable, false);
    }

    @Test
    public void testWithError1Flowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())),
                Flowable.just("one", "two", "three")).toFlowable();
        verifyError(observable);
    }

    @Test
    public void testWithError2Flowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException()))).toFlowable();
        verifyError(observable);
    }

    @Test
    public void testWithError3Flowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())),
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException()))).toFlowable();
        verifyError(observable);
    }

    @Test
    public void testWithEmpty1Flowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.<String> empty(),
                Flowable.just("one", "two", "three")).toFlowable();
        verifyResult(observable, false);
    }

    @Test
    public void testWithEmpty2Flowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.<String> empty()).toFlowable();
        verifyResult(observable, false);
    }

    @Test
    public void testWithEmpty3Flowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.<String> empty(), Flowable.<String> empty()).toFlowable();
        verifyResult(observable, true);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull1Flowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just((String) null), Flowable.just("one")).toFlowable();
        verifyResult(observable, false);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull2Flowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just((String) null), Flowable.just((String) null)).toFlowable();
        verifyResult(observable, true);
    }

    @Test
    public void testWithEqualityErrorFlowable() {
        Flowable<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one"), Flowable.just("one"),
                new BiPredicate<String, String>() {
                    @Override
                    public boolean test(String t1, String t2) {
                        throw new TestException();
                    }
                }).toFlowable();
        verifyError(observable);
    }

    @Test
    public void test1() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.just("one", "two", "three"));
        verifyResult(observable, true);
    }

    @Test
    public void test2() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.just("one", "two", "three", "four"));
        verifyResult(observable, false);
    }

    @Test
    public void test3() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three", "four"),
                Flowable.just("one", "two", "three"));
        verifyResult(observable, false);
    }

    @Test
    public void testWithError1() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())),
                Flowable.just("one", "two", "three"));
        verifyError(observable);
    }

    @Test
    public void testWithError2() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())));
        verifyError(observable);
    }

    @Test
    public void testWithError3() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())),
                Flowable.concat(Flowable.just("one"),
                        Flowable.<String> error(new TestException())));
        verifyError(observable);
    }

    @Test
    public void testWithEmpty1() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.<String> empty(),
                Flowable.just("one", "two", "three"));
        verifyResult(observable, false);
    }

    @Test
    public void testWithEmpty2() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one", "two", "three"),
                Flowable.<String> empty());
        verifyResult(observable, false);
    }

    @Test
    public void testWithEmpty3() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.<String> empty(), Flowable.<String> empty());
        verifyResult(observable, true);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull1() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just((String) null), Flowable.just("one"));
        verifyResult(observable, false);
    }

    @Test
    @Ignore("Null values not allowed")
    public void testWithNull2() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just((String) null), Flowable.just((String) null));
        verifyResult(observable, true);
    }

    @Test
    public void testWithEqualityError() {
        Single<Boolean> observable = Flowable.sequenceEqual(
                Flowable.just("one"), Flowable.just("one"),
                new BiPredicate<String, String>() {
                    @Override
                    public boolean test(String t1, String t2) {
                        throw new TestException();
                    }
                });
        verifyError(observable);
    }

    private void verifyResult(Flowable<Boolean> observable, boolean result) {
        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(result);
        inOrder.verify(observer).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyResult(Single<Boolean> observable, boolean result) {
        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(result);
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyError(Flowable<Boolean> observable) {
        Subscriber<Boolean> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(isA(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyError(Single<Boolean> observable) {
        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(isA(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void prefetch() {

        Flowable.sequenceEqual(Flowable.range(1, 20), Flowable.range(1, 20), 2)
        .test()
        .assertResult(true);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Flowable.sequenceEqual(Flowable.just(1), Flowable.just(2)));
    }

    @Test
    public void simpleInequal() {
        Flowable.sequenceEqual(Flowable.just(1), Flowable.just(2))
        .test()
        .assertResult(false);
    }

    @Test
    public void simpleInequalObservable() {
        Flowable.sequenceEqual(Flowable.just(1), Flowable.just(2))
        .toFlowable()
        .test()
        .assertResult(false);
    }

    @Test
    public void onNextCancelRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> ps = PublishProcessor.create();

            final TestObserver<Boolean> to = Flowable.sequenceEqual(Flowable.never(), ps).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                }
            };

            TestHelper.race(r1, r2);

            to.assertEmpty();
        }
    }

    @Test
    public void onNextCancelRaceObservable() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> ps = PublishProcessor.create();

            final TestSubscriber<Boolean> to = Flowable.sequenceEqual(Flowable.never(), ps).toFlowable().test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                }
            };

            TestHelper.race(r1, r2);

            to.assertEmpty();
        }
    }

    @Test
    @Ignore("RS Subscription no isCancelled")
    public void disposedFlowable() {
        TestHelper.checkDisposed(Flowable.sequenceEqual(Flowable.just(1), Flowable.just(2)).toFlowable());
    }

    @Test
    public void prefetchFlowable() {
        Flowable.sequenceEqual(Flowable.range(1, 20), Flowable.range(1, 20), 2)
        .toFlowable()
        .test()
        .assertResult(true);
    }
}