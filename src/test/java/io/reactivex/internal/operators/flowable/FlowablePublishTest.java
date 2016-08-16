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

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowablePublishTest {

    @Test
    public void testPublish() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        ConnectableFlowable<String> o = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(final Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
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
        ConnectableFlowable<Integer> is = Flowable.range(1, Flowable.bufferSize() * 2).publish();
        Flowable<Integer> fast = is.observeOn(Schedulers.computation())
        .doOnComplete(new Action() {
            @Override
            public void run() {
                System.out.println("^^^^^^^^^^^^^ completed FAST");
            }
        });

        Flowable<Integer> slow = is.observeOn(Schedulers.computation()).map(new Function<Integer, Integer>() {
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

        }).doOnComplete(new Action() {

            @Override
            public void run() {
                System.out.println("^^^^^^^^^^^^^ completed SLOW");
            }

        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.merge(fast, slow).subscribe(ts);
        is.connect();
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 4, ts.valueCount());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void testTakeUntilWithPublishedStreamUsingSelector() {
        final AtomicInteger emitted = new AtomicInteger();
        Flowable<Integer> xs = Flowable.range(0, Flowable.bufferSize() * 2).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                emitted.incrementAndGet();
            }

        });
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        xs.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Flowable<Integer> xs) {
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
        Flowable<Integer> xs = Flowable.range(0, Flowable.bufferSize() * 2);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ConnectableFlowable<Integer> xsp = xs.publish();
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
        final Flowable<Integer> source = Flowable.range(1, 100)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t1) {
                        sourceEmission.incrementAndGet();
                    }
                })
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                        sourceUnsubscribed.set(true);
                    }
                }).share();
        ;
        
        final AtomicBoolean child1Unsubscribed = new AtomicBoolean();
        final AtomicBoolean child2Unsubscribed = new AtomicBoolean();

        final TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();

        final TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (valueCount() == 2) {
                    source.doOnCancel(new Action() {
                        @Override
                        public void run() {
                            child2Unsubscribed.set(true);
                        }
                    }).take(5).subscribe(ts2);
                }
                super.onNext(t);
            }
        };
        
        source.doOnCancel(new Action() {
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
        ConnectableFlowable<Long> co = Flowable.interval(10, 10, TimeUnit.MILLISECONDS, scheduler).take(3).publish();
        co.connect();
        // Emit 0
        scheduler.advanceTimeBy(15, TimeUnit.MILLISECONDS);
        TestSubscriber<Long> subscriber = new TestSubscriber<Long>();
        co.subscribe(subscriber);
        // Emit 1 and 2
        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        subscriber.assertValues(1L, 2L);
        subscriber.assertNoErrors();
        subscriber.assertTerminated();
    }
    
    @Test
    public void testSubscribeAfterDisconnectThenConnect() {
        ConnectableFlowable<Integer> source = Flowable.just(1).publish();

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();

        source.subscribe(ts1);

        Disposable s = source.connect();

        ts1.assertValue(1);
        ts1.assertNoErrors();
        ts1.assertTerminated();

        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();

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
        FlowablePublish<Integer> source = (FlowablePublish<Integer>)Flowable.just(1).publish();

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();

        source.subscribe(ts1);

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
        ConnectableFlowable<Object> source = Flowable.never().publish();
        
        assertNotNull(source.connect());
        assertNotNull(source.connect());
    }
    
    @Test
    public void testNoDisconnectSomeoneElse() {
        ConnectableFlowable<Object> source = Flowable.never().publish();

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
        return ((FlowablePublish.PublishSubscriber<Object>)d).isDisposed();
    }
    
    @Test
    public void testZeroRequested() {
        ConnectableFlowable<Integer> source = Flowable.just(1).publish();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);
        
        source.subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        source.connect();

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ts.request(5);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertTerminated();
    }
    @Test
    public void testConnectIsIdempotent() {
        final AtomicInteger calls = new AtomicInteger();
        Flowable<Integer> source = Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> t) {
                t.onSubscribe(new BooleanSubscription());
                calls.getAndIncrement();
            }
        });
        
        ConnectableFlowable<Integer> conn = source.publish();

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
        ConnectableFlowable<Integer> co = Flowable.range(0, 1000).publish();
        Flowable<Integer> obs = co.observeOn(Schedulers.computation());
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestSubscriber<Integer>> tss = new ArrayList<TestSubscriber<Integer>>();
                for (int k = 1; k < j; k++) {
                    TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
                    tss.add(ts);
                    obs.subscribe(ts);
                }
                
                Disposable s = co.connect();
                
                for (TestSubscriber<Integer> ts : tss) {
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