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

package io.reactivex.internal.operators;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorOnBackpressureDropTest {

    @Test
    public void testNoBackpressureSupport() {
        TestSubscriber<Long> ts = new TestSubscriber<>((Long)null);
        // this will be ignored
        ts.request(100);
        // we take 500 so it unsubscribes
        infinite.take(500).subscribe(ts);
        // it completely ignores the `request(100)` and we get 500
        assertEquals(500, ts.values().size());
        ts.assertNoErrors();
    }

    @Test(timeout = 500)
    public void testWithObserveOn() throws InterruptedException {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Observable.range(0, Observable.bufferSize() * 10).onBackpressureDrop().observeOn(Schedulers.io()).subscribe(ts);
        ts.awaitTerminalEvent();
    }

    @Test(timeout = 500)
    public void testFixBackpressureWithBuffer() throws InterruptedException {
        final CountDownLatch l1 = new CountDownLatch(100);
        final CountDownLatch l2 = new CountDownLatch(150);
        TestSubscriber<Long> ts = new TestSubscriber<>(new Observer<Long>() {

            @Override
            protected void onStart() {
            }
            
            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Long t) {
                l1.countDown();
                l2.countDown();
            }

        }, null);
        // this will be ignored
        ts.request(100);
        // we take 500 so it unsubscribes
        infinite.subscribeOn(Schedulers.computation()).onBackpressureDrop().take(500).subscribe(ts);
        // it completely ignores the `request(100)` and we get 500
        l1.await();
        assertEquals(100, ts.values().size());
        ts.request(50);
        l2.await();
        assertEquals(150, ts.values().size());
        ts.request(350);
        ts.awaitTerminalEvent();
        assertEquals(500, ts.values().size());
        ts.assertNoErrors();
        assertEquals(0, ts.values().get(0).intValue());
    }
    
    @Test
    public void testRequestOverflow() throws InterruptedException {
        final AtomicInteger count = new AtomicInteger();
        int n = 10;
        range(n).onBackpressureDrop().subscribe(new Observer<Long>() {

            @Override
            public void onStart() {
                request(10);
            }
            
            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }

            @Override
            public void onNext(Long t) {
                count.incrementAndGet();
                //cause overflow of requested if not handled properly in onBackpressureDrop operator
                request(Long.MAX_VALUE-1);
            }});
        assertEquals(n, count.get());
    }

    static final Observable<Long> infinite = Observable.create(new Publisher<Long>() {

        @Override
        public void subscribe(Subscriber<? super Long> s) {
            BooleanSubscription bs = new BooleanSubscription();
            s.onSubscribe(bs);
            long i = 0;
            while (!bs.isCancelled()) {
                s.onNext(i++);
            }
        }

    });
    
    private static final Observable<Long> range(final long n) {
        return Observable.create(new Publisher<Long>() {

            @Override
            public void subscribe(Subscriber<? super Long> s) {
                BooleanSubscription bs = new BooleanSubscription();
                s.onSubscribe(bs);
                for (long i=0;i < n; i++) {
                    if (bs.isCancelled()) {
                        break;
                    }
                    s.onNext(i);
                }
                s.onComplete();
            }
    
        });
    }
    
}