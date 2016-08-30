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

import static io.reactivex.BackpressureOverflowStrategy.DROP_OLDEST;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.Flowable;
import io.reactivex.functions.Action;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DefaultSubscriber;
import io.reactivex.subscribers.TestSubscriber;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class FlowableOnBackpressureBufferStrategyTest {

    private static Action onOverFlow = new Action() {
        @Override
        public void run() throws Exception {
            // Nothing
        }
    };


    @Test(timeout = 2000)
    public void testFixBackpressureWithBuffer() throws InterruptedException {
        final CountDownLatch l1 = new CountDownLatch(100);
        final CountDownLatch l2 = new CountDownLatch(150);
        final AtomicInteger droppedCount = new AtomicInteger(0);
        Action incrementOnDrop = new Action() {
            @Override
            public void run() throws Exception {
                droppedCount.incrementAndGet();
            }
        };
        TestSubscriber<Long> ts = new TestSubscriber<Long>(new DefaultSubscriber<Long>() {

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

        }, 0L);
        // this will be ignored
        ts.request(100);
        // we take 500 so it unsubscribes
        Flowable.fromPublisher(infinite.subscribeOn(Schedulers.computation())
        .onBackpressureBuffer(1, incrementOnDrop , DROP_OLDEST))
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
        assertEquals(499 + droppedCount.get(), ts.values().get(499).intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFixBackpressureBufferNegativeCapacity() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(-1, onOverFlow , DROP_OLDEST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFixBackpressureBufferZeroCapacity() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(0, onOverFlow , DROP_OLDEST);
    }


    static final Flowable<Long> infinite = Flowable.unsafeCreate(new Publisher<Long>() {

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


    @Test(expected = IllegalArgumentException.class)
    public void fixBackpressureBufferNegativeCapacity() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(-1, onOverFlow , DROP_OLDEST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fixBackpressureBufferZeroCapacity() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(0, onOverFlow , DROP_OLDEST);
    }

}