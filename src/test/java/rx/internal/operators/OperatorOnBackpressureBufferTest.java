/**
 * Copyright 2016 Netflix, Inc.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static rx.BackpressureOverflow.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

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

    @Test(expected = NullPointerException.class)
    public void testFixBackpressureBufferNullStrategy() throws InterruptedException {
        Observable.empty().onBackpressureBuffer(10, new Action0() {
            @Override
            public void call() { }
        }, null);
    }

    @Test
    public void testFixBackpressureBoundedBuffer() throws InterruptedException {
        final CountDownLatch l1 = new CountDownLatch(100);
        final CountDownLatch backpressureCallback = new CountDownLatch(1);
        final TestSubscriber<Long> ts = testSubscriber(l1);

        ts.requestMore(100);
        Subscription s = infinite.subscribeOn(Schedulers.computation())
                                 .onBackpressureBuffer(500, new Action0() {
                                     @Override
                                     public void call() {
                                         backpressureCallback.countDown();
                                     }
                                 }).take(1000).subscribe(ts);
        assertTrue(l1.await(2, TimeUnit.SECONDS));

        ts.requestMore(50);

        assertTrue(backpressureCallback.await(2, TimeUnit.SECONDS));
        assertTrue(ts.getOnErrorEvents().get(0) instanceof MissingBackpressureException);

        int size = ts.getOnNextEvents().size();
        assertTrue(size <= 150);  // will get up to 50 more
        assertTrue(ts.getOnNextEvents().get(size-1) == size-1);
        assertTrue(s.isUnsubscribed());
    }

    @Test
    public void testFixBackpressureBoundedBufferDroppingOldest()
        throws InterruptedException {
        List<Long> events = overflowBufferWithBehaviour(100, 10, ON_OVERFLOW_DROP_OLDEST);

        // The consumer takes 100 initial elements, then 10 are temporarily
        // buffered and the oldest (100, 101, etc.) are dropped to make room for
        // higher items.
        int i = 0;
        for (Long n : events) {
            if (i < 100) {  // backpressure is expected to kick in after the
                            // initial batch is consumed
                assertEquals(Long.valueOf(i), n);
            } else {
                assertTrue(i < n);
            }
            i++;
        }
    }

    @Test
    public void testFixBackpressureBoundedBufferDroppingLatest()
        throws InterruptedException {

        List<Long> events = overflowBufferWithBehaviour(100, 10, ON_OVERFLOW_DROP_LATEST);

        // The consumer takes 100 initial elements, then 10 are temporarily
        // buffered and the newest are dropped to make room for higher items.
        int i = 0;
        for (Long n : events) {
            if (i < 110) {
                assertEquals(Long.valueOf(i), n);
            } else {
                assertTrue(i < n);
            }
            i++;
        }
    }

    private List<Long> overflowBufferWithBehaviour(int initialRequest, int bufSize,
                                                   BackpressureOverflow.Strategy backpressureStrategy)
        throws InterruptedException {

        final CountDownLatch l1 = new CountDownLatch(initialRequest * 2);
        final CountDownLatch backpressureCallback = new CountDownLatch(1);

        final TestSubscriber<Long> ts = testSubscriber(l1);

        ts.requestMore(initialRequest);
        Subscription s = infinite.subscribeOn(Schedulers.computation())
            .onBackpressureBuffer(bufSize, new Action0() {
                                      @Override
                                      public void call() {
                                          backpressureCallback.countDown();
                                      }
                                  }, backpressureStrategy
            ).subscribe(ts);

        assertTrue(backpressureCallback.await(2, TimeUnit.SECONDS));

        ts.requestMore(initialRequest);

        assertTrue(l1.await(2, TimeUnit.SECONDS));

        // Stop receiving elements
        s.unsubscribe();

        // No failure despite overflows
        assertTrue(ts.getOnErrorEvents().isEmpty());
        assertEquals(initialRequest * 2, ts.getOnNextEvents().size());

        assertTrue(ts.isUnsubscribed());

        return ts.getOnNextEvents();
    }

    static <T> TestSubscriber<T> testSubscriber(final CountDownLatch latch) {
        return new TestSubscriber<T>(new Observer<T>() {

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(T t) {
                latch.countDown();
            }
        });
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
    
    private static final Action0 THROWS_NON_FATAL = new Action0() {

        @Override
        public void call() {
            throw new RuntimeException();
        }}; 
    
    @Test
    public void testNonFatalExceptionThrownByOnOverflowIsNotReportedByUpstream() {
         final AtomicBoolean errorOccurred = new AtomicBoolean(false);
         TestSubscriber<Long> ts = TestSubscriber.create(0);
         infinite
           .subscribeOn(Schedulers.computation())
           .doOnError(new Action1<Throwable>() {
                 @Override
                 public void call(Throwable t) {
                     errorOccurred.set(true);
                 }
             })
           .onBackpressureBuffer(1, THROWS_NON_FATAL)
           .subscribe(ts);
         ts.awaitTerminalEvent();
         assertFalse(errorOccurred.get());
    }

}
