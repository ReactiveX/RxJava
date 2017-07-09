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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.observers.*;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.*;

public class FlowableIgnoreElementsTest {

    @Test
    public void testWithEmptyFlowable() {
        assertTrue(Flowable.empty().ignoreElements().toFlowable().isEmpty().blockingGet());
    }

    @Test
    public void testWithNonEmptyFlowable() {
        assertTrue(Flowable.just(1, 2, 3).ignoreElements().toFlowable().isEmpty().blockingGet());
    }

    @Test
    public void testUpstreamIsProcessedButIgnoredFlowable() {
        final int num = 10;
        final AtomicInteger upstreamCount = new AtomicInteger();
        long count = Flowable.range(1, num)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                .ignoreElements()
                .toFlowable()
                .count().blockingGet();
        assertEquals(num, upstreamCount.get());
        assertEquals(0, count);
    }

    @Test
    public void testCompletedOkFlowable() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        Flowable.range(1, 10).ignoreElements().toFlowable().subscribe(ts);
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertTerminated();
    }

    @Test
    public void testErrorReceivedFlowable() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        TestException ex = new TestException("boo");
        Flowable.error(ex).ignoreElements().toFlowable().subscribe(ts);
        ts.assertNoValues();
        ts.assertTerminated();
        ts.assertError(TestException.class);
        ts.assertErrorMessage("boo");
    }

    @Test
    public void testUnsubscribesFromUpstreamFlowable() {
        final AtomicBoolean unsub = new AtomicBoolean();
        Flowable.range(1, 10).concatWith(Flowable.<Integer>never())
        .doOnCancel(new Action() {
            @Override
            public void run() {
                unsub.set(true);
            }})
            .ignoreElements()
            .toFlowable()
            .subscribe().dispose();

        assertTrue(unsub.get());
    }

    @Test(timeout = 10000)
    public void testDoesNotHangAndProcessesAllUsingBackpressureFlowable() {
        final AtomicInteger upstreamCount = new AtomicInteger();
        final AtomicInteger count = new AtomicInteger(0);
        int num = 10;
        Flowable.range(1, num)
        //
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                //
                .ignoreElements()
                .<Integer>toFlowable()
                //
                .doOnNext(new Consumer<Integer>() {

                    @Override
                    public void accept(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                //
                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Integer t) {
                        count.incrementAndGet();
                    }
                });
        assertEquals(num, upstreamCount.get());
        assertEquals(0, count.get());
    }


    @Test
    public void testWithEmpty() {
        assertNull(Flowable.empty().ignoreElements().blockingGet());
    }

    @Test
    public void testWithNonEmpty() {
        assertNull(Flowable.just(1, 2, 3).ignoreElements().blockingGet());
    }

    @Test
    public void testUpstreamIsProcessedButIgnored() {
        final int num = 10;
        final AtomicInteger upstreamCount = new AtomicInteger();
        Object count = Flowable.range(1, num)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                .ignoreElements()
                .blockingGet();
        assertEquals(num, upstreamCount.get());
        assertNull(count);
    }

    @Test
    public void testCompletedOk() {
        TestObserver<Object> ts = new TestObserver<Object>();
        Flowable.range(1, 10).ignoreElements().subscribe(ts);
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertTerminated();
    }

    @Test
    public void testErrorReceived() {
        TestObserver<Object> ts = new TestObserver<Object>();
        TestException ex = new TestException("boo");
        Flowable.error(ex).ignoreElements().subscribe(ts);
        ts.assertNoValues();
        ts.assertTerminated();
        ts.assertError(TestException.class);
        ts.assertErrorMessage("boo");
    }

    @Test
    public void testUnsubscribesFromUpstream() {
        final AtomicBoolean unsub = new AtomicBoolean();
        Flowable.range(1, 10).concatWith(Flowable.<Integer>never())
        .doOnCancel(new Action() {
            @Override
            public void run() {
                unsub.set(true);
            }})
            .ignoreElements()
            .subscribe().dispose();

        assertTrue(unsub.get());
    }

    @Test(timeout = 10000)
    public void testDoesNotHangAndProcessesAllUsingBackpressure() {
        final AtomicInteger upstreamCount = new AtomicInteger();
        final AtomicInteger count = new AtomicInteger(0);
        int num = 10;
        Flowable.range(1, num)
        //
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                //
                .ignoreElements()
                //
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }
                });
        assertEquals(num, upstreamCount.get());
        assertEquals(0, count.get());
    }

    @Test
    public void cancel() {

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.ignoreElements().<Integer>toFlowable().test();

        assertTrue(pp.hasSubscribers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void fused() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

        Flowable.just(1).hide().ignoreElements().<Integer>toFlowable()
        .subscribe(ts);

        ts.assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC))
        .assertResult();
    }

    @Test
    public void fusedAPICalls() {
        Flowable.just(1).hide().ignoreElements().<Integer>toFlowable()
        .subscribe(new FlowableSubscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription s) {
                @SuppressWarnings("unchecked")
                QueueSubscription<Integer> qs = (QueueSubscription<Integer>)s;

                try {
                    assertNull(qs.poll());
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }

                assertTrue(qs.isEmpty());

                qs.clear();

                assertTrue(qs.isEmpty());

                try {
                    assertNull(qs.poll());
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }

                try {
                    qs.offer(1);
                    fail("Should have thrown!");
                } catch (UnsupportedOperationException ex) {
                    // expected
                }

                try {
                    qs.offer(1, 2);
                    fail("Should have thrown!");
                } catch (UnsupportedOperationException ex) {
                    // expected
                }
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).ignoreElements());

        TestHelper.checkDisposed(Flowable.just(1).ignoreElements().toFlowable());
    }
}
