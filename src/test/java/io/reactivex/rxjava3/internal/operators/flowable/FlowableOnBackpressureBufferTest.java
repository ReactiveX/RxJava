/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableOnBackpressureBufferTest extends RxJavaTest {

    @Test
    public void noBackpressureSupport() {
        TestSubscriber<Long> ts = new TestSubscriber<>(0L);
        // this will be ignored
        ts.request(100);
        // we take 500 so it unsubscribes
        infinite.take(500).subscribe(ts);
        // it completely ignores the `request(100)` and we get 500
        assertEquals(500, ts.values().size());
        ts.assertNoErrors();
    }

    @Test
    public void fixBackpressureWithBuffer() throws InterruptedException {
        final CountDownLatch l1 = new CountDownLatch(100);
        final CountDownLatch l2 = new CountDownLatch(150);
        TestSubscriber<Long> ts = new TestSubscriber<>(new DefaultSubscriber<Long>() {

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
        ts.awaitDone(5, TimeUnit.SECONDS);
        assertEquals(500, ts.values().size());
        ts.assertNoErrors();
        assertEquals(0, ts.values().get(0).intValue());
        assertEquals(499, ts.values().get(499).intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fixBackpressureBufferNegativeCapacity() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fixBackpressureBufferZeroCapacity() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(0);
    }

    @Test
    public void fixBackpressureBoundedBuffer() throws InterruptedException {
        final CountDownLatch l1 = new CountDownLatch(100);
        final CountDownLatch backpressureCallback = new CountDownLatch(1);
        TestSubscriber<Long> ts = new TestSubscriber<>(new DefaultSubscriber<Long>() {

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
            }

        }, 0L);

        ts.request(100);
        infinite.subscribeOn(Schedulers.computation())
             .onBackpressureBuffer(500, new Action() {
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
        ts.awaitDone(1, TimeUnit.SECONDS);
        ts.assertError(MissingBackpressureException.class);

        int size = ts.values().size();
        assertTrue(size <= 150);  // will get up to 50 more
        assertEquals((long)ts.values().get(size - 1), size - 1);
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

    private static final Action THROWS_NON_FATAL = new Action() {

        @Override
        public void run() {
            throw new RuntimeException();
        }
    };

    @Test
    public void nonFatalExceptionThrownByOnOverflowIsNotReportedByUpstream() {
         final AtomicBoolean errorOccurred = new AtomicBoolean(false);
         TestSubscriber<Long> ts = TestSubscriber.create(0);
         infinite
           .subscribeOn(Schedulers.computation())
           .doOnError(new Consumer<Throwable>() {
                 @Override
                 public void accept(Throwable t) {
                     errorOccurred.set(true);
                 }
             })
           .onBackpressureBuffer(1, THROWS_NON_FATAL)
           .subscribe(ts);
         ts.awaitDone(5, TimeUnit.SECONDS);
         assertFalse(errorOccurred.get());
    }

    @Test
    public void maxSize() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Flowable.range(1, 10).onBackpressureBuffer(1).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotComplete();
    }

    @Test(expected = IllegalArgumentException.class)
    public void fixBackpressureBufferNegativeCapacity2() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fixBackpressureBufferZeroCapacity2() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(0);
    }

    @Test(expected = NullPointerException.class)
    public void fixBackpressureBufferNullStrategy() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(10, new Action() {
            @Override
            public void run() { }
        }, null);
    }

    @Test
    public void noDelayError() {

        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .onBackpressureBuffer(false)
        .test(0L)
        .assertFailure(TestException.class);
    }

    @Test
    public void delayError() {
        TestSubscriber<Integer> ts = Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .onBackpressureBuffer(true)
        .test(0L)
        .assertEmpty();

        ts.request(1);
        ts.assertFailure(TestException.class, 1);

    }

    @Test
    public void delayErrorBuffer() {
        TestSubscriber<Integer> ts = Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .onBackpressureBuffer(16, true)
        .test(0L)
        .assertEmpty();

        ts.request(1);
        ts.assertFailure(TestException.class, 1);
    }

    @Test
    public void fusedNormal() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 10).onBackpressureBuffer().subscribe(ts);

        ts.assertFuseable()
          .assertFusionMode(QueueFuseable.ASYNC)
          .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void fusedError() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.<Integer>error(new TestException()).onBackpressureBuffer().subscribe(ts);

        ts.assertFuseable()
          .assertFusionMode(QueueFuseable.ASYNC)
          .assertFailure(TestException.class);
    }

    @Test
    public void fusedPreconsume() throws Exception {

        TestSubscriber<Integer> ts = Flowable.range(1, 1000 * 1000)
        .onBackpressureBuffer()
        .observeOn(Schedulers.single())
        .test(0L);

        ts.assertEmpty();

        Thread.sleep(100);

        ts.request(1000 * 1000);

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1000 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void emptyDelayError() {
        Flowable.empty()
        .onBackpressureBuffer(true)
        .test()
        .assertResult();
    }

    @Test
    public void fusionRejected() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.SYNC);

        Flowable.<Integer>never().onBackpressureBuffer().subscribe(ts);

        ts.assertFusionMode(QueueFuseable.NONE)
        .assertEmpty();
    }

    @Test
    public void fusedNoConcurrentCleanDueToCancel() {
        for (int j = 0; j < TestHelper.RACE_LONG_LOOPS; j++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp = PublishProcessor.create();

                TestObserver<Integer> to = pp.onBackpressureBuffer(4, false, true)
                .observeOn(Schedulers.io())
                .map(Functions.<Integer>identity())
                .observeOn(Schedulers.single())
                .firstOrError()
                .test();

                for (int i = 0; pp.hasSubscribers(); i++) {
                    pp.onNext(i);
                }

                to
                .awaitDone(5, TimeUnit.SECONDS)
                ;

                if (!errors.isEmpty()) {
                    throw new CompositeException(errors);
                }

                to.assertResult(0);
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }
}
