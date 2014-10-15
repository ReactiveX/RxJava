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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OnSubscribeRefCountTest {

    @Test
    public void testRefCountAsync() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger nextCount = new AtomicInteger();
        Observable<Long> r = Observable.timer(0, 1, TimeUnit.MILLISECONDS)
                .doOnSubscribe(new Action0() {

                    @Override
                    public void call() {
                        subscribeCount.incrementAndGet();
                    }

                })
                .doOnNext(new Action1<Long>() {

                    @Override
                    public void call(Long l) {
                        nextCount.incrementAndGet();
                    }

                })
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        Subscription s1 = r.subscribe(new Action1<Long>() {

            @Override
            public void call(Long l) {
                receivedCount.incrementAndGet();
            }

        });
        Subscription s2 = r.subscribe();

        // give time to emit
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }

        // now unsubscribe
        s2.unsubscribe(); // unsubscribe s2 first as we're counting in 1 and there can be a race between unsubscribe and one subscriber getting a value but not the other
        s1.unsubscribe();

        System.out.println("onNext: " + nextCount.get());

        // should emit once for both subscribers
        assertEquals(nextCount.get(), receivedCount.get());
        // only 1 subscribe
        assertEquals(1, subscribeCount.get());
    }

    @Test
    public void testRefCountSynchronous() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger nextCount = new AtomicInteger();
        Observable<Integer> r = Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .doOnSubscribe(new Action0() {

                    @Override
                    public void call() {
                        subscribeCount.incrementAndGet();
                    }

                })
                .doOnNext(new Action1<Integer>() {

                    @Override
                    public void call(Integer l) {
                        nextCount.incrementAndGet();
                    }

                })
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        Subscription s1 = r.subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer l) {
                receivedCount.incrementAndGet();
            }

        });
        Subscription s2 = r.subscribe();

        // give time to emit
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }

        // now unsubscribe
        s2.unsubscribe(); // unsubscribe s2 first as we're counting in 1 and there can be a race between unsubscribe and one subscriber getting a value but not the other
        s1.unsubscribe();

        System.out.println("onNext: " + nextCount.get());

        // it will emit twice because it is synchronous
        assertEquals(nextCount.get(), receivedCount.get() * 2);
        // it will subscribe twice because it is synchronous
        assertEquals(2, subscribeCount.get());
    }

    @Test
    public void testRefCountSynchronousTake() {
        final AtomicInteger nextCount = new AtomicInteger();
        Observable<Integer> r = Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .doOnNext(new Action1<Integer>() {

                    @Override
                    public void call(Integer l) {
                        System.out.println("onNext --------> " + l);
                        nextCount.incrementAndGet();
                    }

                })
                .take(4)
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        r.subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer l) {
                receivedCount.incrementAndGet();
            }

        });

        System.out.println("onNext: " + nextCount.get());

        assertEquals(4, receivedCount.get());
        assertEquals(4, receivedCount.get());
    }

    @Test
    public void testRepeat() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger unsubscribeCount = new AtomicInteger();
        Observable<Long> r = Observable.timer(0, 1, TimeUnit.MILLISECONDS)
                .doOnSubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* Subscribe received");
                        // when we are subscribed
                        subscribeCount.incrementAndGet();
                    }

                })
                .doOnUnsubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* Unsubscribe received");
                        // when we are unsubscribed
                        unsubscribeCount.incrementAndGet();
                    }

                })
                .publish().refCount();

        for (int i = 0; i < 10; i++) {
            TestSubscriber<Long> ts1 = new TestSubscriber<Long>();
            TestSubscriber<Long> ts2 = new TestSubscriber<Long>();
            r.subscribe(ts1);
            r.subscribe(ts2);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            ts1.unsubscribe();
            ts2.unsubscribe();
            ts1.assertNoErrors();
            ts2.assertNoErrors();
            assertTrue(ts1.getOnNextEvents().size() > 0);
            assertTrue(ts2.getOnNextEvents().size() > 0);
        }

        assertEquals(10, subscribeCount.get());
        assertEquals(10, unsubscribeCount.get());
    }

    @Test
    public void testConnectUnsubscribe() throws InterruptedException {
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);
        Observable<Long> o = synchronousInterval()
                .doOnSubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* Subscribe received");
                        // when we are subscribed
                        subscribeLatch.countDown();
                    }

                })
                .doOnUnsubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* Unsubscribe received");
                        // when we are unsubscribed
                        unsubscribeLatch.countDown();
                    }

                });
        TestSubscriber<Long> s = new TestSubscriber<Long>();
        o.publish().refCount().subscribeOn(Schedulers.newThread()).subscribe(s);
        System.out.println("send unsubscribe");
        // wait until connected
        subscribeLatch.await();
        // now unsubscribe
        s.unsubscribe();
        System.out.println("DONE sending unsubscribe ... now waiting");
        if (!unsubscribeLatch.await(3000, TimeUnit.MILLISECONDS)) {
            System.out.println("Errors: " + s.getOnErrorEvents());
            if (s.getOnErrorEvents().size() > 0) {
                s.getOnErrorEvents().get(0).printStackTrace();
            }
            fail("timed out waiting for unsubscribe");
        }
        s.assertNoErrors();
    }

    @Test
    public void testConnectUnsubscribeRaceCondition() throws InterruptedException {
        final AtomicInteger subUnsubCount = new AtomicInteger();
        Observable<Long> o = synchronousInterval()
                .doOnUnsubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* Unsubscribe received");
                        // when we are unsubscribed
                        subUnsubCount.decrementAndGet();
                    }

                })
                .doOnSubscribe(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("******************************* SUBSCRIBE received");
                        subUnsubCount.incrementAndGet();
                    }

                });

        TestSubscriber<Long> s = new TestSubscriber<Long>();
        o.publish().refCount().subscribeOn(Schedulers.newThread()).subscribe(s);
        System.out.println("send unsubscribe");
        // now immediately unsubscribe while subscribeOn is racing to subscribe
        s.unsubscribe();
        // this generally will mean it won't even subscribe as it is already unsubscribed by the time connect() gets scheduled

        // either we subscribed and then unsubscribed, or we didn't ever even subscribe
        assertEquals(0, subUnsubCount.get());

        System.out.println("DONE sending unsubscribe ... now waiting");
        System.out.println("Errors: " + s.getOnErrorEvents());
        if (s.getOnErrorEvents().size() > 0) {
            s.getOnErrorEvents().get(0).printStackTrace();
        }
        s.assertNoErrors();
    }

    private Observable<Long> synchronousInterval() {
        return Observable.create(new OnSubscribe<Long>() {

            @Override
            public void call(Subscriber<? super Long> subscriber) {
                while (!subscriber.isUnsubscribed()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    subscriber.onNext(1L);
                }
            }
        });
    }

    @Test
    public void testConcurrency() {

    }
}
