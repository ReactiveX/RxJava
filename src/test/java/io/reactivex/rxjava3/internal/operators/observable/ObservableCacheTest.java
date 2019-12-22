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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableCacheTest extends RxJavaTest {
    @Test
    public void coldReplayNoBackpressure() {
        ObservableCache<Integer> source = new ObservableCache<>(Observable.range(0, 1000), 16);

        assertFalse("Source is connected!", source.isConnected());

        TestObserverEx<Integer> to = new TestObserverEx<>();

        source.subscribe(to);

        assertTrue("Source is not connected!", source.isConnected());
        assertFalse("Subscribers retained!", source.hasObservers());

        to.assertNoErrors();
        to.assertTerminated();
        List<Integer> onNextEvents = to.values();
        assertEquals(1000, onNextEvents.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }
    }

    @Test
    public void cache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Observable<String> o = Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(final Observer<? super String> observer) {
                observer.onSubscribe(Disposable.empty());
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        System.out.println("published Observable being executed");
                        observer.onNext("one");
                        observer.onComplete();
                    }
                }).start();
            }
        }).cache();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Consumer<String>() {
            @Override
            public void accept(String v) {
                    assertEquals("one", v);
                    System.out.println("v: " + v);
                    latch.countDown();
            }
        });

        // subscribe again
        o.subscribe(new Consumer<String>() {
            @Override
            public void accept(String v) {
                    assertEquals("one", v);
                    System.out.println("v: " + v);
                    latch.countDown();
            }
        });

        if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("subscriptions did not receive values");
        }
        assertEquals(1, counter.get());
    }

    @Test
    public void unsubscribeSource() throws Throwable {
        Action unsubscribe = mock(Action.class);
        Observable<Integer> o = Observable.just(1).doOnDispose(unsubscribe).cache();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        verify(unsubscribe, never()).run();
    }

    @Test
    public void take() {
        TestObserver<Integer> to = new TestObserver<>();

        ObservableCache<Integer> cached = new ObservableCache<>(Observable.range(1, 1000), 16);
        cached.take(10).subscribe(to);

        to.assertNoErrors();
        to.assertComplete();
        to.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
//        ts.assertUnsubscribed(); // FIXME no longer valid
        assertFalse(cached.hasObservers());
    }

    @Test
    public void async() {
        Observable<Integer> source = Observable.range(1, 10000);
        for (int i = 0; i < 100; i++) {
            TestObserver<Integer> to1 = new TestObserver<>();

            ObservableCache<Integer> cached = new ObservableCache<>(source, 16);

            cached.observeOn(Schedulers.computation()).subscribe(to1);

            to1.awaitDone(2, TimeUnit.SECONDS);
            to1.assertNoErrors();
            to1.assertComplete();
            assertEquals(10000, to1.values().size());

            TestObserver<Integer> to2 = new TestObserver<>();
            cached.observeOn(Schedulers.computation()).subscribe(to2);

            to2.awaitDone(2, TimeUnit.SECONDS);
            to2.assertNoErrors();
            to2.assertComplete();
            assertEquals(10000, to2.values().size());
        }
    }

    @Test
    public void asyncComeAndGo() {
        Observable<Long> source = Observable.interval(1, 1, TimeUnit.MILLISECONDS)
                .take(1000)
                .subscribeOn(Schedulers.io());
        ObservableCache<Long> cached = new ObservableCache<>(source, 16);

        Observable<Long> output = cached.observeOn(Schedulers.computation());

        List<TestObserver<Long>> list = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            TestObserver<Long> to = new TestObserver<>();
            list.add(to);
            output.skip(i * 10).take(10).subscribe(to);
        }

        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            expected.add((long)(i - 10));
        }
        int j = 0;
        for (TestObserver<Long> to : list) {
            to.awaitDone(3, TimeUnit.SECONDS);
            to.assertNoErrors();
            to.assertComplete();

            for (int i = j * 10; i < j * 10 + 10; i++) {
                expected.set(i - j * 10, (long)i);
            }

            to.assertValueSequence(expected);

            j++;
        }
    }

    @Test
    public void noMissingBackpressureException() {
        final int m = 4 * 1000 * 1000;
        Observable<Integer> firehose = Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> t) {
                t.onSubscribe(Disposable.empty());
                for (int i = 0; i < m; i++) {
                    t.onNext(i);
                }
                t.onComplete();
            }
        });

        TestObserver<Integer> to = new TestObserver<>();
        firehose.cache().observeOn(Schedulers.computation()).takeLast(100).subscribe(to);

        to.awaitDone(3, TimeUnit.SECONDS);
        to.assertNoErrors();
        to.assertComplete();

        assertEquals(100, to.values().size());
    }

    @Test
    public void valuesAndThenError() {
        Observable<Integer> source = Observable.range(1, 10)
                .concatWith(Observable.<Integer>error(new TestException()))
                .cache();

        TestObserver<Integer> to = new TestObserver<>();
        source.subscribe(to);

        to.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        to.assertNotComplete();
        to.assertError(TestException.class);

        TestObserver<Integer> to2 = new TestObserver<>();
        source.subscribe(to2);

        to2.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        to2.assertNotComplete();
        to2.assertError(TestException.class);
    }

    @Test
    public void observers() {
        PublishSubject<Integer> ps = PublishSubject.create();
        ObservableCache<Integer> cache = (ObservableCache<Integer>)Observable.range(1, 5).concatWith(ps).cache();

        assertFalse(cache.hasObservers());

        assertEquals(0, cache.cachedEventCount());

        TestObserver<Integer> to = cache.test();

        assertTrue(cache.hasObservers());

        assertEquals(5, cache.cachedEventCount());

        ps.onComplete();

        to.assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void disposeOnArrival() {
        Observable.range(1, 5).cache()
        .test(true)
        .assertEmpty();
    }

    @Test
    public void disposeOnArrival2() {
        Observable<Integer> o = PublishSubject.<Integer>create().cache();

        o.test();

        o.test(true)
        .assertEmpty();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.range(1, 5).cache());
    }

    @Test
    public void take2() {
        Observable<Integer> cache = Observable.range(1, 5).cache();

        cache.take(2).test().assertResult(1, 2);
        cache.take(3).test().assertResult(1, 2, 3);
    }

    @Test
    public void subscribeEmitRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.<Integer>create();

            final Observable<Integer> cache = ps.cache();

            cache.test();

            final TestObserverEx<Integer> to = new TestObserverEx<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cache.subscribe(to);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 500; j++) {
                        ps.onNext(j);
                    }
                    ps.onComplete();
                }
            };

            TestHelper.race(r1, r2);

            to
            .awaitDone(5, TimeUnit.SECONDS)
            .assertSubscribed().assertValueCount(500).assertComplete().assertNoErrors();
        }
    }

    @Test
    public void cancelledUpFront() {
        final AtomicInteger call = new AtomicInteger();
        Observable<Object> f = Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return call.incrementAndGet();
            }
        }).concatWith(Observable.never())
        .cache();

        f.test().assertValuesOnly(1);

        f.test(true)
        .assertEmpty();

        assertEquals(1, call.get());
    }
}
