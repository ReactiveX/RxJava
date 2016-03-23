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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observable.NbpOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.*;

public class NbpOperatorPublishTest {

    @Test
    public void testPublish() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        ConnectableObservable<String> o = Observable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(final Observer<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        NbpObserver.onNext("one");
                        NbpObserver.onComplete();
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

        Disposable s = o.connect();
        try {
            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                fail("subscriptions did not receive values");
            }
            assertEquals(1, counter.get());
        } finally {
            s.dispose();
        }
    }

    @Test
    public void testBackpressureFastSlow() {
        ConnectableObservable<Integer> is = Observable.range(1, Flowable.bufferSize() * 2).publish();
        Observable<Integer> fast = is.observeOn(Schedulers.computation())
        .doOnComplete(new Runnable() {
            @Override
            public void run() {
                System.out.println("^^^^^^^^^^^^^ completed FAST");
            }
        });

        Observable<Integer> slow = is.observeOn(Schedulers.computation()).map(new Function<Integer, Integer>() {
            int c = 0;

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

        }).doOnComplete(new Runnable() {

            @Override
            public void run() {
                System.out.println("^^^^^^^^^^^^^ completed SLOW");
            }

        });

        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.merge(fast, slow).subscribe(ts);
        is.connect();
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 4, ts.valueCount());
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
        TestObserver<Integer> ts = new TestObserver<Integer>();
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

        }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        ts.assertValues(0, 1, 2, 3);
        assertEquals(5, emitted.get());
        System.out.println(ts.values());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void testTakeUntilWithPublishedStream() {
        Observable<Integer> xs = Observable.range(0, Flowable.bufferSize() * 2);
        TestObserver<Integer> ts = new TestObserver<Integer>();
        ConnectableObservable<Integer> xsp = xs.publish();
        xsp.takeUntil(xsp.skipWhile(new Predicate<Integer>() {

            @Override
            public boolean test(Integer i) {
                return i <= 3;
            }

        })).subscribe(ts);
        xsp.connect();
        System.out.println(ts.values());
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
                .doOnCancel(new Runnable() {
                    @Override
                    public void run() {
                        sourceUnsubscribed.set(true);
                    }
                }).share();
        ;
        
        final AtomicBoolean child1Unsubscribed = new AtomicBoolean();
        final AtomicBoolean child2Unsubscribed = new AtomicBoolean();

        final TestObserver<Integer> ts2 = new TestObserver<Integer>();

        final TestObserver<Integer> ts1 = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (valueCount() == 2) {
                    source.doOnCancel(new Runnable() {
                        @Override
                        public void run() {
                            child2Unsubscribed.set(true);
                        }
                    }).take(5).subscribe(ts2);
                }
                super.onNext(t);
            }
        };
        
        source.doOnCancel(new Runnable() {
            @Override
            public void run() {
                child1Unsubscribed.set(true);
            }
        }).take(5)
        .subscribe(ts1);
        
        ts1.awaitTerminalEvent();
        ts2.awaitTerminalEvent();
        
        ts1.assertNoErrors();
        ts2.assertNoErrors();
        
        assertTrue(sourceUnsubscribed.get());
        assertTrue(child1Unsubscribed.get());
        assertTrue(child2Unsubscribed.get());
        
        ts1.assertValues(1, 2, 3, 4, 5);
        ts2.assertValues(4, 5, 6, 7, 8);
        
        assertEquals(8, sourceEmission.get());
    }

    @Test
    public void testConnectWithNoSubscriber() {
        TestScheduler scheduler = new TestScheduler();
        ConnectableObservable<Long> co = Observable.interval(10, 10, TimeUnit.MILLISECONDS, scheduler).take(3).publish();
        co.connect();
        // Emit 0
        scheduler.advanceTimeBy(15, TimeUnit.MILLISECONDS);
        TestObserver<Long> NbpSubscriber = new TestObserver<Long>();
        co.subscribe(NbpSubscriber);
        // Emit 1 and 2
        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        NbpSubscriber.assertValues(1L, 2L);
        NbpSubscriber.assertNoErrors();
        NbpSubscriber.assertTerminated();
    }
    
    @Test
    public void testSubscribeAfterDisconnectThenConnect() {
        ConnectableObservable<Integer> source = Observable.just(1).publish();

        TestObserver<Integer> ts1 = new TestObserver<Integer>();

        source.subscribe(ts1);

        Disposable s = source.connect();

        ts1.assertValue(1);
        ts1.assertNoErrors();
        ts1.assertTerminated();

        TestObserver<Integer> ts2 = new TestObserver<Integer>();

        source.subscribe(ts2);

        Disposable s2 = source.connect();

        ts2.assertValue(1);
        ts2.assertNoErrors();
        ts2.assertTerminated();

        System.out.println(s);
        System.out.println(s2);
    }
    
    @Test
    public void testNoSubscriberRetentionOnCompleted() {
        NbpOperatorPublish<Integer> source = (NbpOperatorPublish<Integer>)Observable.just(1).publish();

        TestObserver<Integer> ts1 = new TestObserver<Integer>();

        source.unsafeSubscribe(ts1);

        ts1.assertNoValues();
        ts1.assertNoErrors();
        ts1.assertNotComplete();
        
        source.connect();

        ts1.assertValue(1);
        ts1.assertNoErrors();
        ts1.assertTerminated();

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

        Disposable s1 = source.connect();
        Disposable s2 = source.connect();
        
        s1.dispose();
        
        Disposable s3 = source.connect();
        
        s2.dispose();
        
        assertTrue(checkPublishDisposed(s1));
        assertTrue(checkPublishDisposed(s2));
        assertFalse(checkPublishDisposed(s3));
    }
    
    @SuppressWarnings("unchecked")
    static boolean checkPublishDisposed(Disposable d) {
        return ((NbpOperatorPublish.PublishSubscriber<Object>)d).isDisposed();
    }
    
    @Test
    public void testConnectIsIdempotent() {
        final AtomicInteger calls = new AtomicInteger();
        Observable<Integer> source = Observable.create(new NbpOnSubscribe<Integer>() {
            @Override
            public void accept(Observer<? super Integer> t) {
                t.onSubscribe(EmptyDisposable.INSTANCE);
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
                List<TestObserver<Integer>> tss = new ArrayList<TestObserver<Integer>>();
                for (int k = 1; k < j; k++) {
                    TestObserver<Integer> ts = new TestObserver<Integer>();
                    tss.add(ts);
                    obs.subscribe(ts);
                }
                
                Disposable s = co.connect();
                
                for (TestObserver<Integer> ts : tss) {
                    ts.awaitTerminalEvent(2, TimeUnit.SECONDS);
                    ts.assertTerminated();
                    ts.assertNoErrors();
                    assertEquals(1000, ts.valueCount());
                }
                s.dispose();
            }
        }
    }
}