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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableRepeatTest {

    @Test
    public void repetition() {
        int num = 10;
        final AtomicInteger count = new AtomicInteger();
        int value = Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(final Subscriber<? super Integer> subscriber) {
                subscriber.onNext(count.incrementAndGet());
                subscriber.onComplete();
            }
        }).repeat().subscribeOn(Schedulers.computation())
        .take(num).blockingLast();

        assertEquals(num, value);
    }

    @Test
    public void repeatTake() {
        Flowable<Integer> xs = Flowable.just(1, 2);
        Object[] ys = xs.repeat().subscribeOn(Schedulers.newThread()).take(4).toList().blockingGet().toArray();
        assertArrayEquals(new Object[] { 1, 2, 1, 2 }, ys);
    }

    @Test
    public void noStackOverFlow() {
        Flowable.just(1).repeat().subscribeOn(Schedulers.newThread()).take(100000).blockingLast();
    }

    @Test
    public void repeatTakeWithSubscribeOn() throws InterruptedException {

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

    @Test
    public void repeatAndTake() {
        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        Flowable.just(1).repeat().take(10).subscribe(subscriber);

        verify(subscriber, times(10)).onNext(1);
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void repeatLimited() {
        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        Flowable.just(1).repeat(10).subscribe(subscriber);

        verify(subscriber, times(10)).onNext(1);
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void repeatError() {
        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        Flowable.error(new TestException()).repeat(10).subscribe(subscriber);

        verify(subscriber).onError(any(TestException.class));
        verify(subscriber, never()).onNext(any());
        verify(subscriber, never()).onComplete();

    }

    @Test
    public void repeatZero() {
        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        Flowable.just(1).repeat(0).subscribe(subscriber);

        verify(subscriber).onComplete();
        verify(subscriber, never()).onNext(any());
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void repeatOne() {
        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        Flowable.just(1).repeat(1).subscribe(subscriber);

        verify(subscriber).onComplete();
        verify(subscriber, times(1)).onNext(any());
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    /** Issue #2587. */
    @Test
    public void repeatAndDistinctUnbounded() {
        Flowable<Integer> src = Flowable.fromIterable(Arrays.asList(1, 2, 3, 4, 5))
                .take(3)
                .repeat(3)
                .distinct();

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        src.subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertValues(1, 2, 3);
    }

    /** Issue #2844: wrong target of request. */
    @Test
    public void repeatRetarget() {
        final List<Integer> concatBase = new ArrayList<>();
        TestSubscriber<Integer> ts = new TestSubscriber<>();
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

        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertNoValues();

        assertEquals(Arrays.asList(1, 2, 1, 2, 1, 2, 1, 2, 1, 2), concatBase);
    }

    @Test
    public void repeatScheduled() {

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(1).subscribeOn(Schedulers.computation()).repeat(5).subscribe(ts);

        ts.awaitDone(5, TimeUnit.SECONDS);
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
            public Flowable apply(Flowable f) {
                return f.take(2);
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
            public Flowable apply(Flowable f) {
                return f.take(2);
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
    public void repeatUntilCancel() {
        Flowable.just(1)
        .repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return true;
            }
        })
        .test(2L, true)
        .assertEmpty();
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
      final PublishProcessor<Object> processor = PublishProcessor.create();
      final Disposable disposable = Flowable.just("Leak")
          .repeatWhen(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> completions) throws Exception {
                return completions.switchMap(new Function<Object, Flowable<Object>>() {
                    @Override
                    public Flowable<Object> apply(Object ignore) throws Exception {
                        return processor;
                    }
                });
            }
        })
          .subscribe();

      assertTrue(processor.hasSubscribers());
      disposable.dispose();
      assertFalse(processor.hasSubscribers());
    }

    @Test
    public void repeatWhen() {
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

    @Test
    public void noCancelPreviousRepeat() {
        final AtomicInteger counter = new AtomicInteger();

        Flowable<Integer> source = Flowable.just(1).doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                counter.getAndIncrement();
            }
        });

        source.repeat(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);

        assertEquals(0, counter.get());
    }

    @Test
    public void noCancelPreviousRepeatUntil() {
        final AtomicInteger counter = new AtomicInteger();

        Flowable<Integer> source = Flowable.just(1).doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                counter.getAndIncrement();
            }
        });

        final AtomicInteger times = new AtomicInteger();

        source.repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return times.getAndIncrement() == 4;
            }
        })
        .test()
        .assertResult(1, 1, 1, 1, 1);

        assertEquals(0, counter.get());
    }

    @Test
    public void noCancelPreviousRepeatWhen() {
        final AtomicInteger counter = new AtomicInteger();

        Flowable<Integer> source = Flowable.just(1).doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                counter.getAndIncrement();
            }
        });

        final AtomicInteger times = new AtomicInteger();

        source.repeatWhen(new Function<Flowable<Object>, Flowable<?>>() {
            @Override
            public Flowable<?> apply(Flowable<Object> e) throws Exception {
                return e.takeWhile(new Predicate<Object>() {
                    @Override
                    public boolean test(Object v) throws Exception {
                        return times.getAndIncrement() < 4;
                    }
                });
            }
        })
        .test()
        .assertResult(1, 1, 1, 1, 1);

        assertEquals(0, counter.get());
    }

    @Test
    public void repeatFloodNoSubscriptionError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            final PublishProcessor<Integer> source = PublishProcessor.create();
            final PublishProcessor<Integer> signaller = PublishProcessor.create();

            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

                TestSubscriber<Integer> ts = source.take(1)
                .repeatWhen(new Function<Flowable<Object>, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Flowable<Object> v)
                            throws Exception {
                        return signaller;
                    }
                }).test();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
                            source.onNext(1);
                        }
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
                            signaller.offer(1);
                        }
                    }
                };

                TestHelper.race(r1, r2);

                ts.cancel();
            }

            if (!errors.isEmpty()) {
                for (Throwable e : errors) {
                    e.printStackTrace();
                }
                fail(errors + "");
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
