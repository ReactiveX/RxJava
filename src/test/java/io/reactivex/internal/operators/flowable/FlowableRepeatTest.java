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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableRepeatTest {

    @Test(timeout = 2000)
    public void testRepetition() {
        int NUM = 10;
        final AtomicInteger count = new AtomicInteger();
        int value = Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(final Subscriber<? super Integer> o) {
                o.onNext(count.incrementAndGet());
                o.onComplete();
            }
        }).repeat().subscribeOn(Schedulers.computation())
        .take(NUM).blockingLast();

        assertEquals(NUM, value);
    }

    @Test(timeout = 2000)
    public void testRepeatTake() {
        Flowable<Integer> xs = Flowable.just(1, 2);
        Object[] ys = xs.repeat().subscribeOn(Schedulers.newThread()).take(4).toList().blockingGet().toArray();
        assertArrayEquals(new Object[] { 1, 2, 1, 2 }, ys);
    }

    @Test(timeout = 20000)
    public void testNoStackOverFlow() {
        Flowable.just(1).repeat().subscribeOn(Schedulers.newThread()).take(100000).blockingLast();
    }

    @Test
    public void testRepeatTakeWithSubscribeOn() throws InterruptedException {

        final AtomicInteger counter = new AtomicInteger();
        Flowable<Integer> oi = Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                sub.onSubscribe(new BooleanSubscription());
                counter.incrementAndGet();
                sub.onNext(1);
                sub.onNext(2);
                sub.onComplete();
            }
        }).subscribeOn(Schedulers.newThread());

        Object[] ys = oi.repeat().subscribeOn(Schedulers.newThread()).map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return t1;
            }

        }).take(4).toList().blockingGet().toArray();

        assertEquals(2, counter.get());
        assertArrayEquals(new Object[] { 1, 2, 1, 2 }, ys);
    }

    @Test(timeout = 2000)
    public void testRepeatAndTake() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Flowable.just(1).repeat().take(10).subscribe(o);

        verify(o, times(10)).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test(timeout = 2000)
    public void testRepeatLimited() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Flowable.just(1).repeat(10).subscribe(o);

        verify(o, times(10)).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test(timeout = 2000)
    public void testRepeatError() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Flowable.error(new TestException()).repeat(10).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();

    }

    @Test(timeout = 2000)
    public void testRepeatZero() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Flowable.just(1).repeat(0).subscribe(o);

        verify(o).onComplete();
        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test(timeout = 2000)
    public void testRepeatOne() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Flowable.just(1).repeat(1).subscribe(o);

        verify(o).onComplete();
        verify(o, times(1)).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }

    /** Issue #2587. */
    @Test
    public void testRepeatAndDistinctUnbounded() {
        Flowable<Integer> src = Flowable.fromIterable(Arrays.asList(1, 2, 3, 4, 5))
                .take(3)
                .repeat(3)
                .distinct();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        src.subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertValues(1, 2, 3);
    }

    /** Issue #2844: wrong target of request. */
    @Test(timeout = 3000)
    public void testRepeatRetarget() {
        final List<Integer> concatBase = new ArrayList<Integer>();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.just(1, 2)
        .repeat(5)
        .concatMap(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer x) {
                System.out.println("testRepeatRetarget -> " + x);
                concatBase.add(x);
                return Flowable.<Integer>empty()
                        .delay(200, TimeUnit.MILLISECONDS);
            }
        })
        .subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        ts.assertNoValues();

        assertEquals(Arrays.asList(1, 2, 1, 2, 1, 2, 1, 2, 1, 2), concatBase);
    }

    @Test
    public void repeatScheduled() {

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(1).subscribeOn(Schedulers.computation()).repeat(5).subscribe(ts);

        ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
        ts.assertValues(1, 1, 1, 1, 1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void repeatWhenDefaultScheduler() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(1).repeatWhen((Function)new Function<Flowable, Flowable>() {
            @Override
            public Flowable apply(Flowable o) {
                return o.take(2);
            }
        }).subscribe(ts);

        ts.assertValues(1, 1);
        ts.assertNoErrors();
        ts.assertComplete();

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void repeatWhenTrampolineScheduler() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(1).subscribeOn(Schedulers.trampoline())
        .repeatWhen((Function)new Function<Flowable, Flowable>() {
            @Override
            public Flowable apply(Flowable o) {
                return o.take(2);
            }
        }).subscribe(ts);

        ts.assertValues(1, 1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void repeatUntil() {
        Flowable.just(1)
        .repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        })
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatLongPredicateInvalid() {
        try {
            Flowable.just(1).repeat(-99);
            fail("Should have thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("times >= 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void repeatUntilError() {
        Flowable.error(new TestException())
        .repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return true;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void repeatUntilFalse() {
        Flowable.just(1)
        .repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return true;
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void repeatUntilSupplierCrash() {
        Flowable.just(1)
        .repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void shouldDisposeInnerObservable() {
      final PublishProcessor<Object> subject = PublishProcessor.create();
      final Disposable disposable = Flowable.just("Leak")
          .repeatWhen(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> completions) throws Exception {
                return completions.switchMap(new Function<Object, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> apply(Object ignore) throws Exception {
                        return subject;
                    }
                });
            }
        })
          .subscribe();

      assertTrue(subject.hasSubscribers());
      disposable.dispose();
      assertFalse(subject.hasSubscribers());
    }

    @Test
    public void testRepeatWhen() {
        Flowable.error(new TestException())
        .repeatWhen(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> v) throws Exception {
                return v.delay(10, TimeUnit.SECONDS);
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void whenTake() {
        Flowable.range(1, 3).repeatWhen(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> handler) throws Exception {
                return handler.take(2);
            }
        })
        .test()
        .assertResult(1, 2, 3, 1, 2, 3);
    }
}
