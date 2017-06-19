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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.*;

public class FlowableBufferTest {

    private Subscriber<List<String>> observer;
    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        observer = TestHelper.mockSubscriber();
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void testComplete() {
        Flowable<String> source = Flowable.empty();

        Flowable<List<String>> buffered = source.buffer(3, 3);
        buffered.subscribe(observer);

        Mockito.verify(observer, Mockito.never()).onNext(Mockito.<String>anyList());
        Mockito.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        Mockito.verify(observer, Mockito.times(1)).onComplete();
    }

    @Test
    public void testSkipAndCountOverlappingBuffers() {
        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                observer.onNext("one");
                observer.onNext("two");
                observer.onNext("three");
                observer.onNext("four");
                observer.onNext("five");
            }
        });

        Flowable<List<String>> buffered = source.buffer(3, 1);
        buffered.subscribe(observer);

        InOrder inOrder = Mockito.inOrder(observer);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two", "three"));
        inOrder.verify(observer, Mockito.times(1)).onNext(list("two", "three", "four"));
        inOrder.verify(observer, Mockito.times(1)).onNext(list("three", "four", "five"));
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.<String>anyList());
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void testSkipAndCountGaplessBuffers() {
        Flowable<String> source = Flowable.just("one", "two", "three", "four", "five");

        Flowable<List<String>> buffered = source.buffer(3, 3);
        buffered.subscribe(observer);

        InOrder inOrder = Mockito.inOrder(observer);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two", "three"));
        inOrder.verify(observer, Mockito.times(1)).onNext(list("four", "five"));
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.<String>anyList());
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onComplete();
    }

    @Test
    public void testSkipAndCountBuffersWithGaps() {
        Flowable<String> source = Flowable.just("one", "two", "three", "four", "five");

        Flowable<List<String>> buffered = source.buffer(2, 3);
        buffered.subscribe(observer);

        InOrder inOrder = Mockito.inOrder(observer);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two"));
        inOrder.verify(observer, Mockito.times(1)).onNext(list("four", "five"));
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.<String>anyList());
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onComplete();
    }

    @Test
    public void testTimedAndCount() {
        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                push(observer, "one", 10);
                push(observer, "two", 90);
                push(observer, "three", 110);
                push(observer, "four", 190);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Flowable<List<String>> buffered = source.buffer(100, TimeUnit.MILLISECONDS, scheduler, 2);
        buffered.subscribe(observer);

        InOrder inOrder = Mockito.inOrder(observer);
        scheduler.advanceTimeTo(100, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two"));

        scheduler.advanceTimeTo(200, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("three", "four"));

        scheduler.advanceTimeTo(300, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("five"));
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.<String>anyList());
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onComplete();
    }

    @Test
    public void testTimed() {
        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                push(observer, "one", 97);
                push(observer, "two", 98);
                /**
                 * Changed from 100. Because scheduling the cut to 100ms happens before this
                 * Flowable even runs due how lift works, pushing at 100ms would execute after the
                 * buffer cut.
                 */
                push(observer, "three", 99);
                push(observer, "four", 101);
                push(observer, "five", 102);
                complete(observer, 150);
            }
        });

        Flowable<List<String>> buffered = source.buffer(100, TimeUnit.MILLISECONDS, scheduler);
        buffered.subscribe(observer);

        InOrder inOrder = Mockito.inOrder(observer);
        scheduler.advanceTimeTo(101, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two", "three"));

        scheduler.advanceTimeTo(201, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("four", "five"));
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.<String>anyList());
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onComplete();
    }

    @Test
    public void testFlowableBasedOpenerAndCloser() {
        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 500);
            }
        });

        Flowable<Object> openings = Flowable.unsafeCreate(new Publisher<Object>() {
            @Override
            public void subscribe(Subscriber<Object> observer) {
                observer.onSubscribe(new BooleanSubscription());
                push(observer, new Object(), 50);
                push(observer, new Object(), 200);
                complete(observer, 250);
            }
        });

        Function<Object, Flowable<Object>> closer = new Function<Object, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Object opening) {
                return Flowable.unsafeCreate(new Publisher<Object>() {
                    @Override
                    public void subscribe(Subscriber<? super Object> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        push(observer, new Object(), 100);
                        complete(observer, 101);
                    }
                });
            }
        };

        Flowable<List<String>> buffered = source.buffer(openings, closer);
        buffered.subscribe(observer);

        InOrder inOrder = Mockito.inOrder(observer);
        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("two", "three"));
        inOrder.verify(observer, Mockito.times(1)).onNext(list("five"));
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.<String>anyList());
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onComplete();
    }

    @Test
    public void testFlowableBasedCloser() {
        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Callable<Flowable<Object>> closer = new Callable<Flowable<Object>>() {
            @Override
            public Flowable<Object> call() {
                return Flowable.unsafeCreate(new Publisher<Object>() {
                    @Override
                    public void subscribe(Subscriber<? super Object> observer) {
                        observer.onSubscribe(new BooleanSubscription());
                        push(observer, new Object(), 100);
                        push(observer, new Object(), 200);
                        push(observer, new Object(), 300);
                        complete(observer, 301);
                    }
                });
            }
        };

        Flowable<List<String>> buffered = source.buffer(closer);
        buffered.subscribe(observer);

        InOrder inOrder = Mockito.inOrder(observer);
        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two"));
        inOrder.verify(observer, Mockito.times(1)).onNext(list("three", "four"));
        inOrder.verify(observer, Mockito.times(1)).onNext(list("five"));
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.<String>anyList());
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onComplete();
    }

    @Test
    public void testLongTimeAction() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        LongTimeAction action = new LongTimeAction(latch);
        Flowable.just(1).buffer(10, TimeUnit.MILLISECONDS, 10)
                .subscribe(action);
        latch.await();
        assertFalse(action.fail);
    }

    static final class LongTimeAction implements Consumer<List<Integer>> {

        CountDownLatch latch;
        boolean fail;

        LongTimeAction(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void accept(List<Integer> t1) {
            try {
                if (fail) {
                    return;
                }
                Thread.sleep(200);
            } catch (InterruptedException e) {
                fail = true;
            } finally {
                latch.countDown();
            }
        }
    }

    private List<String> list(String... args) {
        List<String> list = new ArrayList<String>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }

    private <T> void push(final Subscriber<T> observer, final T value, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void complete(final Subscriber<?> observer, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testBufferStopsWhenUnsubscribed1() {
        Flowable<Integer> source = Flowable.never();

        Subscriber<List<Integer>> o = TestHelper.mockSubscriber();
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(o, 0L);

        source.buffer(100, 200, TimeUnit.MILLISECONDS, scheduler)
        .doOnNext(new Consumer<List<Integer>>() {
            @Override
            public void accept(List<Integer> pv) {
                System.out.println(pv);
            }
        })
        .subscribe(ts);

        InOrder inOrder = Mockito.inOrder(o);

        scheduler.advanceTimeBy(1001, TimeUnit.MILLISECONDS);

        inOrder.verify(o, times(5)).onNext(Arrays.<Integer> asList());

        ts.dispose();

        scheduler.advanceTimeBy(999, TimeUnit.MILLISECONDS);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void bufferWithBONormal1() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = Mockito.inOrder(o);

        source.buffer(boundary).subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        boundary.onNext(1);

        inOrder.verify(o, times(1)).onNext(Arrays.asList(1, 2, 3));

        source.onNext(4);
        source.onNext(5);

        boundary.onNext(2);

        inOrder.verify(o, times(1)).onNext(Arrays.asList(4, 5));

        source.onNext(6);
        boundary.onComplete();

        inOrder.verify(o, times(1)).onNext(Arrays.asList(6));

        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void bufferWithBOEmptyLastViaBoundary() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = Mockito.inOrder(o);

        source.buffer(boundary).subscribe(o);

        boundary.onComplete();

        inOrder.verify(o, times(1)).onNext(Arrays.asList());

        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void bufferWithBOEmptyLastViaSource() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = Mockito.inOrder(o);

        source.buffer(boundary).subscribe(o);

        source.onComplete();

        inOrder.verify(o, times(1)).onNext(Arrays.asList());

        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void bufferWithBOEmptyLastViaBoth() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = Mockito.inOrder(o);

        source.buffer(boundary).subscribe(o);

        source.onComplete();
        boundary.onComplete();

        inOrder.verify(o, times(1)).onNext(Arrays.asList());

        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void bufferWithBOSourceThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        Subscriber<Object> o = TestHelper.mockSubscriber();

        source.buffer(boundary).subscribe(o);
        source.onNext(1);
        source.onError(new TestException());

        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
    }

    @Test
    public void bufferWithBOBoundaryThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        Subscriber<Object> o = TestHelper.mockSubscriber();

        source.buffer(boundary).subscribe(o);

        source.onNext(1);
        boundary.onError(new TestException());

        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
    }
    @Test(timeout = 2000)
    public void bufferWithSizeTake1() {
        Flowable<Integer> source = Flowable.just(1).repeat();

        Flowable<List<Integer>> result = source.buffer(2).take(1);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 1));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test(timeout = 2000)
    public void bufferWithSizeSkipTake1() {
        Flowable<Integer> source = Flowable.just(1).repeat();

        Flowable<List<Integer>> result = source.buffer(2, 3).take(1);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 1));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test(timeout = 2000)
    public void bufferWithTimeTake1() {
        Flowable<Long> source = Flowable.interval(40, 40, TimeUnit.MILLISECONDS, scheduler);

        Flowable<List<Long>> result = source.buffer(100, TimeUnit.MILLISECONDS, scheduler).take(1);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        result.subscribe(o);

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        verify(o).onNext(Arrays.asList(0L, 1L));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test(timeout = 2000)
    public void bufferWithTimeSkipTake2() {
        Flowable<Long> source = Flowable.interval(40, 40, TimeUnit.MILLISECONDS, scheduler);

        Flowable<List<Long>> result = source.buffer(100, 60, TimeUnit.MILLISECONDS, scheduler).take(2);

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        inOrder.verify(o).onNext(Arrays.asList(0L, 1L));
        inOrder.verify(o).onNext(Arrays.asList(1L, 2L));
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test(timeout = 2000)
    public void bufferWithBoundaryTake2() {
        Flowable<Long> boundary = Flowable.interval(60, 60, TimeUnit.MILLISECONDS, scheduler);
        Flowable<Long> source = Flowable.interval(40, 40, TimeUnit.MILLISECONDS, scheduler);

        Flowable<List<Long>> result = source.buffer(boundary).take(2);

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        inOrder.verify(o).onNext(Arrays.asList(0L));
        inOrder.verify(o).onNext(Arrays.asList(1L));
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test(timeout = 2000)
    public void bufferWithStartEndBoundaryTake2() {
        Flowable<Long> start = Flowable.interval(61, 61, TimeUnit.MILLISECONDS, scheduler);
        Function<Long, Flowable<Long>> end = new Function<Long, Flowable<Long>>() {
            @Override
            public Flowable<Long> apply(Long t1) {
                return Flowable.interval(100, 100, TimeUnit.MILLISECONDS, scheduler);
            }
        };

        Flowable<Long> source = Flowable.interval(40, 40, TimeUnit.MILLISECONDS, scheduler);

        Flowable<List<Long>> result = source.buffer(start, end).take(2);

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        result
        .doOnNext(new Consumer<List<Long>>() {
            @Override
            public void accept(List<Long> pv) {
                System.out.println(pv);
            }
        })
        .subscribe(o);

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        inOrder.verify(o).onNext(Arrays.asList(1L, 2L, 3L));
        inOrder.verify(o).onNext(Arrays.asList(3L, 4L));
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void bufferWithSizeThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<List<Integer>> result = source.buffer(2);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onError(new TestException());

        inOrder.verify(o).onNext(Arrays.asList(1, 2));
        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(Arrays.asList(3));
        verify(o, never()).onComplete();

    }

    @Test
    public void bufferWithTimeThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<List<Integer>> result = source.buffer(100, TimeUnit.MILLISECONDS, scheduler);

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        source.onNext(3);
        source.onError(new TestException());
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        inOrder.verify(o).onNext(Arrays.asList(1, 2));
        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(Arrays.asList(3));
        verify(o, never()).onComplete();

    }

    @Test
    public void bufferWithTimeAndSize() {
        Flowable<Long> source = Flowable.interval(30, 30, TimeUnit.MILLISECONDS, scheduler);

        Flowable<List<Long>> result = source.buffer(100, TimeUnit.MILLISECONDS, scheduler, 2).take(3);

        Subscriber<Object> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        inOrder.verify(o).onNext(Arrays.asList(0L, 1L));
        inOrder.verify(o).onNext(Arrays.asList(2L));
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void bufferWithStartEndStartThrows() {
        PublishProcessor<Integer> start = PublishProcessor.create();

        Function<Integer, Flowable<Integer>> end = new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t1) {
                return Flowable.never();
            }
        };

        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<List<Integer>> result = source.buffer(start, end);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        result.subscribe(o);

        start.onNext(1);
        source.onNext(1);
        source.onNext(2);
        start.onError(new TestException());

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }
    @Test
    public void bufferWithStartEndEndFunctionThrows() {
        PublishProcessor<Integer> start = PublishProcessor.create();

        Function<Integer, Flowable<Integer>> end = new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t1) {
                throw new TestException();
            }
        };

        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<List<Integer>> result = source.buffer(start, end);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        result.subscribe(o);

        start.onNext(1);
        source.onNext(1);
        source.onNext(2);

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }
    @Test
    public void bufferWithStartEndEndThrows() {
        PublishProcessor<Integer> start = PublishProcessor.create();

        Function<Integer, Flowable<Integer>> end = new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t1) {
                return Flowable.error(new TestException());
            }
        };

        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<List<Integer>> result = source.buffer(start, end);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        result.subscribe(o);

        start.onNext(1);
        source.onNext(1);
        source.onNext(2);

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testProducerRequestThroughBufferWithSize1() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(3L);

        final AtomicLong requested = new AtomicLong();
        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {

                    }

                });
            }

        }).buffer(5, 5).subscribe(ts);
        assertEquals(15, requested.get());

        ts.request(4);
        assertEquals(20, requested.get());
    }

    @Test
    public void testProducerRequestThroughBufferWithSize2() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        final AtomicLong requested = new AtomicLong();

        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {

                    }

                });
            }

        }).buffer(5, 5).subscribe(ts);
        assertEquals(Long.MAX_VALUE, requested.get());
    }

    @Test
    public void testProducerRequestThroughBufferWithSize3() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(3L);
        final AtomicLong requested = new AtomicLong();
        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {

                    }

                });
            }

        }).buffer(5, 2).subscribe(ts);
        assertEquals(9, requested.get());
        ts.request(3);
        assertEquals(6, requested.get());
    }

    @Test
    public void testProducerRequestThroughBufferWithSize4() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        final AtomicLong requested = new AtomicLong();
        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {

                    }

                });
            }

        }).buffer(5, 2).subscribe(ts);
        assertEquals(Long.MAX_VALUE, requested.get());
    }


    @Test
    public void testProducerRequestOverflowThroughBufferWithSize1() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(Long.MAX_VALUE >> 1);

        final AtomicLong requested = new AtomicLong();

        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {

                    }

                });
            }

        }).buffer(3, 3).subscribe(ts);
        assertEquals(Long.MAX_VALUE, requested.get());
    }

    @Test
    public void testProducerRequestOverflowThroughBufferWithSize2() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(Long.MAX_VALUE >> 1);

        final AtomicLong requested = new AtomicLong();

        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {

                    }

                });
            }

        }).buffer(3, 2).subscribe(ts);
        assertEquals(Long.MAX_VALUE, requested.get());
    }

    @Test
    public void testProducerRequestOverflowThroughBufferWithSize3() {
        final AtomicLong requested = new AtomicLong();
        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(final Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {
                    AtomicBoolean once = new AtomicBoolean();
                    @Override
                    public void request(long n) {
                        requested.set(n);
                        if (once.compareAndSet(false, true)) {
                            s.onNext(1);
                            s.onNext(2);
                            s.onNext(3);
                        }
                    }

                    @Override
                    public void cancel() {

                    }

                });
            }

        }).buffer(3, 2).subscribe(new DefaultSubscriber<List<Integer>>() {

            @Override
            public void onStart() {
                request(Long.MAX_VALUE / 2 - 4);
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(List<Integer> t) {
                request(Long.MAX_VALUE / 2);
            }

        });
        // FIXME I'm not sure why this is MAX_VALUE in 1.x because MAX_VALUE/2 is even and thus can't overflow when multiplied by 2
        assertEquals(Long.MAX_VALUE - 1, requested.get());
    }
    @Test(timeout = 3000)
    public void testBufferWithTimeDoesntUnsubscribeDownstream() throws InterruptedException {
        final Subscriber<Object> o = TestHelper.mockSubscriber();

        final CountDownLatch cdl = new CountDownLatch(1);
        ResourceSubscriber<Object> s = new ResourceSubscriber<Object>() {
            @Override
            public void onNext(Object t) {
                o.onNext(t);
            }
            @Override
            public void onError(Throwable e) {
                o.onError(e);
                cdl.countDown();
            }
            @Override
            public void onComplete() {
                o.onComplete();
                cdl.countDown();
            }
        };

        Flowable.range(1, 1).delay(1, TimeUnit.SECONDS).buffer(2, TimeUnit.SECONDS).subscribe(s);

        cdl.await();

        verify(o).onNext(Arrays.asList(1));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));

        assertFalse(s.isDisposed());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPostCompleteBackpressure() {
        Flowable<List<Integer>> source = Flowable.range(1, 10).buffer(3, 1);

        TestSubscriber<List<Integer>> ts = TestSubscriber.create(0L);

        source.subscribe(ts);

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertNoErrors();

        ts.request(7);

        ts.assertValues(
                Arrays.asList(1, 2, 3),
                Arrays.asList(2, 3, 4),
                Arrays.asList(3, 4, 5),
                Arrays.asList(4, 5, 6),
                Arrays.asList(5, 6, 7),
                Arrays.asList(6, 7, 8),
                Arrays.asList(7, 8, 9)
        );
        ts.assertNotComplete();
        ts.assertNoErrors();

        ts.request(1);

        ts.assertValues(
                Arrays.asList(1, 2, 3),
                Arrays.asList(2, 3, 4),
                Arrays.asList(3, 4, 5),
                Arrays.asList(4, 5, 6),
                Arrays.asList(5, 6, 7),
                Arrays.asList(6, 7, 8),
                Arrays.asList(7, 8, 9),
                Arrays.asList(8, 9, 10)
        );
        ts.assertNotComplete();
        ts.assertNoErrors();

        ts.request(1);

        ts.assertValues(
                Arrays.asList(1, 2, 3),
                Arrays.asList(2, 3, 4),
                Arrays.asList(3, 4, 5),
                Arrays.asList(4, 5, 6),
                Arrays.asList(5, 6, 7),
                Arrays.asList(6, 7, 8),
                Arrays.asList(7, 8, 9),
                Arrays.asList(8, 9, 10),
                Arrays.asList(9, 10)
        );
        ts.assertNotComplete();
        ts.assertNoErrors();

        ts.request(1);

        ts.assertValues(
                Arrays.asList(1, 2, 3),
                Arrays.asList(2, 3, 4),
                Arrays.asList(3, 4, 5),
                Arrays.asList(4, 5, 6),
                Arrays.asList(5, 6, 7),
                Arrays.asList(6, 7, 8),
                Arrays.asList(7, 8, 9),
                Arrays.asList(8, 9, 10),
                Arrays.asList(9, 10),
                Arrays.asList(10)
        );
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void timeAndSkipOverlap() {

        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<List<Integer>> ts = TestSubscriber.create();

        ps.buffer(2, 1, TimeUnit.SECONDS, scheduler).subscribe(ts);

        ps.onNext(1);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ps.onNext(2);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ps.onNext(3);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ps.onNext(4);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ps.onComplete();

        ts.assertValues(
                Arrays.asList(1, 2),
                Arrays.asList(2, 3),
                Arrays.asList(3, 4),
                Arrays.asList(4),
                Collections.<Integer>emptyList()
        );

        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void timeAndSkipSkip() {

        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<List<Integer>> ts = TestSubscriber.create();

        ps.buffer(2, 3, TimeUnit.SECONDS, scheduler).subscribe(ts);

        ps.onNext(1);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ps.onNext(2);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ps.onNext(3);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ps.onNext(4);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ps.onComplete();

        ts.assertValues(
                Arrays.asList(1, 2),
                Arrays.asList(4)
        );

        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void timeAndSkipOverlapScheduler() {

        RxJavaPlugins.setComputationSchedulerHandler(new Function<Scheduler, Scheduler>() {
            @Override
            public Scheduler apply(Scheduler t) {
                return scheduler;
            }
        });

        try {
            PublishProcessor<Integer> ps = PublishProcessor.create();

            TestSubscriber<List<Integer>> ts = TestSubscriber.create();

            ps.buffer(2, 1, TimeUnit.SECONDS).subscribe(ts);

            ps.onNext(1);

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            ps.onNext(2);

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            ps.onNext(3);

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            ps.onNext(4);

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            ps.onComplete();

            ts.assertValues(
                    Arrays.asList(1, 2),
                    Arrays.asList(2, 3),
                    Arrays.asList(3, 4),
                    Arrays.asList(4),
                    Collections.<Integer>emptyList()
            );

            ts.assertNoErrors();
            ts.assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void timeAndSkipSkipDefaultScheduler() {
        RxJavaPlugins.setComputationSchedulerHandler(new Function<Scheduler, Scheduler>() {
            @Override
            public Scheduler apply(Scheduler t) {
                return scheduler;
            }
        });

        try {

            PublishProcessor<Integer> ps = PublishProcessor.create();

            TestSubscriber<List<Integer>> ts = TestSubscriber.create();

            ps.buffer(2, 3, TimeUnit.SECONDS).subscribe(ts);

            ps.onNext(1);

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            ps.onNext(2);

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            ps.onNext(3);

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            ps.onNext(4);

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            ps.onComplete();

            ts.assertValues(
                    Arrays.asList(1, 2),
                    Arrays.asList(4)
            );

            ts.assertNoErrors();
            ts.assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferBoundaryHint() {
        Flowable.range(1, 5).buffer(Flowable.timer(1, TimeUnit.MINUTES), 2)
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5));
    }

    static HashSet<Integer> set(Integer... values) {
        return new HashSet<Integer>(Arrays.asList(values));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferIntoCustomCollection() {
        Flowable.just(1, 1, 2, 2, 3, 3, 4, 4)
        .buffer(3, new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() throws Exception {
                return new HashSet<Integer>();
            }
        })
        .test()
        .assertResult(set(1, 2), set(2, 3), set(4));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferSkipIntoCustomCollection() {
        Flowable.just(1, 1, 2, 2, 3, 3, 4, 4)
        .buffer(3, 3, new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() throws Exception {
                return new HashSet<Integer>();
            }
        })
        .test()
        .assertResult(set(1, 2), set(2, 3), set(4));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimeSkipDefault() {
        Flowable.range(1, 5).buffer(1, 1, TimeUnit.MINUTES)
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void boundaryBufferSupplierThrows() {
        Flowable.never()
        .buffer(Functions.justCallable(Flowable.never()), new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void boundaryBoundarySupplierThrows() {
        Flowable.never()
        .buffer(new Callable<Publisher<Object>>() {
            @Override
            public Publisher<Object> call() throws Exception {
                throw new TestException();
            }
        }, new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() throws Exception {
                return new ArrayList<Object>();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void boundaryBufferSupplierThrows2() {
        Flowable.never()
        .buffer(Functions.justCallable(Flowable.timer(1, TimeUnit.MILLISECONDS)), new Callable<Collection<Object>>() {
            int count;
            @Override
            public Collection<Object> call() throws Exception {
                if (count++ == 1) {
                    throw new TestException();
                } else {
                    return new ArrayList<Object>();
                }
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void boundaryBufferSupplierReturnsNull() {
        Flowable.never()
        .buffer(Functions.justCallable(Flowable.timer(1, TimeUnit.MILLISECONDS)), new Callable<Collection<Object>>() {
            int count;
            @Override
            public Collection<Object> call() throws Exception {
                if (count++ == 1) {
                    return null;
                } else {
                    return new ArrayList<Object>();
                }
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void boundaryBoundarySupplierThrows2() {
        Flowable.never()
        .buffer(new Callable<Publisher<Long>>() {
            int count;
            @Override
            public Publisher<Long> call() throws Exception {
                if (count++ == 1) {
                    throw new TestException();
                }
                return Flowable.timer(1, TimeUnit.MILLISECONDS);
            }
        }, new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() throws Exception {
                return new ArrayList<Object>();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void boundaryCancel() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestSubscriber<Collection<Object>> ts = pp
        .buffer(Functions.justCallable(Flowable.never()), new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() throws Exception {
                return new ArrayList<Object>();
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        ts.dispose();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void boundaryBoundarySupplierReturnsNull() {
        Flowable.never()
        .buffer(new Callable<Publisher<Long>>() {
            int count;
            @Override
            public Publisher<Long> call() throws Exception {
                if (count++ == 1) {
                    return null;
                }
                return Flowable.timer(1, TimeUnit.MILLISECONDS);
            }
        }, new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() throws Exception {
                return new ArrayList<Object>();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void boundaryBoundarySupplierReturnsNull2() {
        Flowable.never()
        .buffer(new Callable<Publisher<Long>>() {
            int count;
            @Override
            public Publisher<Long> call() throws Exception {
                if (count++ == 1) {
                    return null;
                }
                return Flowable.empty();
            }
        }, new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() throws Exception {
                return new ArrayList<Object>();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void boundaryMainError() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestSubscriber<Collection<Object>> ts = pp
        .buffer(Functions.justCallable(Flowable.never()), new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() throws Exception {
                return new ArrayList<Object>();
            }
        })
        .test();

        pp.onError(new TestException());

        ts.assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void boundaryBoundaryError() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestSubscriber<Collection<Object>> ts = pp
        .buffer(Functions.justCallable(Flowable.error(new TestException())), new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() throws Exception {
                return new ArrayList<Object>();
            }
        })
        .test();

        pp.onError(new TestException());

        ts.assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.range(1, 5).buffer(1, TimeUnit.DAYS, Schedulers.single()));

        TestHelper.checkDisposed(Flowable.range(1, 5).buffer(2, 1, TimeUnit.DAYS, Schedulers.single()));

        TestHelper.checkDisposed(Flowable.range(1, 5).buffer(1, 2, TimeUnit.DAYS, Schedulers.single()));

        TestHelper.checkDisposed(Flowable.range(1, 5)
                .buffer(1, TimeUnit.DAYS, Schedulers.single(), 2, Functions.<Integer>createArrayList(16), true));

        TestHelper.checkDisposed(Flowable.range(1, 5).buffer(1));

        TestHelper.checkDisposed(Flowable.range(1, 5).buffer(2, 1));

        TestHelper.checkDisposed(Flowable.range(1, 5).buffer(1, 2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void supplierReturnsNull() {
        Flowable.<Integer>never()
        .buffer(1, TimeUnit.MILLISECONDS, Schedulers.single(), Integer.MAX_VALUE, new Callable<Collection<Integer>>() {
            int count;
            @Override
            public Collection<Integer> call() throws Exception {
                if (count++ == 1) {
                    return null;
                } else {
                    return new ArrayList<Integer>();
                }
            }
        }, false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void supplierReturnsNull2() {
        Flowable.<Integer>never()
        .buffer(1, TimeUnit.MILLISECONDS, Schedulers.single(), 10, new Callable<Collection<Integer>>() {
            int count;
            @Override
            public Collection<Integer> call() throws Exception {
                if (count++ == 1) {
                    return null;
                } else {
                    return new ArrayList<Integer>();
                }
            }
        }, false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void supplierReturnsNull3() {
        Flowable.<Integer>never()
        .buffer(2, 1, TimeUnit.MILLISECONDS, Schedulers.single(), new Callable<Collection<Integer>>() {
            int count;
            @Override
            public Collection<Integer> call() throws Exception {
                if (count++ == 1) {
                    return null;
                } else {
                    return new ArrayList<Integer>();
                }
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void supplierThrows() {
        Flowable.just(1)
        .buffer(1, TimeUnit.SECONDS, Schedulers.single(), Integer.MAX_VALUE, new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() throws Exception {
                throw new TestException();
            }
        }, false)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void supplierThrows2() {
        Flowable.just(1)
        .buffer(1, TimeUnit.SECONDS, Schedulers.single(), 10, new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() throws Exception {
                throw new TestException();
            }
        }, false)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void supplierThrows3() {
        Flowable.just(1)
        .buffer(2, 1, TimeUnit.SECONDS, Schedulers.single(), new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void supplierThrows4() {
        Flowable.<Integer>never()
        .buffer(1, TimeUnit.MILLISECONDS, Schedulers.single(), Integer.MAX_VALUE, new Callable<Collection<Integer>>() {
            int count;
            @Override
            public Collection<Integer> call() throws Exception {
                if (count++ == 1) {
                    throw new TestException();
                } else {
                    return new ArrayList<Integer>();
                }
            }
        }, false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void supplierThrows5() {
        Flowable.<Integer>never()
        .buffer(1, TimeUnit.MILLISECONDS, Schedulers.single(), 10, new Callable<Collection<Integer>>() {
            int count;
            @Override
            public Collection<Integer> call() throws Exception {
                if (count++ == 1) {
                    throw new TestException();
                } else {
                    return new ArrayList<Integer>();
                }
            }
        }, false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void supplierThrows6() {
        Flowable.<Integer>never()
        .buffer(2, 1, TimeUnit.MILLISECONDS, Schedulers.single(), new Callable<Collection<Integer>>() {
            int count;
            @Override
            public Collection<Integer> call() throws Exception {
                if (count++ == 1) {
                    throw new TestException();
                } else {
                    return new ArrayList<Integer>();
                }
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void restartTimer() {
        Flowable.range(1, 5)
        .buffer(1, TimeUnit.DAYS, Schedulers.single(), 2, Functions.<Integer>createArrayList(16), true)
        .test()
        .assertResult(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferSkipError() {
        Flowable.<Integer>error(new TestException())
        .buffer(2, 1)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferSupplierCrash2() {
        Flowable.range(1, 2)
        .buffer(1, new Callable<List<Integer>>() {
            int calls;
            @Override
            public List<Integer> call() throws Exception {
                if (++calls == 2) {
                    throw new TestException();
                }
                return new ArrayList<Integer>();
            }
        })
        .test()
        .assertFailure(TestException.class, Arrays.asList(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferSkipSupplierCrash2() {
        Flowable.range(1, 2)
        .buffer(1, 2, new Callable<List<Integer>>() {
            int calls;
            @Override
            public List<Integer> call() throws Exception {
                if (++calls == 1) {
                    throw new TestException();
                }
                return new ArrayList<Integer>();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferOverlapSupplierCrash2() {
        Flowable.range(1, 2)
        .buffer(2, 1, new Callable<List<Integer>>() {
            int calls;
            @Override
            public List<Integer> call() throws Exception {
                if (++calls == 2) {
                    throw new TestException();
                }
                return new ArrayList<Integer>();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferSkipOverlap() {
        Flowable.range(1, 5)
        .buffer(5, 1)
        .test()
        .assertResult(
            Arrays.asList(1, 2, 3, 4, 5),
            Arrays.asList(2, 3, 4, 5),
            Arrays.asList(3, 4, 5),
            Arrays.asList(4, 5),
            Arrays.asList(5)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedExactError() {
        Flowable.error(new TestException())
        .buffer(1, TimeUnit.DAYS)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedSkipError() {
        Flowable.error(new TestException())
        .buffer(1, 2, TimeUnit.DAYS)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedOverlapError() {
        Flowable.error(new TestException())
        .buffer(2, 1, TimeUnit.DAYS)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedExactEmpty() {
        Flowable.empty()
        .buffer(1, TimeUnit.DAYS)
        .test()
        .assertResult(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedSkipEmpty() {
        Flowable.empty()
        .buffer(1, 2, TimeUnit.DAYS)
        .test()
        .assertResult(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedOverlapEmpty() {
        Flowable.empty()
        .buffer(2, 1, TimeUnit.DAYS)
        .test()
        .assertResult(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedExactSupplierCrash() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<List<Integer>> to = ps
        .buffer(1, TimeUnit.MILLISECONDS, scheduler, 1, new Callable<List<Integer>>() {
            int calls;
            @Override
            public List<Integer> call() throws Exception {
                if (++calls == 2) {
                    throw new TestException();
                }
                return new ArrayList<Integer>();
            }
        }, true)
        .test();

        ps.onNext(1);

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ps.onNext(2);

        to
        .assertFailure(TestException.class, Arrays.asList(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedExactBoundedError() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<List<Integer>> to = ps
        .buffer(1, TimeUnit.MILLISECONDS, scheduler, 1, Functions.<Integer>createArrayList(16), true)
        .test();

        ps.onError(new TestException());

        to
        .assertFailure(TestException.class);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.buffer(1);
            }
        }, false, 1, 1, Arrays.asList(1));

        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.buffer(1, 2);
            }
        }, false, 1, 1, Arrays.asList(1));

        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.buffer(2, 1);
            }
        }, false, 1, 1, Arrays.asList(1));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<List<Object>>>() {
            @Override
            public Publisher<List<Object>> apply(Flowable<Object> f) throws Exception {
                return f.buffer(1);
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<List<Object>>>() {
            @Override
            public Publisher<List<Object>> apply(Flowable<Object> f) throws Exception {
                return f.buffer(1, 2);
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<List<Object>>>() {
            @Override
            public Publisher<List<Object>> apply(Flowable<Object> f) throws Exception {
                return f.buffer(2, 1);
            }
        });
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(PublishProcessor.create().buffer(1));

        TestHelper.assertBadRequestReported(PublishProcessor.create().buffer(1, 2));

        TestHelper.assertBadRequestReported(PublishProcessor.create().buffer(2, 1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void skipError() {
        Flowable.error(new TestException())
        .buffer(1, 2)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void skipSingleResult() {
        Flowable.just(1)
        .buffer(2, 3)
        .test()
        .assertResult(Arrays.asList(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void skipBackpressure() {
        Flowable.range(1, 10)
        .buffer(2, 3)
        .rebatchRequests(1)
        .test()
        .assertResult(Arrays.asList(1, 2), Arrays.asList(4, 5), Arrays.asList(7, 8), Arrays.asList(10));
    }

    @Test
    public void withTimeAndSizeCapacityRace() {
        for (int i = 0; i < 1000; i++) {
            final TestScheduler scheduler = new TestScheduler();

            final PublishProcessor<Object> ps = PublishProcessor.create();

            TestSubscriber<List<Object>> ts = ps.buffer(1, TimeUnit.SECONDS, scheduler, 5).test();

            ps.onNext(1);
            ps.onNext(2);
            ps.onNext(3);
            ps.onNext(4);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(5);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
                }
            };

            TestHelper.race(r1, r2);

            ps.onComplete();

            int items = 0;
            for (List<Object> o : ts.values()) {
                items += o.size();
            }

            assertEquals("Round: " + i, 5, items);
        }
    }
}
