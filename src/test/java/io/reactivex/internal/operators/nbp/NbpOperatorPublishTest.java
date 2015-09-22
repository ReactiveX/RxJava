/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.Test;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observables.nbp.NbpConnectableObservable;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorPublishTest {

    @Test
    public void testPublish() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        NbpConnectableObservable<String> o = NbpObservable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(final NbpSubscriber<? super String> NbpObserver) {
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
        NbpConnectableObservable<Integer> is = NbpObservable.range(1, Observable.bufferSize() * 2).publish();
        NbpObservable<Integer> fast = is.observeOn(Schedulers.computation())
        .doOnComplete(() -> System.out.println("^^^^^^^^^^^^^ completed FAST"));

        NbpObservable<Integer> slow = is.observeOn(Schedulers.computation()).map(new Function<Integer, Integer>() {
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

        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.merge(fast, slow).subscribe(ts);
        is.connect();
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Observable.bufferSize() * 4, ts.valueCount());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void testTakeUntilWithPublishedStreamUsingSelector() {
        final AtomicInteger emitted = new AtomicInteger();
        NbpObservable<Integer> xs = NbpObservable.range(0, Observable.bufferSize() * 2).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                emitted.incrementAndGet();
            }

        });
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        xs.publish(new Function<NbpObservable<Integer>, NbpObservable<Integer>>() {

            @Override
            public NbpObservable<Integer> apply(NbpObservable<Integer> xs) {
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
        NbpObservable<Integer> xs = NbpObservable.range(0, Observable.bufferSize() * 2);
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpConnectableObservable<Integer> xsp = xs.publish();
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
        final NbpObservable<Integer> source = NbpObservable.range(1, 100)
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

        final NbpTestSubscriber<Integer> ts2 = new NbpTestSubscriber<>();

        final NbpTestSubscriber<Integer> ts1 = new NbpTestSubscriber<Integer>() {
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
        NbpConnectableObservable<Long> co = NbpObservable.interval(10, 10, TimeUnit.MILLISECONDS, scheduler).take(3).publish();
        co.connect();
        // Emit 0
        scheduler.advanceTimeBy(15, TimeUnit.MILLISECONDS);
        NbpTestSubscriber<Long> NbpSubscriber = new NbpTestSubscriber<>();
        co.subscribe(NbpSubscriber);
        // Emit 1 and 2
        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        NbpSubscriber.assertValues(1L, 2L);
        NbpSubscriber.assertNoErrors();
        NbpSubscriber.assertTerminated();
    }
    
    @Test
    public void testSubscribeAfterDisconnectThenConnect() {
        NbpConnectableObservable<Integer> source = NbpObservable.just(1).publish();

        NbpTestSubscriber<Integer> ts1 = new NbpTestSubscriber<>();

        source.subscribe(ts1);

        Disposable s = source.connect();

        ts1.assertValue(1);
        ts1.assertNoErrors();
        ts1.assertTerminated();

        NbpTestSubscriber<Integer> ts2 = new NbpTestSubscriber<>();

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
        NbpOperatorPublish<Integer> source = (NbpOperatorPublish<Integer>)NbpObservable.just(1).publish();

        NbpTestSubscriber<Integer> ts1 = new NbpTestSubscriber<>();

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
        NbpConnectableObservable<Object> source = NbpObservable.never().publish();
        
        assertNotNull(source.connect());
        assertNotNull(source.connect());
    }
    
    @Test
    public void testNoDisconnectSomeoneElse() {
        NbpConnectableObservable<Object> source = NbpObservable.never().publish();

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
        NbpObservable<Integer> source = NbpObservable.create(new NbpOnSubscribe<Integer>() {
            @Override
            public void accept(NbpSubscriber<? super Integer> t) {
                t.onSubscribe(EmptyDisposable.INSTANCE);
                calls.getAndIncrement();
            }
        });
        
        NbpConnectableObservable<Integer> conn = source.publish();

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
        NbpConnectableObservable<Integer> co = NbpObservable.range(0, 1000).publish();
        NbpObservable<Integer> obs = co.observeOn(Schedulers.computation());
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<NbpTestSubscriber<Integer>> tss = new ArrayList<>();
                for (int k = 1; k < j; k++) {
                    NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
                    tss.add(ts);
                    obs.subscribe(ts);
                }
                
                Disposable s = co.connect();
                
                for (NbpTestSubscriber<Integer> ts : tss) {
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