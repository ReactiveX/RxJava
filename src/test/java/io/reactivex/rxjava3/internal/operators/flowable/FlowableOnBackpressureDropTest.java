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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableOnBackpressureDropTest extends RxJavaTest {

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
    public void withObserveOn() throws InterruptedException {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Flowable.range(0, Flowable.bufferSize() * 10).onBackpressureDrop().observeOn(Schedulers.io()).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
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
        infinite.subscribeOn(Schedulers.computation()).onBackpressureDrop().take(500).subscribe(ts);
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
    }

    @Test
    public void requestOverflow() throws InterruptedException {
        final AtomicInteger count = new AtomicInteger();
        int n = 10;
        range(n).onBackpressureDrop().subscribe(new DefaultSubscriber<Long>() {

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
                request(Long.MAX_VALUE - 1);
            }});
        assertEquals(n, count.get());
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

    private static Flowable<Long> range(final long n) {
        return Flowable.unsafeCreate(new Publisher<Long>() {

            @Override
            public void subscribe(Subscriber<? super Long> s) {
                BooleanSubscription bs = new BooleanSubscription();
                s.onSubscribe(bs);
                for (long i = 0; i < n; i++) {
                    if (bs.isCancelled()) {
                        break;
                    }
                    s.onNext(i);
                }
                s.onComplete();
            }

        });
    }

    private static final Consumer<Long> THROW_NON_FATAL = new Consumer<Long>() {
        @Override
        public void accept(Long n) {
            throw new RuntimeException();
        }
    };

    @Test
    public void nonFatalExceptionFromOverflowActionIsNotReportedFromUpstreamOperator() {
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);
        //request 0
        TestSubscriber<Long> ts = TestSubscriber.create(0);
        //range method emits regardless of requests so should trigger onBackpressureDrop action
        range(2)
          // if haven't caught exception in onBackpressureDrop operator then would incorrectly
          // be picked up by this call to doOnError
          .doOnError(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) {
                    errorOccurred.set(true);
                }
            })
          .onBackpressureDrop(THROW_NON_FATAL)
          .subscribe(ts);

        assertFalse(errorOccurred.get());
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.onBackpressureDrop();
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.onBackpressureDrop();
            }
        });
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.just(1).onBackpressureDrop());
    }
}
