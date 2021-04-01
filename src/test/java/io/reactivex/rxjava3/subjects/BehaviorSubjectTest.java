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

package io.reactivex.rxjava3.subjects;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.mockito.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject.BehaviorDisposable;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class BehaviorSubjectTest extends SubjectTest<Integer> {

    private final Throwable testException = new Throwable();

    @Override
    protected Subject<Integer> create() {
        return BehaviorSubject.create();
    }

    @Test
    public void thatSubscriberReceivesDefaultValueAndSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(testException);
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void thatSubscriberReceivesLatestAndThenSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        subject.onNext("one");

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext("two");
        subject.onNext("three");

        verify(observer, Mockito.never()).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(testException);
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void subscribeThenOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onComplete();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void subscribeToCompletedOnlyEmitsOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");
        subject.onNext("one");
        subject.onComplete();

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        verify(observer, never()).onNext("default");
        verify(observer, never()).onNext("one");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void subscribeToErrorOnlyEmitsOnError() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");
        subject.onNext("one");
        RuntimeException re = new RuntimeException("test error");
        subject.onError(re);

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        verify(observer, never()).onNext("default");
        verify(observer, never()).onNext("one");
        verify(observer, times(1)).onError(re);
        verify(observer, never()).onComplete();
    }

    @Test
    public void completedStopsEmittingData() {
        BehaviorSubject<Integer> channel = BehaviorSubject.createDefault(2013);
        Observer<Object> observerA = TestHelper.mockObserver();
        Observer<Object> observerB = TestHelper.mockObserver();
        Observer<Object> observerC = TestHelper.mockObserver();

        TestObserver<Object> to = new TestObserver<>(observerA);

        channel.subscribe(to);
        channel.subscribe(observerB);

        InOrder inOrderA = inOrder(observerA);
        InOrder inOrderB = inOrder(observerB);
        InOrder inOrderC = inOrder(observerC);

        inOrderA.verify(observerA).onNext(2013);
        inOrderB.verify(observerB).onNext(2013);

        channel.onNext(42);

        inOrderA.verify(observerA).onNext(42);
        inOrderB.verify(observerB).onNext(42);

        to.dispose();
        inOrderA.verifyNoMoreInteractions();

        channel.onNext(4711);

        inOrderB.verify(observerB).onNext(4711);

        channel.onComplete();

        inOrderB.verify(observerB).onComplete();

        channel.subscribe(observerC);

        inOrderC.verify(observerC).onComplete();

        channel.onNext(13);

        inOrderB.verifyNoMoreInteractions();
        inOrderC.verifyNoMoreInteractions();
    }

    @Test
    public void completedAfterErrorIsNotSent() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onComplete();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onError(testException);
        verify(observer, never()).onNext("two");
        verify(observer, never()).onComplete();
    }

    @Test
    public void completedAfterErrorIsNotSent2() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onComplete();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onError(testException);
        verify(observer, never()).onNext("two");
        verify(observer, never()).onComplete();

        Observer<Object> o2 = TestHelper.mockObserver();
        subject.subscribe(o2);
        verify(o2, times(1)).onError(testException);
        verify(o2, never()).onNext(any());
        verify(o2, never()).onComplete();
    }

    @Test
    public void completedAfterErrorIsNotSent3() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onComplete();
        subject.onNext("two");
        subject.onComplete();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext("two");

        Observer<Object> o2 = TestHelper.mockObserver();
        subject.subscribe(o2);
        verify(o2, times(1)).onComplete();
        verify(o2, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void unsubscriptionCase() {
        BehaviorSubject<String> src = BehaviorSubject.createDefault("null"); // FIXME was plain null which is not allowed

        for (int i = 0; i < 10; i++) {
            final Observer<Object> o = TestHelper.mockObserver();
            InOrder inOrder = inOrder(o);
            String v = "" + i;
            src.onNext(v);
            System.out.printf("Turn: %d%n", i);
            src.firstElement()
                .toObservable()
                .flatMap(new Function<String, Observable<String>>() {

                    @Override
                    public Observable<String> apply(String t1) {
                        return Observable.just(t1 + ", " + t1);
                    }
                })
                .subscribe(new DefaultObserver<String>() {
                    @Override
                    public void onNext(String t) {
                        o.onNext(t);
                    }

                    @Override
                    public void onError(Throwable e) {
                        o.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        o.onComplete();
                    }
                });
            inOrder.verify(o).onNext(v + ", " + v);
            inOrder.verify(o).onComplete();
            verify(o, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void startEmpty() {
        BehaviorSubject<Integer> source = BehaviorSubject.create();
        final Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.subscribe(o);

        inOrder.verify(o, never()).onNext(any());
        inOrder.verify(o, never()).onComplete();

        source.onNext(1);

        source.onComplete();

        source.onNext(2);

        verify(o, never()).onError(any(Throwable.class));

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void startEmptyThenAddOne() {
        BehaviorSubject<Integer> source = BehaviorSubject.create();
        final Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.onNext(1);

        source.subscribe(o);

        inOrder.verify(o).onNext(1);

        source.onComplete();

        source.onNext(2);

        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void startEmptyCompleteWithOne() {
        BehaviorSubject<Integer> source = BehaviorSubject.create();
        final Observer<Object> o = TestHelper.mockObserver();

        source.onNext(1);
        source.onComplete();

        source.onNext(2);

        source.subscribe(o);

        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onNext(any());
    }

    @Test
    public void takeOneSubscriber() {
        BehaviorSubject<Integer> source = BehaviorSubject.createDefault(1);
        final Observer<Object> o = TestHelper.mockObserver();

        source.take(1).subscribe(o);

        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));

        assertEquals(0, source.subscriberCount());
        assertFalse(source.hasObservers());
    }

    @Test
    public void emissionSubscriptionRace() throws Exception {
        Scheduler s = Schedulers.io();
        Scheduler.Worker worker = Schedulers.io().createWorker();
        try {
            for (int i = 0; i < 50000; i++) {
                if (i % 1000 == 0) {
                    System.out.println(i);
                }
                final BehaviorSubject<Object> rs = BehaviorSubject.create();

                final CountDownLatch finish = new CountDownLatch(1);
                final CountDownLatch start = new CountDownLatch(1);

                worker.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            start.await();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        rs.onNext(1);
                    }
                });

                final AtomicReference<Object> o = new AtomicReference<>();

                rs.subscribeOn(s).observeOn(Schedulers.io())
                .subscribe(new DefaultObserver<Object>() {

                    @Override
                    public void onComplete() {
                        o.set(-1);
                        finish.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        o.set(e);
                        finish.countDown();
                    }

                    @Override
                    public void onNext(Object t) {
                        o.set(t);
                        finish.countDown();
                    }

                });
                start.countDown();

                if (!finish.await(5, TimeUnit.SECONDS)) {
                    System.out.println(o.get());
                    System.out.println(rs.hasObservers());
                    rs.onComplete();
                    Assert.fail("Timeout @ " + i);
                    break;
                } else {
                    Assert.assertEquals(1, o.get());
                    worker.schedule(new Runnable() {
                        @Override
                        public void run() {
                            rs.onComplete();
                        }
                    });
                }
            }
        } finally {
            worker.dispose();
        }
    }

    @Test
    public void currentStateMethodsNormalEmptyStart() {
        BehaviorSubject<Object> as = BehaviorSubject.create();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());

        as.onNext(1);

        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertEquals(1, as.getValue());
        assertNull(as.getThrowable());

        as.onComplete();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
    }

    @Test
    public void currentStateMethodsNormalSomeStart() {
        BehaviorSubject<Object> as = BehaviorSubject.createDefault((Object)1);

        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertEquals(1, as.getValue());
        assertNull(as.getThrowable());

        as.onNext(2);

        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertEquals(2, as.getValue());
        assertNull(as.getThrowable());

        as.onComplete();
        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
    }

    @Test
    public void currentStateMethodsEmpty() {
        BehaviorSubject<Object> as = BehaviorSubject.create();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());

        as.onComplete();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
    }

    @Test
    public void currentStateMethodsError() {
        BehaviorSubject<Object> as = BehaviorSubject.create();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());

        as.onError(new TestException());

        assertFalse(as.hasValue());
        assertTrue(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertTrue(as.getThrowable() instanceof TestException);
    }

    @Test
    public void cancelOnArrival() {
        BehaviorSubject<Object> p = BehaviorSubject.create();

        assertFalse(p.hasObservers());

        p.test(true).assertEmpty();

        assertFalse(p.hasObservers());
    }

    @Test
    public void onSubscribe() {
        BehaviorSubject<Object> p = BehaviorSubject.create();

        Disposable bs = Disposable.empty();

        p.onSubscribe(bs);

        assertFalse(bs.isDisposed());

        p.onComplete();

        bs = Disposable.empty();

        p.onSubscribe(bs);

        assertTrue(bs.isDisposed());
    }

    @Test
    public void onErrorAfterComplete() {
        BehaviorSubject<Object> p = BehaviorSubject.create();

        p.onComplete();

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            p.onError(new TestException());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelOnArrival2() {
        BehaviorSubject<Object> p = BehaviorSubject.create();

        TestObserver<Object> to = p.test();

        p.test(true).assertEmpty();

        p.onNext(1);
        p.onComplete();

        to.assertResult(1);
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final BehaviorSubject<Object> p = BehaviorSubject.create();

            final TestObserver<Object> to = p.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    p.test();
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void subscribeOnNextRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final BehaviorSubject<Object> p = BehaviorSubject.createDefault((Object)1);

            final TestObserver[] to = { null };

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to[0] = p.test();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    p.onNext(2);
                }
            };

            TestHelper.race(r1, r2);

            if (to[0].values().size() == 1) {
                to[0].assertValue(2).assertNoErrors().assertNotComplete();
            } else {
                to[0].assertValues(1, 2).assertNoErrors().assertNotComplete();
            }
        }
    }

    @Test
    public void innerDisposed() {
        BehaviorSubject.create()
        .subscribe(new Observer<Object>() {
            @Override
            public void onSubscribe(Disposable d) {
                assertFalse(d.isDisposed());

                d.dispose();

                assertTrue(d.isDisposed());
            }

            @Override
            public void onNext(Object value) {

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
    public void completeSubscribeRace() throws Exception {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final BehaviorSubject<Object> p = BehaviorSubject.create();

            final TestObserver<Object> to = new TestObserver<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    p.subscribe(to);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    p.onComplete();
                }
            };

            TestHelper.race(r1, r2);

            to.assertResult();
        }
    }

    @Test
    public void errorSubscribeRace() throws Exception {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final BehaviorSubject<Object> p = BehaviorSubject.create();

            final TestObserver<Object> to = new TestObserver<>();

            final TestException ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    p.subscribe(to);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    p.onError(ex);
                }
            };

            TestHelper.race(r1, r2);

            to.assertFailure(TestException.class);
        }
    }

    @Test
    public void behaviorDisposableDisposeState() {
        BehaviorSubject<Integer> bs = BehaviorSubject.create();
        bs.onNext(1);

        TestObserver<Integer> to = new TestObserver<>();

        BehaviorDisposable<Integer> bd = new BehaviorDisposable<>(to, bs);
        to.onSubscribe(bd);

        assertFalse(bd.isDisposed());

        bd.dispose();

        assertTrue(bd.isDisposed());

        bd.dispose();

        assertTrue(bd.isDisposed());

        assertTrue(bd.test(2));

        bd.emitFirst();

        to.assertEmpty();

        bd.emitNext(2, 0);
    }

    @Test
    public void emitFirstDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            BehaviorSubject<Integer> bs = BehaviorSubject.create();
            bs.onNext(1);

            TestObserver<Integer> to = new TestObserver<>();

            final BehaviorDisposable<Integer> bd = new BehaviorDisposable<>(to, bs);
            to.onSubscribe(bd);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    bd.emitFirst();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    bd.dispose();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void emitNextDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            BehaviorSubject<Integer> bs = BehaviorSubject.create();
            bs.onNext(1);

            TestObserver<Integer> to = new TestObserver<>();

            final BehaviorDisposable<Integer> bd = new BehaviorDisposable<>(to, bs);
            to.onSubscribe(bd);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    bd.emitNext(2, 0);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    bd.dispose();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void emittingEmitNext() {
        BehaviorSubject<Integer> bs = BehaviorSubject.create();
        bs.onNext(1);

        TestObserver<Integer> to = new TestObserver<>();

        final BehaviorDisposable<Integer> bd = new BehaviorDisposable<>(to, bs);
        to.onSubscribe(bd);

        bd.emitting = true;
        bd.emitNext(2, 1);
        bd.emitNext(3, 2);

        assertNotNull(bd.queue);
    }

    @Test
    public void hasObservers() {
        BehaviorSubject<Integer> bs = BehaviorSubject.create();

        assertFalse(bs.hasObservers());

        TestObserver<Integer> to = bs.test();

        assertTrue(bs.hasObservers());

        to.dispose();

        assertFalse(bs.hasObservers());
    }
}
