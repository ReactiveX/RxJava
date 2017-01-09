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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.QueueDisposable;
import io.reactivex.subscribers.*;

public class FlowableRangeLongTest {

    @Test
    public void testRangeStartAt2Count3() {
        Subscriber<Long> observer = TestHelper.mockSubscriber();

        Flowable.rangeLong(2, 3).subscribe(observer);

        verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onNext(3L);
        verify(observer, times(1)).onNext(4L);
        verify(observer, never()).onNext(5L);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testRangeUnsubscribe() {
        Subscriber<Long> observer = TestHelper.mockSubscriber();

        final AtomicInteger count = new AtomicInteger();

        Flowable.rangeLong(1, 1000).doOnNext(new Consumer<Long>() {
            @Override
            public void accept(Long t1) {
                count.incrementAndGet();
            }
        })
        .take(3).subscribe(observer);

        verify(observer, times(1)).onNext(1L);
        verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onNext(3L);
        verify(observer, never()).onNext(4L);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        assertEquals(3, count.get());
    }

    @Test
    public void testRangeWithZero() {
        Flowable.rangeLong(1, 0);
    }

    @Test
    public void testRangeWithOverflow2() {
        Flowable.rangeLong(Long.MAX_VALUE, 0);
    }

    @Test
    public void testRangeWithOverflow3() {
        Flowable.rangeLong(1, Long.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeWithOverflow4() {
        Flowable.rangeLong(2, Long.MAX_VALUE);
    }

    @Test
    public void testRangeWithOverflow5() {
        assertFalse(Flowable.rangeLong(Long.MIN_VALUE, 0).blockingIterable().iterator().hasNext());
    }

    @Test
    public void testBackpressureViaRequest() {
        Flowable<Long> o = Flowable.rangeLong(1, Flowable.bufferSize());

        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);

        ts.assertNoValues();
        ts.request(1);

        o.subscribe(ts);

        ts.assertValue(1L);

        ts.request(2);
        ts.assertValues(1L, 2L, 3L);

        ts.request(3);
        ts.assertValues(1L, 2L, 3L, 4L, 5L, 6L);

        ts.request(Flowable.bufferSize());
        ts.assertTerminated();
    }

    @Test
    public void testNoBackpressure() {
        ArrayList<Long> list = new ArrayList<Long>(Flowable.bufferSize() * 2);
        for (long i = 1; i <= Flowable.bufferSize() * 2 + 1; i++) {
            list.add(i);
        }

        Flowable<Long> o = Flowable.rangeLong(1, list.size());

        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);

        ts.assertNoValues();
        ts.request(Long.MAX_VALUE); // infinite

        o.subscribe(ts);

        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    void testWithBackpressureOneByOne(long start) {
        Flowable<Long> source = Flowable.rangeLong(start, 100);

        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);
        ts.request(1);
        source.subscribe(ts);

        List<Long> list = new ArrayList<Long>(100);
        for (long i = 0; i < 100; i++) {
            list.add(i + start);
            ts.request(1);
        }
        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    void testWithBackpressureAllAtOnce(long start) {
        Flowable<Long> source = Flowable.rangeLong(start, 100);

        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);
        ts.request(100);
        source.subscribe(ts);

        List<Long> list = new ArrayList<Long>(100);
        for (long i = 0; i < 100; i++) {
            list.add(i + start);
        }
        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    @Test
    public void testWithBackpressure1() {
        for (long i = 0; i < 100; i++) {
            testWithBackpressureOneByOne(i);
        }
    }
    @Test
    public void testWithBackpressureAllAtOnce() {
        for (long i = 0; i < 100; i++) {
            testWithBackpressureAllAtOnce(i);
        }
    }
    @Test
    public void testWithBackpressureRequestWayMore() {
        Flowable<Long> source = Flowable.rangeLong(50, 100);

        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);
        ts.request(150);
        source.subscribe(ts);

        List<Long> list = new ArrayList<Long>(100);
        for (long i = 0; i < 100; i++) {
            list.add(i + 50);
        }

        ts.request(50); // and then some

        ts.assertValueSequence(list);
        ts.assertTerminated();
    }

    @Test
    public void testRequestOverflow() {
        final AtomicInteger count = new AtomicInteger();
        int n = 10;
        Flowable.rangeLong(1, n).subscribe(new DefaultSubscriber<Long>() {

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
            public void onNext(Long t) {
                count.incrementAndGet();
                request(Long.MAX_VALUE - 1);
            }});
        assertEquals(n, count.get());
    }

    @Test
    public void testEmptyRangeSendsOnCompleteEagerlyWithRequestZero() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Flowable.rangeLong(1, 0).subscribe(new DefaultSubscriber<Long>() {

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
            public void onNext(Long t) {

            }});
        assertTrue(completed.get());
    }

    @Test(timeout = 1000)
    public void testNearMaxValueWithoutBackpressure() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>();
        Flowable.rangeLong(Long.MAX_VALUE - 1L, 2L).subscribe(ts);

        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValues(Long.MAX_VALUE - 1L, Long.MAX_VALUE);
    }

    @Test(timeout = 1000)
    public void testNearMaxValueWithBackpressure() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>(3L);
        Flowable.rangeLong(Long.MAX_VALUE - 1L, 2L).subscribe(ts);

        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValues(Long.MAX_VALUE - 1L, Long.MAX_VALUE);
    }

    @Test
    public void negativeCount() {
        try {
            Flowable.rangeLong(1L, -1L);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("count >= 0 required but it was -1", ex.getMessage());
        }
    }

    @Test
    public void countOne() {
        Flowable.rangeLong(5495454L, 1L)
            .test()
            .assertResult(5495454L);
    }

    @Test
    public void fused() {
        TestSubscriber<Long> to = SubscriberFusion.newTest(QueueDisposable.ANY);

        Flowable.rangeLong(1, 2).subscribe(to);

        SubscriberFusion.assertFusion(to, QueueDisposable.SYNC)
        .assertResult(1L, 2L);
    }

    @Test
    public void fusedReject() {
        TestSubscriber<Long> to = SubscriberFusion.newTest(QueueDisposable.ASYNC);

        Flowable.rangeLong(1, 2).subscribe(to);

        SubscriberFusion.assertFusion(to, QueueDisposable.NONE)
        .assertResult(1L, 2L);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Flowable.rangeLong(1, 2));
    }

    @Test
    public void fusedClearIsEmpty() {
        TestHelper.checkFusedIsEmptyClear(Flowable.rangeLong(1, 2));
    }

    @Test
    public void noOverflow() {
        Flowable.rangeLong(Long.MAX_VALUE - 1, 2);
        Flowable.rangeLong(Long.MIN_VALUE, 2);
        Flowable.rangeLong(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @Test
    public void conditionalNormal() {
        Flowable.rangeLong(1L, 5L)
        .filter(Functions.alwaysTrue())
        .test()
        .assertResult(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.rangeLong(1L, 5L));

        TestHelper.assertBadRequestReported(Flowable.rangeLong(1L, 5L).filter(Functions.alwaysTrue()));
    }

    @Test
    public void conditionalNormalSlowpath() {
        Flowable.rangeLong(1L, 5L)
        .filter(Functions.alwaysTrue())
        .test(5)
        .assertResult(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    public void conditionalSlowPathTakeExact() {
        Flowable.rangeLong(1L, 5L)
        .filter(Functions.alwaysTrue())
        .take(5)
        .test()
        .assertResult(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    public void slowPathTakeExact() {
        Flowable.rangeLong(1L, 5L)
        .filter(Functions.alwaysTrue())
        .take(5)
        .test()
        .assertResult(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    public void conditionalSlowPathRebatch() {
        Flowable.rangeLong(1L, 5L)
        .filter(Functions.alwaysTrue())
        .rebatchRequests(1)
        .test()
        .assertResult(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    public void slowPathRebatch() {
        Flowable.rangeLong(1L, 5L)
        .rebatchRequests(1)
        .test()
        .assertResult(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    public void slowPathCancel() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>(2L) {
            @Override
            public void onNext(Long t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.rangeLong(1L, 5L)
        .subscribe(ts);

        ts.assertResult(1L);
    }

    @Test
    public void fastPathCancel() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>() {
            @Override
            public void onNext(Long t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.rangeLong(1L, 5L)
        .subscribe(ts);

        ts.assertResult(1L);
    }

    @Test
    public void conditionalSlowPathCancel() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>(1L) {
            @Override
            public void onNext(Long t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.rangeLong(1L, 5L)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertResult(1L);
    }

    @Test
    public void conditionalFastPathCancel() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>() {
            @Override
            public void onNext(Long t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.rangeLong(1L, 5L)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertResult(1L);
    }

    @Test
    public void conditionalRequestOneByOne() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>(1L) {
            @Override
            public void onNext(Long t) {
                super.onNext(t);
                request(1);
            }
        };

        Flowable.rangeLong(1L, 5L)
        .filter(new Predicate<Long>() {
            @Override
            public boolean test(Long v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        ts.assertResult(2L, 4L);
    }

    @Test
    public void conditionalRequestOneByOne2() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>(1L) {
            @Override
            public void onNext(Long t) {
                super.onNext(t);
                request(1);
            }
        };

        Flowable.rangeLong(1L, 5L)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertResult(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    public void fastPathCancelExact() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>() {
            @Override
            public void onNext(Long t) {
                super.onNext(t);
                if (t == 5L) {
                    cancel();
                    onComplete();
                }
            }
        };

        Flowable.rangeLong(1L, 5L)
        .subscribe(ts);

        ts.assertResult(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    public void conditionalFastPathCancelExact() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>() {
            @Override
            public void onNext(Long t) {
                super.onNext(t);
                if (t == 5L) {
                    cancel();
                    onComplete();
                }
            }
        };

        Flowable.rangeLong(1L, 5L)
        .filter(new Predicate<Long>() {
            @Override
            public boolean test(Long v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        ts.assertResult(2L, 4L);
    }
}
