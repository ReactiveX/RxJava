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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.HasUpstreamObservableSource;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservablePublishTest extends RxJavaTest {

    @Test
    public void publish() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        ConnectableObservable<String> o = Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(final Observer<? super String> observer) {
                observer.onSubscribe(Disposable.empty());
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
    public void backpressureFastSlow() {
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

        TestObserver<Integer> to = new TestObserver<>();
        Observable.merge(fast, slow).subscribe(to);
        is.connect();
        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 4, to.values().size());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void takeUntilWithPublishedStreamUsingSelector() {
        final AtomicInteger emitted = new AtomicInteger();
        Observable<Integer> xs = Observable.range(0, Flowable.bufferSize() * 2).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                emitted.incrementAndGet();
            }

        });
        TestObserver<Integer> to = new TestObserver<>();
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
        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        to.assertValues(0, 1, 2, 3);
        assertEquals(5, emitted.get());
        System.out.println(to.values());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void takeUntilWithPublishedStream() {
        Observable<Integer> xs = Observable.range(0, Flowable.bufferSize() * 2);
        TestObserver<Integer> to = new TestObserver<>();
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

    @Test
    public void backpressureTwoConsumers() {
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

        final TestObserver<Integer> to2 = new TestObserver<>();

        final TestObserver<Integer> to1 = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (values().size() == 2) {
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

        to1.awaitDone(5, TimeUnit.SECONDS);
        to2.awaitDone(5, TimeUnit.SECONDS);

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
    public void connectWithNoSubscriber() {
        TestScheduler scheduler = new TestScheduler();
        ConnectableObservable<Long> co = Observable.interval(10, 10, TimeUnit.MILLISECONDS, scheduler).take(3).publish();
        co.connect();
        // Emit 0
        scheduler.advanceTimeBy(15, TimeUnit.MILLISECONDS);
        TestObserverEx<Long> to = new TestObserverEx<>();
        co.subscribe(to);
        // Emit 1 and 2
        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        to.assertValues(1L, 2L);
        to.assertNoErrors();
        to.assertTerminated();
    }

    @Test
    public void subscribeAfterDisconnectThenConnect() {
        ConnectableObservable<Integer> source = Observable.just(1).publish();

        TestObserverEx<Integer> to1 = new TestObserverEx<>();

        source.subscribe(to1);

        Disposable connection = source.connect();

        to1.assertValue(1);
        to1.assertNoErrors();
        to1.assertTerminated();

        source.reset();

        TestObserverEx<Integer> to2 = new TestObserverEx<>();

        source.subscribe(to2);

        Disposable connection2 = source.connect();

        to2.assertValue(1);
        to2.assertNoErrors();
        to2.assertTerminated();

        System.out.println(connection);
        System.out.println(connection2);
    }

    @Test
    public void noSubscriberRetentionOnCompleted() {
        ObservablePublish<Integer> source = (ObservablePublish<Integer>)Observable.just(1).publish();

        TestObserverEx<Integer> to1 = new TestObserverEx<>();

        source.subscribe(to1);

        to1.assertNoValues();
        to1.assertNoErrors();
        to1.assertNotComplete();

        source.connect();

        to1.assertValue(1);
        to1.assertNoErrors();
        to1.assertTerminated();

        assertEquals(0, source.current.get().get().length);
    }

    @Test
    public void nonNullConnection() {
        ConnectableObservable<Object> source = Observable.never().publish();

        assertNotNull(source.connect());
        assertNotNull(source.connect());
    }

    @Test
    public void noDisconnectSomeoneElse() {
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
        return ((ObservablePublish.PublishConnection<Object>)d).isDisposed();
    }

    @Test
    public void connectIsIdempotent() {
        final AtomicInteger calls = new AtomicInteger();
        Observable<Integer> source = Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> t) {
                t.onSubscribe(Disposable.empty());
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
    public void observeOn() {
        ConnectableObservable<Integer> co = Observable.range(0, 1000).publish();
        Observable<Integer> obs = co.observeOn(Schedulers.computation());
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestObserverEx<Integer>> tos = new ArrayList<>();
                for (int k = 1; k < j; k++) {
                    TestObserverEx<Integer> to = new TestObserverEx<>();
                    tos.add(to);
                    obs.subscribe(to);
                }

                Disposable connection = co.connect();

                for (TestObserverEx<Integer> to : tos) {
                    to.awaitDone(2, TimeUnit.SECONDS);
                    to.assertTerminated();
                    to.assertNoErrors();
                    assertEquals(1000, to.values().size());
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

            final TestObserver<Integer> to2 = new TestObserver<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to2);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
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
                    to.dispose();
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
                    observer.onSubscribe(Disposable.empty());
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
        ConnectableObservable<Object> co = Observable.error(new TestException()).publish();

        co.connect();

        // 3.x: terminal events remain observable until reset
        co.test()
        .assertFailure(TestException.class);
    }

    @Test
    public void subscribeDisconnectRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final ConnectableObservable<Integer> co = ps.publish();

            final Disposable d = co.connect();
            final TestObserver<Integer> to = new TestObserver<>();

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

    @Test
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

        Disposable bs = Disposable.empty();

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
            new ObservablePublish<>(Observable.<Integer>empty())
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
                    new ObservablePublish<>(Observable.<Integer>never());

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    co.connect();
                }
            };

            TestHelper.race(r, r);
        }
    }

    @Test
    public void onCompleteAvailableUntilReset() {
        ConnectableObservable<Integer> co = Observable.just(1).publish();

        TestObserver<Integer> to = co.test();
        to.assertEmpty();

        co.connect();

        to.assertResult(1);

        co.test().assertResult();

        co.reset();

        to = co.test();
        to.assertEmpty();

        co.connect();

        to.assertResult(1);
    }

    @Test
    public void onErrorAvailableUntilReset() {
        ConnectableObservable<Integer> co = Observable.just(1)
                .concatWith(Observable.<Integer>error(new TestException()))
                .publish();

        TestObserver<Integer> to = co.test();
        to.assertEmpty();

        co.connect();

        to.assertFailure(TestException.class, 1);

        co.test().assertFailure(TestException.class);

        co.reset();

        to = co.test();
        to.assertEmpty();

        co.connect();

        to.assertFailure(TestException.class, 1);
    }

    @Test
    public void disposeResets() {
        PublishSubject<Integer> ps = PublishSubject.create();

        ConnectableObservable<Integer> co = ps.publish();

        assertFalse(ps.hasObservers());

        Disposable d = co.connect();

        assertTrue(ps.hasObservers());

        d.dispose();

        assertFalse(ps.hasObservers());

        TestObserver<Integer> to = co.test();

        co.connect();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to.assertValuesOnly(1);
    }
}
