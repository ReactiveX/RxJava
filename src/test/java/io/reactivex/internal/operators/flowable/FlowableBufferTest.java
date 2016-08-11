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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
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
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.TestScheduler;
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

        Mockito.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
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
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
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
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
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
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
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

        Flowable<List<String>> buffered = source.buffer(100, TimeUnit.MILLISECONDS, 2, scheduler);
        buffered.subscribe(observer);

        InOrder inOrder = Mockito.inOrder(observer);
        scheduler.advanceTimeTo(100, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two"));

        scheduler.advanceTimeTo(200, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("three", "four"));

        scheduler.advanceTimeTo(300, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("five"));
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
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
                 * Observable even runs due how lift works, pushing at 100ms would execute after the
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
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onComplete();
    }

    @Test
    public void testObservableBasedOpenerAndCloser() {
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
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onComplete();
    }

    @Test
    public void testObservableBasedCloser() {
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
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
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
        boolean fail = false;

        public LongTimeAction(CountDownLatch latch) {
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
        
        Flowable<List<Long>> result = source.buffer(100, TimeUnit.MILLISECONDS, 2, scheduler).take(3);
        
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
}