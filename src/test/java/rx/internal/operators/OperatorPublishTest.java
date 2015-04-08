/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.functions.*;
import rx.internal.util.RxRingBuffer;
import rx.observables.ConnectableObservable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

public class OperatorPublishTest {

    @Test
    public void testPublish() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        ConnectableObservable<String> o = Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(final Subscriber<? super String> observer) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        observer.onNext("one");
                        observer.onCompleted();
                    }
                }).start();
            }
        }).publish();

        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Action1<String>() {

            @Override
            public void call(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        // subscribe again
        o.subscribe(new Action1<String>() {

            @Override
            public void call(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        Subscription s = o.connect();
        try {
            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                fail("subscriptions did not receive values");
            }
            assertEquals(1, counter.get());
        } finally {
            s.unsubscribe();
        }
    }

    @Test
    public void testBackpressureFastSlow() {
        ConnectableObservable<Integer> is = Observable.range(1, RxRingBuffer.SIZE * 2).publish();
        Observable<Integer> fast = is.observeOn(Schedulers.computation()).doOnCompleted(new Action0() {

            @Override
            public void call() {
                System.out.println("^^^^^^^^^^^^^ completed FAST");
            }

        });
        Observable<Integer> slow = is.observeOn(Schedulers.computation()).map(new Func1<Integer, Integer>() {
            int c = 0;

            @Override
            public Integer call(Integer i) {
                if (c == 0) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
                c++;
                return i;
            }

        }).doOnCompleted(new Action0() {

            @Override
            public void call() {
                System.out.println("^^^^^^^^^^^^^ completed SLOW");
            }

        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.merge(fast, slow).subscribe(ts);
        is.connect();
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(RxRingBuffer.SIZE * 4, ts.getOnNextEvents().size());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void testTakeUntilWithPublishedStreamUsingSelector() {
        final AtomicInteger emitted = new AtomicInteger();
        Observable<Integer> xs = Observable.range(0, RxRingBuffer.SIZE * 2).doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer t1) {
                emitted.incrementAndGet();
            }

        });
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        xs.publish(new Func1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Observable<Integer> xs) {
                return xs.takeUntil(xs.skipWhile(new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer i) {
                        return i <= 3;
                    }

                }));
            }

        }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(0, 1, 2, 3));
        assertEquals(5, emitted.get());
        System.out.println(ts.getOnNextEvents());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void testTakeUntilWithPublishedStream() {
        Observable<Integer> xs = Observable.range(0, RxRingBuffer.SIZE * 2);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ConnectableObservable<Integer> xsp = xs.publish();
        xsp.takeUntil(xsp.skipWhile(new Func1<Integer, Boolean>() {

            @Override
            public Boolean call(Integer i) {
                return i <= 3;
            }

        })).subscribe(ts);
        xsp.connect();
        System.out.println(ts.getOnNextEvents());
    }

    @Test(timeout = 10000)
    public void testBackpressureTwoConsumers() {
        final AtomicInteger sourceEmission = new AtomicInteger();
        final AtomicBoolean sourceUnsubscribed = new AtomicBoolean();
        final Observable<Integer> source = Observable.range(1, 100)
                .doOnNext(new Action1<Integer>() {
                    @Override
                    public void call(Integer t1) {
                        sourceEmission.incrementAndGet();
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
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
                if (getOnNextEvents().size() == 2) {
                    source.doOnUnsubscribe(new Action0() {
                        @Override
                        public void call() {
                            child2Unsubscribed.set(true);
                        }
                    }).take(5).subscribe(ts2);
                }
                super.onNext(t);
            }
        };
        
        source.doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                child1Unsubscribed.set(true);
            }
        }).take(5).subscribe(ts1);
        
        ts1.awaitTerminalEvent();
        ts2.awaitTerminalEvent();
        
        ts1.assertNoErrors();
        ts2.assertNoErrors();
        
        assertTrue(sourceUnsubscribed.get());
        assertTrue(child1Unsubscribed.get());
        assertTrue(child2Unsubscribed.get());
        
        ts1.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5));
        ts2.assertReceivedOnNext(Arrays.asList(4, 5, 6, 7, 8));
        
        assertEquals(8, sourceEmission.get());
    }

    @Test
    public void testConnectWithNoSubscriber() {
        TestScheduler scheduler = new TestScheduler();
        ConnectableObservable<Long> co = Observable.timer(10, 10, TimeUnit.MILLISECONDS, scheduler).take(3).publish();
        co.connect();
        // Emit 0
        scheduler.advanceTimeBy(15, TimeUnit.MILLISECONDS);
        TestSubscriber<Long> subscriber = new TestSubscriber<Long>();
        co.subscribe(subscriber);
        // Emit 1 and 2
        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        subscriber.assertReceivedOnNext(Arrays.asList(1L, 2L));
        subscriber.assertNoErrors();
        subscriber.assertTerminalEvent();
    }
}
