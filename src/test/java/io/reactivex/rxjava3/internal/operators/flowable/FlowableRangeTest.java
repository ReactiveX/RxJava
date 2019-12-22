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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableRangeTest extends RxJavaTest {

    @Test
    public void rangeStartAt2Count3() {
        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        Flowable.range(2, 3).subscribe(subscriber);

        verify(subscriber, times(1)).onNext(2);
        verify(subscriber, times(1)).onNext(3);
        verify(subscriber, times(1)).onNext(4);
        verify(subscriber, never()).onNext(5);
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void rangeUnsubscribe() {
        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        final AtomicInteger count = new AtomicInteger();

        Flowable.range(1, 1000).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t1) {
                count.incrementAndGet();
            }
        })
        .take(3).subscribe(subscriber);

        verify(subscriber, times(1)).onNext(1);
        verify(subscriber, times(1)).onNext(2);
        verify(subscriber, times(1)).onNext(3);
        verify(subscriber, never()).onNext(4);
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
        assertEquals(3, count.get());
    }

    @Test
    public void rangeWithZero() {
        Flowable.range(1, 0);
    }

    @Test
    public void rangeWithOverflow2() {
        Flowable.range(Integer.MAX_VALUE, 0);
    }

    @Test
    public void rangeWithOverflow3() {
        Flowable.range(1, Integer.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rangeWithOverflow4() {
        Flowable.range(2, Integer.MAX_VALUE);
    }

    @Test
    public void rangeWithOverflow5() {
        assertFalse(Flowable.range(Integer.MIN_VALUE, 0).blockingIterable().iterator().hasNext());
    }

    @Test
    public void backpressureViaRequest() {
        Flowable<Integer> f = Flowable.range(1, Flowable.bufferSize());

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0L);

        ts.assertNoValues();
        ts.request(1);

        f.subscribe(ts);

        ts.assertValue(1);

        ts.request(2);
        ts.assertValues(1, 2, 3);

        ts.request(3);
        ts.assertValues(1, 2, 3, 4, 5, 6);

        ts.request(Flowable.bufferSize());
        ts.assertTerminated();
    }

    @Test
    public void noBackpressure() {
        ArrayList<Integer> list = new ArrayList<>(Flowable.bufferSize() * 2);
        for (int i = 1; i <= Flowable.bufferSize() * 2 + 1; i++) {
            list.add(i);
        }

        Flowable<Integer> f = Flowable.range(1, list.size());

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0L);

        ts.assertNoValues();
        ts.request(Long.MAX_VALUE); // infinite

        f.subscribe(ts);

        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    void withBackpressureOneByOne(int start) {
        Flowable<Integer> source = Flowable.range(start, 100);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0L);
        ts.request(1);
        source.subscribe(ts);

        List<Integer> list = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            list.add(i + start);
            ts.request(1);
        }
        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    void withBackpressureAllAtOnce(int start) {
        Flowable<Integer> source = Flowable.range(start, 100);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0L);
        ts.request(100);
        source.subscribe(ts);

        List<Integer> list = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            list.add(i + start);
        }
        ts.assertValueSequence(list);
        ts.assertTerminated();
    }

    @Test
    public void withBackpressure1() {
        for (int i = 0; i < 100; i++) {
            withBackpressureOneByOne(i);
        }
    }

    @Test
    public void withBackpressureAllAtOnce() {
        for (int i = 0; i < 100; i++) {
            withBackpressureAllAtOnce(i);
        }
    }

    @Test
    public void withBackpressureRequestWayMore() {
        Flowable<Integer> source = Flowable.range(50, 100);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0L);
        ts.request(150);
        source.subscribe(ts);

        List<Integer> list = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            list.add(i + 50);
        }

        ts.request(50); // and then some

        ts.assertValueSequence(list);
        ts.assertTerminated();
    }

    @Test
    public void requestOverflow() {
        final AtomicInteger count = new AtomicInteger();
        int n = 10;
        Flowable.range(1, n).subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onStart() {
                request(2);
            }

            @Override
            public void onComplete() {
                //do nothing
            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }

            @Override
            public void onNext(Integer t) {
                count.incrementAndGet();
                request(Long.MAX_VALUE - 1);
            }});
        assertEquals(n, count.get());
    }

    @Test
    public void emptyRangeSendsOnCompleteEagerlyWithRequestZero() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Flowable.range(1, 0).subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onStart() {
//                request(0);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {

            }});
        assertTrue(completed.get());
    }

    @Test
    public void nearMaxValueWithoutBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Flowable.range(Integer.MAX_VALUE - 1, 2).subscribe(ts);

        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValues(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
    }

    @Test
    public void nearMaxValueWithBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(3L);
        Flowable.range(Integer.MAX_VALUE - 1, 2).subscribe(ts);

        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValues(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
    }

    @Test
    public void negativeCount() {
        try {
            Flowable.range(1, -1);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("count >= 0 required but it was -1", ex.getMessage());
        }
    }

    @Test
    public void requestWrongFusion() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ASYNC);

        Flowable.range(1, 5)
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void countOne() {
        Flowable.range(5495454, 1)
            .test()
            .assertResult(5495454);
    }

    @Test
    public void fused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 2).subscribe(ts);

        ts.assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2);
    }

    @Test
    public void fusedReject() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ASYNC);

        Flowable.range(1, 2).subscribe(ts);

        ts.assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Flowable.range(1, 2));
    }

    @Test
    public void fusedClearIsEmpty() {
        TestHelper.checkFusedIsEmptyClear(Flowable.range(1, 2));
    }

    @Test
    public void noOverflow() {
        Flowable.range(Integer.MAX_VALUE - 1, 2);
        Flowable.range(Integer.MIN_VALUE, 2);
        Flowable.range(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void conditionalNormal() {
        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.range(1, 5));

        TestHelper.assertBadRequestReported(Flowable.range(1, 5).filter(Functions.alwaysTrue()));
    }

    @Test
    public void conditionalNormalSlowpath() {
        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .test(5)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void conditionalSlowPathTakeExact() {
        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void slowPathTakeExact() {
        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void conditionalSlowPathRebatch() {
        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void slowPathRebatch() {
        Flowable.range(1, 5)
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void slowPathCancel() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(2L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.range(1, 5)
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void fastPathCancel() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.range(1, 5)
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void conditionalSlowPathCancel() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void conditionalFastPathCancel() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void conditionalRequestOneByOne() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                request(1);
            }
        };

        Flowable.range(1, 5)
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        ts.assertResult(2, 4);
    }

    @Test
    public void conditionalRequestOneByOne2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                request(1);
            }
        };

        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fastPathCancelExact() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 5L) {
                    cancel();
                    onComplete();
                }
            }
        };

        Flowable.range(1, 5)
        .subscribe(ts);

        ts.assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void conditionalFastPathCancelExact() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 5L) {
                    cancel();
                    onComplete();
                }
            }
        };

        Flowable.range(1, 5)
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        ts.assertResult(2, 4);
    }

    @Test
    public void conditionalCancel1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(2L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    cancel();
                    onComplete();
                }
            }
        };

        Flowable.range(1, 2)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void conditionalCancel2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(2L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 2) {
                    cancel();
                    onComplete();
                }
            }
        };

        Flowable.range(1, 2)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertResult(1, 2);
    }
}
