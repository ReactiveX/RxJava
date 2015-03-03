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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OperatorOnBackpressureBufferTest {

    @Test
    public void testNoBackpressureSupport() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>();
        // this will be ignored
        ts.requestMore(100);
        // we take 500 so it unsubscribes
        infinite.take(500).subscribe(ts);
        // it completely ignores the `request(100)` and we get 500
        assertEquals(500, ts.getOnNextEvents().size());
        ts.assertNoErrors();
    }

    @Test(timeout = 2000)
    public void testFixBackpressureWithBuffer() throws InterruptedException {
        final CountDownLatch l1 = new CountDownLatch(100);
        final CountDownLatch l2 = new CountDownLatch(150);
        TestSubscriber<Long> ts = new TestSubscriber<Long>(new Observer<Long>() {

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Long t) {
                l1.countDown();
                l2.countDown();
            }

        });
        // this will be ignored
        ts.requestMore(100);
        // we take 500 so it unsubscribes
        infinite.subscribeOn(Schedulers.computation()).onBackpressureBuffer().take(500).subscribe(ts);
        // it completely ignores the `request(100)` and we get 500
        l1.await();
        assertEquals(100, ts.getOnNextEvents().size());
        ts.requestMore(50);
        l2.await();
        assertEquals(150, ts.getOnNextEvents().size());
        ts.requestMore(350);
        ts.awaitTerminalEvent();
        assertEquals(500, ts.getOnNextEvents().size());
        ts.assertNoErrors();
        assertEquals(0, ts.getOnNextEvents().get(0).intValue());
        assertEquals(499, ts.getOnNextEvents().get(499).intValue());
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
        TestSubscriber<Long> ts = new TestSubscriber<Long>(new Observer<Long>() {

            @Override
            public void onCompleted() { }

            @Override
            public void onError(Throwable e) { }

            @Override
            public void onNext(Long t) {
                l1.countDown();
            }

        });

        ts.requestMore(100);
        Subscription s = infinite.subscribeOn(Schedulers.computation())
                                 .onBackpressureBuffer(500, new Action0() {
                                     @Override
                                     public void call() {
                                         backpressureCallback.countDown();
                                     }
                                 }).take(1000).subscribe(ts);
        l1.await();

        ts.requestMore(50);

        assertTrue(backpressureCallback.await(500, TimeUnit.MILLISECONDS));
        assertTrue(ts.getOnErrorEvents().get(0) instanceof MissingBackpressureException);

        int size = ts.getOnNextEvents().size();
        assertTrue(size <= 150);  // will get up to 50 more
        assertTrue(ts.getOnNextEvents().get(size-1) == size-1);
        assertTrue(s.isUnsubscribed());
    }

    static final Observable<Long> infinite = Observable.create(new OnSubscribe<Long>() {

        @Override
        public void call(Subscriber<? super Long> s) {
            long i = 0;
            while (!s.isUnsubscribed()) {
                s.onNext(i++);
            }
        }

    });

}
