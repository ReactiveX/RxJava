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

import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorOnBackpressureBufferTest {

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

    @Test(timeout = 2000)
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
        infinite.subscribeOn(Schedulers.computation())
        .onBackpressureBuffer()
        .take(500)
        .subscribe(ts);
        
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
        assertEquals(499, ts.values().get(499).intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFixBackpressureBufferNegativeCapacity() throws InterruptedException {
        Observable.empty().onBackpressureBuffer(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFixBackpressureBufferZeroCapacity() throws InterruptedException {
        Observable.empty().onBackpressureBuffer(0);
    }

    @Test
    public void testFixBackpressureBoundedBuffer() throws InterruptedException {
        final CountDownLatch l1 = new CountDownLatch(100);
        final CountDownLatch backpressureCallback = new CountDownLatch(1);
        TestSubscriber<Long> ts = new TestSubscriber<>(new Observer<Long>() {

            @Override
            protected void onStart() {
            }
            
            @Override
            public void onComplete() { }

            @Override
            public void onError(Throwable e) { }

            @Override
            public void onNext(Long t) {
                l1.countDown();
            }

        }, null);

        ts.request(100);
        infinite.subscribeOn(Schedulers.computation())
             .onBackpressureBuffer(500, new Runnable() {
                 @Override
                 public void run() {
                     backpressureCallback.countDown();
                 }
             })
             /*.take(1000)*/
             .subscribe(ts);
        l1.await();

        ts.request(50);

        assertTrue(backpressureCallback.await(500, TimeUnit.MILLISECONDS));
        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertError(MissingBackpressureException.class);

        int size = ts.values().size();
        assertTrue(size <= 150);  // will get up to 50 more
        assertTrue(ts.values().get(size-1) == size-1);
        // FIXME no longer assertable
//        assertTrue(s.isUnsubscribed());
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

}