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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.HasUpstreamObservableSource;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.PublishSubject;

public class ObservablePublishAltTest {

    @Test
    public void testPublish() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        ConnectableObservable<String> o = Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(final Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        observer.onNext("one");
                        observer.onComplete();
                    }
                }).start();
            }
        }).publish();

        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        // subscribe again
        o.subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        Disposable connection = o.connect();
        try {
            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                fail("subscriptions did not receive values");
            }
            assertEquals(1, counter.get());
        } finally {
            connection.dispose();
        }
    }

    @Test
    public void testBackpressureFastSlow() {
        ConnectableObservable<Integer> is = Observable.range(1, Flowable.bufferSize() * 2).publish();
        Observable<Integer> fast = is.observeOn(Schedulers.computation())
        .doOnComplete(new Action() {
            @Override
            public void run() {
                System.out.println("^^^^^^^^^^^^^ completed FAST");
            }
        });

        Observable<Integer> slow = is.observeOn(Schedulers.computation()).map(new Function<Integer, Integer>() {
            int c;

            @Override
            public Integer apply(Integer i) {
                if (c == 0) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
                c++;
                return i;
            }

        }).doOnComplete(new Action() {

            @Override
            public void run() {
                System.out.println("^^^^^^^^^^^^^ completed SLOW");
            }

        });

        TestObserver<Integer> to = new TestObserver<Integer>();
        Observable.merge(fast, slow).subscribe(to);
        is.connect();
        to.awaitTerminalEvent();
        to.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 4, to.valueCount());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void testTakeUntilWithPublishedStreamUsingSelector() {
        final AtomicInteger emitted = new AtomicInteger();
        Observable<Integer> xs = Observable.range(0, Flowable.bufferSize() * 2).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                emitted.incrementAndGet();
            }

        });
        TestObserver<Integer> to = new TestObserver<Integer>();
        xs.publish(new Function<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Observable<Integer> xs) {
                return xs.takeUntil(xs.skipWhile(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer i) {
                        return i <= 3;
                    }

                }));
            }

        }).subscribe(to);
        to.awaitTerminalEvent();
        to.assertNoErrors();
        to.assertValues(0, 1, 2, 3);
        assertEquals(5, emitted.get());
        System.out.println(to.values());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void testTakeUntilWithPublishedStream() {
        Observable<Integer> xs = Observable.range(0, Flowable.bufferSize() * 2);
        TestObserver<Integer> to = new TestObserver<Integer>();
        ConnectableObservable<Integer> xsp = xs.publish();
        xsp.takeUntil(xsp.skipWhile(new Predicate<Integer>() {

            @Override
            public boolean test(Integer i) {
                return i <= 3;
            }

        })).subscribe(to);
        xsp.connect();
        System.out.println(to.values());
    }

    @Test(timeout = 10000)
    public void testBackpressureTwoConsumers() {
        final AtomicInteger sourceEmission = new AtomicInteger();
        final AtomicBoolean sourceUnsubscribed = new AtomicBoolean();
        final Observable<Integer> source = Observable.range(1, 100)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t1) {
                        sourceEmission.incrementAndGet();
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                        sourceUnsubscribed.set(true);
                    }
                }).share();
        ;

        final AtomicBoolean child1Unsubscribed = new AtomicBoolean();
        final AtomicBoolean child2Unsubscribed = new AtomicBoolean();

        final TestObserver<Integer> to2 = new TestObserver<Integer>();

        final TestObserver<Integer> to1 = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (valueCount() == 2) {
                    source.doOnDispose(new Action() {
                        @Override
                        public void run() {
                            child2Unsubscribed.set(true);
                        }
                    }).take(5).subscribe(to2);
                }
                super.onNext(t);
            }
        };

        source.doOnDispose(new Action() {
            @Override
            public void run() {
                child1Unsubscribed.set(true);
            }
        }).take(5)
        .subscribe(to1);

        to1.awaitTerminalEvent();
        to2.awaitTerminalEvent();

        to1.assertNoErrors();
        to2.assertNoErrors();

        assertTrue(sourceUnsubscribed.get());
        assertTrue(child1Unsubscribed.get());
        assertTrue(child2Unsubscribed.get());

        to1.assertValues(1, 2, 3, 4, 5);
        to2.assertValues(4, 5, 6, 7, 8);

        assertEquals(8, sourceEmission.get());
    }

    @Test
    public void testConnectWithNoSubscriber() {
        TestScheduler scheduler = new TestScheduler();
        ConnectableObservable<Long> co = Observable.interval(10, 10, TimeUnit.MILLISECONDS, scheduler).take(3).publish();
        co.connect();
        // Emit 0
        scheduler.advanceTimeBy(15, TimeUnit.MILLISECONDS);
        TestObserver<Long> to = new TestObserver<Long>();
        co.subscribe(to);
        // Emit 1 and 2
        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        to.assertValues(1L, 2L);
        to.assertNoErrors();
        to.assertTerminated();
    }

    @Test
    public void testSubscribeAfterDisconnectThenConnect() {
        ConnectableObservable<Integer> source = Observable.just(1).publish();

        TestObserver<Integer> to1 = new TestObserver<Integer>();

        source.subscribe(to1);

        Disposable connection = source.connect();

        to1.assertValue(1);
        to1.assertNoErrors();
        to1.assertTerminated();

        TestObserver<Integer> to2 = new TestObserver<Integer>();

        source.subscribe(to2);

        Disposable connection2 = source.connect();

        to2.assertValue(1);
        to2.assertNoErrors();
        to2.assertTerminated();

        System.out.println(connection);
        System.out.println(connection2);
    }

    @Test
    public void testNoSubscriberRetentionOnCompleted() {
        ObservablePublish<Integer> source = (ObservablePublish<Integer>)Observable.just(1).publish();

        TestObserver<Integer> to1 = new TestObserver<Integer>();

        source.subscribe(to1);

        to1.assertNoValues();
        to1.assertNoErrors();
        to1.assertNotComplete();

        source.connect();

        to1.assertValue(1);
        to1.assertNoErrors();
        to1.assertTerminated();

        assertNull(source.current.get());
    }

    @Test
    public void testNonNullConnection() {
        ConnectableObservable<Object> source = Observable.never().publish();

        assertNotNull(source.connect());
        assertNotNull(source.connect());
    }

    @Test
    public void testNoDisconnectSomeoneElse() {
        ConnectableObservable<Object> source = Observable.never().publish();

        Disposable connection1 = source.connect();
        Disposable connection2 = source.connect();

        connection1.dispose();

        Disposable connection3 = source.connect();

        connection2.dispose();

        assertTrue(checkPublishDisposed(connection1));
        assertTrue(checkPublishDisposed(connection2));
        assertFalse(checkPublishDisposed(connection3));
    }

    @SuppressWarnings("unchecked")
    static boolean checkPublishDisposed(Disposable d) {
        return ((ObservablePublish.PublishObserver<Object>)d).isDisposed();
    }

    @Test
    public void testConnectIsIdempotent() {
        final AtomicInteger calls = new AtomicInteger();
        Observable<Integer> source = Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> t) {
                t.onSubscribe(Disposables.empty());
                calls.getAndIncrement();
            }
        });

        ConnectableObservable<Integer> conn = source.publish();

        assertEquals(0, calls.get());

        conn.connect();
        conn.connect();

        assertEquals(1, calls.get());

        conn.connect().dispose();

        conn.connect();
        conn.connect();

        assertEquals(2, calls.get());
    }

    @Test
    public void testObserveOn() {
        ConnectableObservable<Integer> co = Observable.range(0, 1000).publish();
        Observable<Integer> obs = co.observeOn(Schedulers.computation());
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestObserver<Integer>> tos = new ArrayList<TestObserver<Integer>>();
                for (int k = 1; k < j; k++) {
                    TestObserver<Integer> to = new TestObserver<Integer>();
                    tos.add(to);
                    obs.subscribe(to);
                }

                Disposable connection = co.connect();

                for (TestObserver<Integer> to : tos) {
                    to.awaitTerminalEvent(2, TimeUnit.SECONDS);
                    to.assertTerminated();
                    to.assertNoErrors();
                    assertEquals(1000, to.valueCount());
                }
                connection.dispose();
            }
        }
    }

    @Test
    public void preNextConnect() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();

            co.connect();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.test();
                }
            };

            TestHelper.race(r1, r1);
        }
    }

    @Test
    public void connectRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.connect();
                }
            };

            TestHelper.race(r1, r1);
        }
    }

    @Test
    public void selectorCrash() {
        Observable.just(1).publish(new Function<Observable<Integer>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Integer> v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void source() {
        Observable<Integer> o = Observable.never();

        assertSame(o, (((HasUpstreamObservableSource<?>)o.publish()).source()));
    }

    @Test
    public void connectThrows() {
        ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();
        try {
            co.connect(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable d) throws Exception {
                    throw new TestException();
                }
            });
        } catch (TestException ex) {
            // expected
        }
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();

            final TestObserver<Integer> to = co.test();

            final TestObserver<Integer> to2 = new TestObserver<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to2);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void disposeOnArrival() {
        ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();

        co.test(true).assertEmpty();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.never().publish());

        TestHelper.checkDisposed(Observable.never().publish(Functions.<Observable<Object>>identity()));
    }

    @Test
    public void empty() {
        ConnectableObservable<Integer> co = Observable.<Integer>empty().publish();

        co.connect();
    }

    @Test
    public void take() {
        ConnectableObservable<Integer> co = Observable.range(1, 2).publish();

        TestObserver<Integer> to = co.take(1).test();

        co.connect();

        to.assertResult(1);
    }

    @Test
    public void just() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        ConnectableObservable<Integer> co = ps.publish();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                ps.onComplete();
            }
        };

        co.subscribe(to);
        co.connect();

        ps.onNext(1);

        to.assertResult(1);
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final ConnectableObservable<Integer> co = ps.publish();

            final TestObserver<Integer> to = co.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .publish()
            .autoConnect()
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void noErrorLoss() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ConnectableObservable<Object> co = Observable.error(new TestException()).publish();

            co.connect();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void subscribeDisconnectRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final ConnectableObservable<Integer> co = ps.publish();

            final Disposable d = co.connect();
            final TestObserver<Integer> to = new TestObserver<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    d.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void selectorDisconnectsIndependentSource() {
        PublishSubject<Integer> ps = PublishSubject.create();

        ps.publish(new Function<Observable<Integer>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Integer> v) throws Exception {
                return Observable.range(1, 2);
            }
        })
        .test()
        .assertResult(1, 2);

        assertFalse(ps.hasObservers());
    }

    @Test(timeout = 5000)
    public void selectorLatecommer() {
        Observable.range(1, 5)
        .publish(new Function<Observable<Integer>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Integer> v) throws Exception {
                return v.concatWith(v);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void mainError() {
        Observable.error(new TestException())
        .publish(Functions.<Observable<Object>>identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void selectorInnerError() {
        PublishSubject<Integer> ps = PublishSubject.create();

        ps.publish(new Function<Observable<Integer>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Integer> v) throws Exception {
                return Observable.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void delayedUpstreamOnSubscribe() {
        final Observer<?>[] sub = { null };

        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                sub[0] = observer;
            }
        }
        .publish()
        .connect()
        .dispose();

        Disposable bs = Disposables.empty();

        sub[0].onSubscribe(bs);

        assertTrue(bs.isDisposed());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(final Observable<Object> o)
                    throws Exception {
                return Observable.<Integer>never().publish(new Function<Observable<Integer>, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Observable<Integer> v)
                            throws Exception {
                        return o;
                    }
                });
            }
        }
        );
    }

    @Test
    public void disposedUpfront() {
        ConnectableObservable<Integer> co = Observable.just(1)
                .concatWith(Observable.<Integer>never())
                .publish();

        TestObserver<Integer> to1 = co.test();

        TestObserver<Integer> to2 = co.test(true);

        co.connect();

        to1.assertValuesOnly(1);

        to2.assertEmpty();

        ((ObservablePublish<Integer>)co).current.get().remove(null);
    }

    @Test
    public void altConnectCrash() {
        try {
            new ObservablePublishAlt<Integer>(Observable.<Integer>empty())
            .connect(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable t) throws Exception {
                    throw new TestException();
                }
            });
            fail("Should have thrown");
        } catch (TestException expected) {
            // expected
        }
    }

    @Test
    public void altConnectRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final ConnectableObservable<Integer> co =
                    new ObservablePublishAlt<Integer>(Observable.<Integer>never());

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    co.connect();
                }
            };

            TestHelper.race(r, r);
        }
    }
}
