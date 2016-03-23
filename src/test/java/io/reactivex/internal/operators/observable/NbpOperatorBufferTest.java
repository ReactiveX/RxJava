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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.*;

import io.reactivex.Observable;
import io.reactivex.Observable.NbpOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observers.*;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.PublishSubject;

public class NbpOperatorBufferTest {

    private Observer<List<String>> NbpObserver;
    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        NbpObserver = TestHelper.mockNbpSubscriber();
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void testComplete() {
        Observable<String> source = Observable.empty();

        Observable<List<String>> buffered = source.buffer(3, 3);
        buffered.subscribe(NbpObserver);

        Mockito.verify(NbpObserver, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        Mockito.verify(NbpObserver, Mockito.never()).onError(Mockito.any(Throwable.class));
        Mockito.verify(NbpObserver, Mockito.times(1)).onComplete();
    }

    @Test
    public void testSkipAndCountOverlappingBuffers() {
        Observable<String> source = Observable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(Observer<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                NbpObserver.onNext("one");
                NbpObserver.onNext("two");
                NbpObserver.onNext("three");
                NbpObserver.onNext("four");
                NbpObserver.onNext("five");
            }
        });

        Observable<List<String>> buffered = source.buffer(3, 1);
        buffered.subscribe(NbpObserver);

        InOrder inOrder = Mockito.inOrder(NbpObserver);
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("one", "two", "three"));
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("two", "three", "four"));
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("three", "four", "five"));
        inOrder.verify(NbpObserver, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(NbpObserver, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(NbpObserver, Mockito.never()).onComplete();
    }

    @Test
    public void testSkipAndCountGaplessBuffers() {
        Observable<String> source = Observable.just("one", "two", "three", "four", "five");

        Observable<List<String>> buffered = source.buffer(3, 3);
        buffered.subscribe(NbpObserver);

        InOrder inOrder = Mockito.inOrder(NbpObserver);
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("one", "two", "three"));
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("four", "five"));
        inOrder.verify(NbpObserver, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(NbpObserver, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(NbpObserver, Mockito.times(1)).onComplete();
    }

    @Test
    public void testSkipAndCountBuffersWithGaps() {
        Observable<String> source = Observable.just("one", "two", "three", "four", "five");

        Observable<List<String>> buffered = source.buffer(2, 3);
        buffered.subscribe(NbpObserver);

        InOrder inOrder = Mockito.inOrder(NbpObserver);
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("one", "two"));
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("four", "five"));
        inOrder.verify(NbpObserver, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(NbpObserver, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(NbpObserver, Mockito.times(1)).onComplete();
    }

    @Test
    public void testTimedAndCount() {
        Observable<String> source = Observable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(Observer<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                push(NbpObserver, "one", 10);
                push(NbpObserver, "two", 90);
                push(NbpObserver, "three", 110);
                push(NbpObserver, "four", 190);
                push(NbpObserver, "five", 210);
                complete(NbpObserver, 250);
            }
        });

        Observable<List<String>> buffered = source.buffer(100, TimeUnit.MILLISECONDS, 2, scheduler);
        buffered.subscribe(NbpObserver);

        InOrder inOrder = Mockito.inOrder(NbpObserver);
        scheduler.advanceTimeTo(100, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("one", "two"));

        scheduler.advanceTimeTo(200, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("three", "four"));

        scheduler.advanceTimeTo(300, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("five"));
        inOrder.verify(NbpObserver, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(NbpObserver, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(NbpObserver, Mockito.times(1)).onComplete();
    }

    @Test
    public void testTimed() {
        Observable<String> source = Observable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(Observer<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                push(NbpObserver, "one", 97);
                push(NbpObserver, "two", 98);
                /**
                 * Changed from 100. Because scheduling the cut to 100ms happens before this
                 * NbpObservable even runs due how lift works, pushing at 100ms would execute after the
                 * buffer cut.
                 */
                push(NbpObserver, "three", 99);
                push(NbpObserver, "four", 101);
                push(NbpObserver, "five", 102);
                complete(NbpObserver, 150);
            }
        });

        Observable<List<String>> buffered = source.buffer(100, TimeUnit.MILLISECONDS, scheduler);
        buffered.subscribe(NbpObserver);

        InOrder inOrder = Mockito.inOrder(NbpObserver);
        scheduler.advanceTimeTo(101, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("one", "two", "three"));

        scheduler.advanceTimeTo(201, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("four", "five"));
        inOrder.verify(NbpObserver, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(NbpObserver, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(NbpObserver, Mockito.times(1)).onComplete();
    }

    @Test
    public void testObservableBasedOpenerAndCloser() {
        Observable<String> source = Observable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(Observer<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                push(NbpObserver, "one", 10);
                push(NbpObserver, "two", 60);
                push(NbpObserver, "three", 110);
                push(NbpObserver, "four", 160);
                push(NbpObserver, "five", 210);
                complete(NbpObserver, 500);
            }
        });

        Observable<Object> openings = Observable.create(new NbpOnSubscribe<Object>() {
            @Override
            public void accept(Observer<Object> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                push(NbpObserver, new Object(), 50);
                push(NbpObserver, new Object(), 200);
                complete(NbpObserver, 250);
            }
        });

        Function<Object, Observable<Object>> closer = new Function<Object, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Object opening) {
                return Observable.create(new NbpOnSubscribe<Object>() {
                    @Override
                    public void accept(Observer<? super Object> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        push(NbpObserver, new Object(), 100);
                        complete(NbpObserver, 101);
                    }
                });
            }
        };

        Observable<List<String>> buffered = source.buffer(openings, closer);
        buffered.subscribe(NbpObserver);

        InOrder inOrder = Mockito.inOrder(NbpObserver);
        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("two", "three"));
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("five"));
        inOrder.verify(NbpObserver, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(NbpObserver, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(NbpObserver, Mockito.times(1)).onComplete();
    }

    @Test
    public void testObservableBasedCloser() {
        Observable<String> source = Observable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(Observer<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                push(NbpObserver, "one", 10);
                push(NbpObserver, "two", 60);
                push(NbpObserver, "three", 110);
                push(NbpObserver, "four", 160);
                push(NbpObserver, "five", 210);
                complete(NbpObserver, 250);
            }
        });

        Supplier<Observable<Object>> closer = new Supplier<Observable<Object>>() {
            @Override
            public Observable<Object> get() {
                return Observable.create(new NbpOnSubscribe<Object>() {
                    @Override
                    public void accept(Observer<? super Object> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        push(NbpObserver, new Object(), 100);
                        push(NbpObserver, new Object(), 200);
                        push(NbpObserver, new Object(), 300);
                        complete(NbpObserver, 301);
                    }
                });
            }
        };

        Observable<List<String>> buffered = source.buffer(closer);
        buffered.subscribe(NbpObserver);

        InOrder inOrder = Mockito.inOrder(NbpObserver);
        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("one", "two"));
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("three", "four"));
        inOrder.verify(NbpObserver, Mockito.times(1)).onNext(list("five"));
        inOrder.verify(NbpObserver, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(NbpObserver, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(NbpObserver, Mockito.times(1)).onComplete();
    }

    @Test
    public void testLongTimeAction() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        LongTimeAction action = new LongTimeAction(latch);
        Observable.just(1).buffer(10, TimeUnit.MILLISECONDS, 10)
                .subscribe(action);
        latch.await();
        assertFalse(action.fail);
    }

    private static class LongTimeAction implements Consumer<List<Integer>> {

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

    private <T> void push(final Observer<T> NbpObserver, final T value, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                NbpObserver.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void complete(final Observer<?> NbpObserver, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                NbpObserver.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testBufferStopsWhenUnsubscribed1() {
        Observable<Integer> source = Observable.never();

        Observer<List<Integer>> o = TestHelper.mockNbpSubscriber();
        TestObserver<List<Integer>> ts = new TestObserver<List<Integer>>(o);

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
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        Observer<Object> o = TestHelper.mockNbpSubscriber();
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
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        Observer<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = Mockito.inOrder(o);

        source.buffer(boundary).subscribe(o);

        boundary.onComplete();

        inOrder.verify(o, times(1)).onNext(Arrays.asList());

        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void bufferWithBOEmptyLastViaSource() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        Observer<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = Mockito.inOrder(o);

        source.buffer(boundary).subscribe(o);

        source.onComplete();

        inOrder.verify(o, times(1)).onNext(Arrays.asList());

        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void bufferWithBOEmptyLastViaBoth() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        Observer<Object> o = TestHelper.mockNbpSubscriber();
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
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        Observer<Object> o = TestHelper.mockNbpSubscriber();

        source.buffer(boundary).subscribe(o);
        source.onNext(1);
        source.onError(new TestException());

        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
    }

    @Test
    public void bufferWithBOBoundaryThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        Observer<Object> o = TestHelper.mockNbpSubscriber();

        source.buffer(boundary).subscribe(o);

        source.onNext(1);
        boundary.onError(new TestException());

        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
    }
    @Test(timeout = 2000)
    public void bufferWithSizeTake1() {
        Observable<Integer> source = Observable.just(1).repeat();
        
        Observable<List<Integer>> result = source.buffer(2).take(1);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        result.subscribe(o);
        
        verify(o).onNext(Arrays.asList(1, 1));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test(timeout = 2000)
    public void bufferWithSizeSkipTake1() {
        Observable<Integer> source = Observable.just(1).repeat();
        
        Observable<List<Integer>> result = source.buffer(2, 3).take(1);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        result.subscribe(o);
        
        verify(o).onNext(Arrays.asList(1, 1));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test(timeout = 2000)
    public void bufferWithTimeTake1() {
        Observable<Long> source = Observable.interval(40, 40, TimeUnit.MILLISECONDS, scheduler);
        
        Observable<List<Long>> result = source.buffer(100, TimeUnit.MILLISECONDS, scheduler).take(1);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        result.subscribe(o);
        
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        
        verify(o).onNext(Arrays.asList(0L, 1L));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test(timeout = 2000)
    public void bufferWithTimeSkipTake2() {
        Observable<Long> source = Observable.interval(40, 40, TimeUnit.MILLISECONDS, scheduler);
        
        Observable<List<Long>> result = source.buffer(100, 60, TimeUnit.MILLISECONDS, scheduler).take(2);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
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
        Observable<Long> boundary = Observable.interval(60, 60, TimeUnit.MILLISECONDS, scheduler);
        Observable<Long> source = Observable.interval(40, 40, TimeUnit.MILLISECONDS, scheduler);
        
        Observable<List<Long>> result = source.buffer(boundary).take(2);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
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
        Observable<Long> start = Observable.interval(61, 61, TimeUnit.MILLISECONDS, scheduler);
        Function<Long, Observable<Long>> end = new Function<Long, Observable<Long>>() {
            @Override
            public Observable<Long> apply(Long t1) {
                return Observable.interval(100, 100, TimeUnit.MILLISECONDS, scheduler);
            }
        };
        
        Observable<Long> source = Observable.interval(40, 40, TimeUnit.MILLISECONDS, scheduler);
        
        Observable<List<Long>> result = source.buffer(start, end).take(2);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
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
        PublishSubject<Integer> source = PublishSubject.create();
        
        Observable<List<Integer>> result = source.buffer(2);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
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
        PublishSubject<Integer> source = PublishSubject.create();
        
        Observable<List<Integer>> result = source.buffer(100, TimeUnit.MILLISECONDS, scheduler);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
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
        Observable<Long> source = Observable.interval(30, 30, TimeUnit.MILLISECONDS, scheduler);
        
        Observable<List<Long>> result = source.buffer(100, TimeUnit.MILLISECONDS, 2, scheduler).take(3);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
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
        PublishSubject<Integer> start = PublishSubject.create();
        
        Function<Integer, Observable<Integer>> end = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                return Observable.never();
            }
        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<List<Integer>> result = source.buffer(start, end);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
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
        PublishSubject<Integer> start = PublishSubject.create();
        
        Function<Integer, Observable<Integer>> end = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                throw new TestException();
            }
        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<List<Integer>> result = source.buffer(start, end);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
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
        PublishSubject<Integer> start = PublishSubject.create();
        
        Function<Integer, Observable<Integer>> end = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                return Observable.error(new TestException());
            }
        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<List<Integer>> result = source.buffer(start, end);
        
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        result.subscribe(o);
        
        start.onNext(1);
        source.onNext(1);
        source.onNext(2);
        
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test(timeout = 3000)
    public void testBufferWithTimeDoesntUnsubscribeDownstream() throws InterruptedException {
        final Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        final CountDownLatch cdl = new CountDownLatch(1);
        AsyncObserver<Object> s = new AsyncObserver<Object>() {
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
        
        Observable.range(1, 1).delay(1, TimeUnit.SECONDS).buffer(2, TimeUnit.SECONDS).unsafeSubscribe(s);
        
        cdl.await();
        
        verify(o).onNext(Arrays.asList(1));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
        
        assertFalse(s.isDisposed());
    }
}