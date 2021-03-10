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

package io.reactivex.rxjava3.internal.operators.single;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.util.CrashingIterable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class SingleFlatMapIterableFlowableTest extends RxJavaTest {

    @Test
    public void normal() {

        Single.just(1).flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> Arrays.asList(v, v + 1))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void emptyIterable() {

        Single.just(1).flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> Collections.emptyList())
        .test()
        .assertResult();
    }

    @Test
    public void error() {

        Single.<Integer>error(new TestException()).flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> Arrays.asList(v, v + 1))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void backpressure() {

        TestSubscriber<Integer> ts = Single.just(1).flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> Arrays.asList(v, v + 1))
        .test(0);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(1);

        ts.request(1);

        ts.assertResult(1, 2);
    }

    @Test
    public void take() {
        Single.just(1).flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> Arrays.asList(v, v + 1))
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void fused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Single.just(1).flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> Arrays.asList(v, v + 1))
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2);
    }

    @Test
    public void fusedNoSync() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.SYNC);

        Single.just(1).flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> Arrays.asList(v, v + 1))
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2);
    }

    @Test
    public void iteratorCrash() {

        Single.just(1).flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> new CrashingIterable(1, 100, 100))
        .to(TestHelper.testConsumer())
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void hasNextCrash() {

        Single.just(1).flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> new CrashingIterable(100, 1, 100))
        .to(TestHelper.testConsumer())
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void nextCrash() {

        Single.just(1).flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> new CrashingIterable(100, 100, 1))
        .to(TestHelper.testConsumer())
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void hasNextCrash2() {

        Single.just(1).flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> new CrashingIterable(100, 2, 100))
        .to(TestHelper.testConsumer())
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void async1() {
        Single.just(1)
        .flattenAsFlowable((Function<Object, Iterable<Integer>>) v -> {
            Integer[] array = new Integer[1000 * 1000];
            Arrays.fill(array, 1);
            return Arrays.asList(array);
        })
        .hide()
        .observeOn(Schedulers.single())
        .to(TestHelper.testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(1000 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async2() {
        Single.just(1)
        .flattenAsFlowable((Function<Object, Iterable<Integer>>) v -> {
            Integer[] array = new Integer[1000 * 1000];
            Arrays.fill(array, 1);
            return Arrays.asList(array);
        })
        .observeOn(Schedulers.single())
        .to(TestHelper.testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(1000 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async3() {
        Single.just(1)
        .flattenAsFlowable((Function<Object, Iterable<Integer>>) v -> {
            Integer[] array = new Integer[1000 * 1000];
            Arrays.fill(array, 1);
            return Arrays.asList(array);
        })
        .take(500 * 1000)
        .observeOn(Schedulers.single())
        .to(TestHelper.testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(500 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async4() {
        Single.just(1)
        .flattenAsFlowable((Function<Object, Iterable<Integer>>) v -> {
            Integer[] array = new Integer[1000 * 1000];
            Arrays.fill(array, 1);
            return Arrays.asList(array);
        })
        .observeOn(Schedulers.single())
        .take(500 * 1000)
        .to(TestHelper.testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(500 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void fusedEmptyCheck() {
        Single.just(1)
        .flattenAsFlowable((Function<Object, Iterable<Integer>>) v -> Arrays.asList(1, 2, 3)).subscribe(new FlowableSubscriber<Integer>() {
            QueueSubscription<Integer> qs;
            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(@NonNull Subscription s) {
                qs = (QueueSubscription<Integer>)s;

                assertEquals(QueueFuseable.ASYNC, qs.requestFusion(QueueFuseable.ANY));
            }

            @Override
            public void onNext(Integer value) {
                assertFalse(qs.isEmpty());

                qs.clear();

                assertTrue(qs.isEmpty());

                qs.cancel();
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void hasNextThrowsUnbounded() {
        Single.just(1)
        .flattenAsFlowable((Function<Object, Iterable<Integer>>) v -> new CrashingIterable(100, 2, 100))
        .to(TestHelper.testConsumer())
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void nextThrowsUnbounded() {
        Single.just(1)
        .flattenAsFlowable((Function<Object, Iterable<Integer>>) v -> new CrashingIterable(100, 100, 1))
        .to(TestHelper.testConsumer())
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void hasNextThrows() {
        Single.just(1)
        .flattenAsFlowable((Function<Object, Iterable<Integer>>) v -> new CrashingIterable(100, 2, 100))
        .to(TestHelper.testSubscriber(2L))
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void nextThrows() {
        Single.just(1)
        .flattenAsFlowable((Function<Object, Iterable<Integer>>) v -> new CrashingIterable(100, 100, 1))
        .to(TestHelper.testSubscriber(2L))
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void requestBefore() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            ps.singleElement().flattenAsFlowable(
                    (Function<Integer, Iterable<Integer>>) v -> Arrays.asList(1, 2, 3))
            .test(5L)
            .assertEmpty();
        }
    }

    @Test
    public void requestCreateInnerRace() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            ps.onNext(1);

            final TestSubscriber<Integer> ts = ps.singleElement().flattenAsFlowable(
                    (Function<Integer, Iterable<Integer>>) v -> Arrays.asList(a))
            .test(0L);

            Runnable r1 = () -> {
                ps.onComplete();
                for (int i12 = 0; i12 < 500; i12++) {
                    ts.request(1);
                }
            };

            Runnable r2 = () -> {
                for (int i1 = 0; i1 < 500; i1++) {
                    ts.request(1);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void cancelCreateInnerRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            ps.onNext(1);

            final TestSubscriber<Integer> ts = ps.singleElement().flattenAsFlowable(
                    (Function<Integer, Iterable<Integer>>) v -> Arrays.asList(1, 2, 3))
            .test(0L);

            Runnable r1 = ps::onComplete;

            Runnable r2 = ts::cancel;

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void slowPathCancelAfterHasNext() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        final TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

        Single.just(1)
        .flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> () -> new Iterator<Integer>() {
            int count;
            @Override
            public boolean hasNext() {
                if (count++ == 2) {
                    ts.cancel();
                }
                return true;
            }

            @Override
            public Integer next() {
                return 1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        })
        .subscribe(ts);

        ts.request(3);
        ts.assertValues(1, 1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void fastPathCancelAfterHasNext() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        final TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

        Single.just(1)
        .flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> () -> new Iterator<Integer>() {
            int count;
            @Override
            public boolean hasNext() {
                if (count++ == 2) {
                    ts.cancel();
                }
                return true;
            }

            @Override
            public Integer next() {
                return 1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        })
        .subscribe(ts);

        ts.request(Long.MAX_VALUE);
        ts.assertValues(1, 1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void requestIteratorRace() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestSubscriber<Integer> ts = ps.singleOrError().flattenAsFlowable((Function<Integer, Iterable<Integer>>) v -> Arrays.asList(a)).test();

            Runnable r1 = () -> {
                for (int i1 = 0; i1 < 1000; i1++) {
                    ts.request(1);
                }
            };

            Runnable r2 = () -> {
                ps.onNext(1);
                ps.onComplete();
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToFlowable(s -> s.flattenAsFlowable(v -> Collections.emptyList()));
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(SingleSubject.create().flattenAsFlowable(v -> Collections.emptyList()));
    }

    @Test
    public void slowPatchCancelAfterOnNext() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(@NonNull Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Single.just(1)
        .flattenAsFlowable(v -> Arrays.asList(1, 2))
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void onSuccessRequestRace() {
        List<Object> list = Collections.singletonList(1);
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            SingleSubject<Integer> ss = SingleSubject.create();

            TestSubscriber<Object> ts = ss.flattenAsFlowable(v -> list)
            .test(0L);

            TestHelper.race(
                    () -> ss.onSuccess(1),
                    () -> ts.request(1)
            );

            ts.assertResult(1);
        }
    }
}
