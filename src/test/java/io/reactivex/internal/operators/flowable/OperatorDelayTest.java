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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Optional;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.*;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorDelayTest {
    private Subscriber<Long> observer;
    private Subscriber<Long> observer2;

    private TestScheduler scheduler;

    @Before
    public void before() {
        observer = TestHelper.mockSubscriber();
        observer2 = TestHelper.mockSubscriber();
        
        scheduler = new TestScheduler();
    }

    @Test
    public void testDelay() {
        Flowable<Long> source = Flowable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Flowable<Long> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testLongDelay() {
        Flowable<Long> source = Flowable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Flowable<Long> delayed = source.delay(5L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(5999L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(6000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        scheduler.advanceTimeTo(6999L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        scheduler.advanceTimeTo(7000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        scheduler.advanceTimeTo(7999L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        scheduler.advanceTimeTo(8000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder.verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithError() {
        Flowable<Long> source = Flowable.interval(1L, TimeUnit.SECONDS, scheduler)
        .map(new Function<Long, Long>() {
            @Override
            public Long apply(Long value) {
                if (value == 1L) {
                    throw new RuntimeException("error!");
                }
                return value;
            }
        });
        Flowable<Long> delayed = source.delay(1L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(1999L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();

        scheduler.advanceTimeTo(5000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
    }

    @Test
    public void testDelayWithMultipleSubscriptions() {
        Flowable<Long> source = Flowable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Flowable<Long> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(observer);
        delayed.subscribe(observer2);

        InOrder inOrder = inOrder(observer);
        InOrder inOrder2 = inOrder(observer2);

        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder2.verify(observer2, times(1)).onNext(0L);

        scheduler.advanceTimeTo(2499L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder2.verify(observer2, times(1)).onNext(1L);

        verify(observer, never()).onComplete();
        verify(observer2, never()).onComplete();

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        inOrder2.verify(observer2, times(1)).onNext(2L);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());
        inOrder.verify(observer, times(1)).onComplete();
        inOrder2.verify(observer2, times(1)).onComplete();

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer2, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelaySubscription() {
        Flowable<Integer> result = Flowable.just(1, 2, 3).delaySubscription(100, TimeUnit.MILLISECONDS, scheduler);

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        inOrder.verify(o, never()).onNext(any());
        inOrder.verify(o, never()).onComplete();

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        inOrder.verify(o, times(1)).onNext(1);
        inOrder.verify(o, times(1)).onNext(2);
        inOrder.verify(o, times(1)).onNext(3);
        inOrder.verify(o, times(1)).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelaySubscriptionCancelBeforeTime() {
        Flowable<Integer> result = Flowable.just(1, 2, 3).delaySubscription(100, TimeUnit.MILLISECONDS, scheduler);

        Subscriber<Object> o = TestHelper.mockSubscriber();
        TestSubscriber<Object> ts = new TestSubscriber<Object>(o);

        result.subscribe(ts);
        ts.dispose();
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableNormal1() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final List<PublishProcessor<Integer>> delays = new ArrayList<PublishProcessor<Integer>>();
        final int n = 10;
        for (int i = 0; i < n; i++) {
            PublishProcessor<Integer> delay = PublishProcessor.create();
            delays.add(delay);
        }

        Function<Integer, Flowable<Integer>> delayFunc = new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t1) {
                return delays.get(t1);
            }
        };

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        for (int i = 0; i < n; i++) {
            source.onNext(i);
            delays.get(i).onNext(i);
            inOrder.verify(o).onNext(i);
        }
        source.onComplete();

        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableSingleSend1() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();

        Function<Integer, Flowable<Integer>> delayFunc = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return delay;
            }
        };
        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);
        delay.onNext(2);

        inOrder.verify(o).onNext(1);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableSourceThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();

        Function<Integer, Flowable<Integer>> delayFunc = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return delay;
            }
        };
        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);
        source.onError(new TestException());
        delay.onNext(1);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableDelayFunctionThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        Function<Integer, Flowable<Integer>> delayFunc = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                throw new TestException();
            }
        };
        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableDelayThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();

        Function<Integer, Flowable<Integer>> delayFunc = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return delay;
            }
        };
        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);
        delay.onError(new TestException());

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableSubscriptionNormal() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();
        Supplier<Flowable<Integer>> subFunc = new Supplier<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> get() {
                return delay;
            }
        };
        Function<Integer, Flowable<Integer>> delayFunc = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return delay;
            }
        };

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);

        source.onNext(2);
        delay.onNext(2);

        inOrder.verify(o).onNext(2);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableSubscriptionFunctionThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();
        Supplier<Flowable<Integer>> subFunc = new Supplier<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> get() {
                throw new TestException();
            }
        };
        Function<Integer, Flowable<Integer>> delayFunc = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return delay;
            }
        };

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);

        source.onNext(2);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableSubscriptionThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();
        Supplier<Flowable<Integer>> subFunc = new Supplier<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> get() {
                return delay;
            }
        };
        Function<Integer, Flowable<Integer>> delayFunc = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return delay;
            }
        };

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        delay.onError(new TestException());

        source.onNext(2);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableEmptyDelayer() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        Function<Integer, Flowable<Integer>> delayFunc = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return Flowable.empty();
            }
        };
        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        source.onNext(1);
        source.onComplete();

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableSubscriptionRunCompletion() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> sdelay = PublishProcessor.create();
        final PublishProcessor<Integer> delay = PublishProcessor.create();
        Supplier<Flowable<Integer>> subFunc = new Supplier<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> get() {
                return sdelay;
            }
        };
        Function<Integer, Flowable<Integer>> delayFunc = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return delay;
            }
        };

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        sdelay.onComplete();

        source.onNext(2);
        delay.onNext(2);

        inOrder.verify(o).onNext(2);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableAsTimed() {
        Flowable<Long> source = Flowable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);

        final Flowable<Long> delayer = Flowable.timer(500L, TimeUnit.MILLISECONDS, scheduler);

        Function<Long, Flowable<Long>> delayFunc = new Function<Long, Flowable<Long>>() {
            @Override
            public Flowable<Long> apply(Long t1) {
                return delayer;
            }
        };

        Flowable<Long> delayed = source.delay(delayFunc);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableReorder() {
        int n = 3;

        PublishProcessor<Integer> source = PublishProcessor.create();
        final List<PublishProcessor<Integer>> subjects = new ArrayList<PublishProcessor<Integer>>();
        for (int i = 0; i < n; i++) {
            subjects.add(PublishProcessor.<Integer> create());
        }

        Flowable<Integer> result = source.delay(new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return subjects.get(t1);
            }
        });

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        for (int i = 0; i < n; i++) {
            source.onNext(i);
        }
        source.onComplete();

        inOrder.verify(o, never()).onNext(anyInt());
        inOrder.verify(o, never()).onComplete();

        for (int i = n - 1; i >= 0; i--) {
            subjects.get(i).onComplete();
            inOrder.verify(o).onNext(i);
        }

        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayEmitsEverything() {
        Flowable<Integer> source = Flowable.range(1, 5);
        Flowable<Integer> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed = delayed.doOnEach(new Consumer<Try<Optional<Integer>>>() {

            @Override
            public void accept(Try<Optional<Integer>> t1) {
                System.out.println(t1);
            }

        });
        TestSubscriber<Integer> observer = new TestSubscriber<Integer>();
        delayed.subscribe(observer);
        // all will be delivered after 500ms since range does not delay between them
        scheduler.advanceTimeBy(500L, TimeUnit.MILLISECONDS);
        observer.assertValues(1, 2, 3, 4, 5);
    }

    @Test
    public void testBackpressureWithTimedDelay() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(1, Flowable.bufferSize() * 2)
                .delay(100, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c = 0;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
    }
    
    @Test
    public void testBackpressureWithSubscriptionTimedDelay() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(1, Flowable.bufferSize() * 2)
                .delaySubscription(100, TimeUnit.MILLISECONDS)
                .delay(100, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c = 0;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
    }

    @Test
    public void testBackpressureWithSelectorDelay() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(1, Flowable.bufferSize() * 2)
                .delay(new Function<Integer, Flowable<Long>>() {

                    @Override
                    public Flowable<Long> apply(Integer i) {
                        return Flowable.timer(100, TimeUnit.MILLISECONDS);
                    }

                })
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c = 0;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
    }

    @Test
    public void testBackpressureWithSelectorDelayAndSubscriptionDelay() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(1, Flowable.bufferSize() * 2)
                .delay(new Supplier<Flowable<Long>>() {

                    @Override
                    public Flowable<Long> get() {
                        return Flowable.timer(500, TimeUnit.MILLISECONDS);
                    }
                }, new Function<Integer, Flowable<Long>>() {

                    @Override
                    public Flowable<Long> apply(Integer i) {
                        return Flowable.timer(100, TimeUnit.MILLISECONDS);
                    }

                })
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c = 0;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.valueCount());
    }
    
    @Test
    public void testErrorRunsBeforeOnNext() {
        TestScheduler test = Schedulers.test();
        
        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        ps.delay(1, TimeUnit.SECONDS, test).subscribe(ts);
        
        ps.onNext(1);
        
        test.advanceTimeBy(500, TimeUnit.MILLISECONDS);
        
        ps.onError(new TestException());
        
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }
    public void testDelaySupplierSimple() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();
        
        Flowable<Integer> source = Flowable.range(1, 5);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.delaySubscription(new Supplier<Publisher<Integer>>() {
            @Override
            public Publisher<Integer> get() {
                return ps;
            }
        }).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ps.onNext(1);
        
        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertComplete();
        ts.assertNoErrors();
    }
    
    @Test
    public void testDelaySupplierCompletes() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();
        
        Flowable<Integer> source = Flowable.range(1, 5);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.delaySubscription(new Supplier<Publisher<Integer>>() {
            @Override
            public Publisher<Integer> get() {
                return ps;
            }
        }).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        // FIXME should this complete the source instead of consuming it?
        ps.onComplete();
        
        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertComplete();
        ts.assertNoErrors();
    }
    
    @Test
    public void testDelaySupplierErrors() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();
        
        Flowable<Integer> source = Flowable.range(1, 5);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.delaySubscription(new Supplier<Publisher<Integer>>() {
            @Override
            public Publisher<Integer> get() {
                return ps;
            }
        }).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ps.onError(new TestException());
        
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

}