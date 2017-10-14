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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.*;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.*;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.PublishSubject;

public class ObservableBufferTest {

    private Observer<List<String>> observer;
    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        observer = TestHelper.mockObserver();
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void testComplete() {
        Observable<String> source = Observable.empty();

        Observable<List<String>> buffered = source.buffer(3, 3);
        buffered.subscribe(observer);

        Mockito.verify(observer, Mockito.never()).onNext(Mockito.<String>anyList());
        Mockito.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        Mockito.verify(observer, Mockito.times(1)).onComplete();
    }

    @Test
    public void testSkipAndCountOverlappingBuffers() {
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                observer.onNext("one");
                observer.onNext("two");
                observer.onNext("three");
                observer.onNext("four");
                observer.onNext("five");
            }
        });

        Observable<List<String>> buffered = source.buffer(3, 1);
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
        Observable<String> source = Observable.just("one", "two", "three", "four", "five");

        Observable<List<String>> buffered = source.buffer(3, 3);
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
        Observable<String> source = Observable.just("one", "two", "three", "four", "five");

        Observable<List<String>> buffered = source.buffer(2, 3);
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
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                push(observer, "one", 10);
                push(observer, "two", 90);
                push(observer, "three", 110);
                push(observer, "four", 190);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Observable<List<String>> buffered = source.buffer(100, TimeUnit.MILLISECONDS, scheduler, 2);
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
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
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

        Observable<List<String>> buffered = source.buffer(100, TimeUnit.MILLISECONDS, scheduler);
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
    public void testObservableBasedOpenerAndCloser() {
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 500);
            }
        });

        Observable<Object> openings = Observable.unsafeCreate(new ObservableSource<Object>() {
            @Override
            public void subscribe(Observer<Object> observer) {
                observer.onSubscribe(Disposables.empty());
                push(observer, new Object(), 50);
                push(observer, new Object(), 200);
                complete(observer, 250);
            }
        });

        Function<Object, Observable<Object>> closer = new Function<Object, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Object opening) {
                return Observable.unsafeCreate(new ObservableSource<Object>() {
                    @Override
                    public void subscribe(Observer<? super Object> observer) {
                        observer.onSubscribe(Disposables.empty());
                        push(observer, new Object(), 100);
                        complete(observer, 101);
                    }
                });
            }
        };

        Observable<List<String>> buffered = source.buffer(openings, closer);
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
    public void testObservableBasedCloser() {
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Callable<Observable<Object>> closer = new Callable<Observable<Object>>() {
            @Override
            public Observable<Object> call() {
                return Observable.unsafeCreate(new ObservableSource<Object>() {
                    @Override
                    public void subscribe(Observer<? super Object> observer) {
                        observer.onSubscribe(Disposables.empty());
                        push(observer, new Object(), 100);
                        push(observer, new Object(), 200);
                        push(observer, new Object(), 300);
                        complete(observer, 301);
                    }
                });
            }
        };

        Observable<List<String>> buffered = source.buffer(closer);
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
        Observable.just(1).buffer(10, TimeUnit.MILLISECONDS, 10)
                .subscribe(action);
        latch.await();
        assertFalse(action.fail);
    }

    private static class LongTimeAction implements Consumer<List<Integer>> {

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

    private <T> void push(final Observer<T> observer, final T value, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void complete(final Observer<?> observer, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testBufferStopsWhenUnsubscribed1() {
        Observable<Integer> source = Observable.never();

        Observer<List<Integer>> o = TestHelper.mockObserver();
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

        Observer<Object> o = TestHelper.mockObserver();
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

        Observer<Object> o = TestHelper.mockObserver();
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

        Observer<Object> o = TestHelper.mockObserver();
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

        Observer<Object> o = TestHelper.mockObserver();
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

        Observer<Object> o = TestHelper.mockObserver();

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

        Observer<Object> o = TestHelper.mockObserver();

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

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 1));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test(timeout = 2000)
    public void bufferWithSizeSkipTake1() {
        Observable<Integer> source = Observable.just(1).repeat();

        Observable<List<Integer>> result = source.buffer(2, 3).take(1);

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(Arrays.asList(1, 1));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test(timeout = 2000)
    public void bufferWithTimeTake1() {
        Observable<Long> source = Observable.interval(40, 40, TimeUnit.MILLISECONDS, scheduler);

        Observable<List<Long>> result = source.buffer(100, TimeUnit.MILLISECONDS, scheduler).take(1);

        Observer<Object> o = TestHelper.mockObserver();

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

        Observer<Object> o = TestHelper.mockObserver();
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

        Observer<Object> o = TestHelper.mockObserver();
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

        Observer<Object> o = TestHelper.mockObserver();
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

        Observer<Object> o = TestHelper.mockObserver();

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

        Observer<Object> o = TestHelper.mockObserver();
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

        Observable<List<Long>> result = source.buffer(100, TimeUnit.MILLISECONDS, scheduler, 2).take(3);

        Observer<Object> o = TestHelper.mockObserver();
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

        Observer<Object> o = TestHelper.mockObserver();

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

        Observer<Object> o = TestHelper.mockObserver();

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

        Observer<Object> o = TestHelper.mockObserver();

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
        final Observer<Object> o = TestHelper.mockObserver();

        final CountDownLatch cdl = new CountDownLatch(1);
        DisposableObserver<Object> s = new DisposableObserver<Object>() {
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

        Observable.range(1, 1).delay(1, TimeUnit.SECONDS).buffer(2, TimeUnit.SECONDS).subscribe(s);

        cdl.await();

        verify(o).onNext(Arrays.asList(1));
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));

        assertFalse(s.isDisposed());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimeSkipDefault() {
        Observable.range(1, 5).buffer(1, 1, TimeUnit.MINUTES)
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferBoundaryHint() {
        Observable.range(1, 5).buffer(Observable.timer(1, TimeUnit.MINUTES), 2)
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5));
    }

    static HashSet<Integer> set(Integer... values) {
        return new HashSet<Integer>(Arrays.asList(values));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferIntoCustomCollection() {
        Observable.just(1, 1, 2, 2, 3, 3, 4, 4)
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
        Observable.just(1, 1, 2, 2, 3, 3, 4, 4)
        .buffer(3, 3, new Callable<Collection<Integer>>() {
            @Override
            public Collection<Integer> call() throws Exception {
                return new HashSet<Integer>();
            }
        })
        .test()
        .assertResult(set(1, 2), set(2, 3), set(4));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void supplierThrows() {
        Observable.just(1)
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
        Observable.just(1)
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
        Observable.just(1)
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
        Observable.<Integer>never()
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
        Observable.<Integer>never()
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
        Observable.<Integer>never()
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

    @Test
    @SuppressWarnings("unchecked")
    public void supplierReturnsNull() {
        Observable.<Integer>never()
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
        Observable.<Integer>never()
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
        Observable.<Integer>never()
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
    public void boundaryBufferSupplierThrows() {
        Observable.never()
        .buffer(Functions.justCallable(Observable.never()), new Callable<Collection<Object>>() {
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
        Observable.never()
        .buffer(new Callable<ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> call() throws Exception {
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
        Observable.never()
        .buffer(Functions.justCallable(Observable.timer(1, TimeUnit.MILLISECONDS)), new Callable<Collection<Object>>() {
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
        Observable.never()
        .buffer(Functions.justCallable(Observable.timer(1, TimeUnit.MILLISECONDS)), new Callable<Collection<Object>>() {
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
        Observable.never()
        .buffer(new Callable<ObservableSource<Long>>() {
            int count;
            @Override
            public ObservableSource<Long> call() throws Exception {
                if (count++ == 1) {
                    throw new TestException();
                }
                return Observable.timer(1, TimeUnit.MILLISECONDS);
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
        PublishSubject<Object> pp = PublishSubject.create();

        TestObserver<Collection<Object>> ts = pp
        .buffer(Functions.justCallable(Observable.never()), new Callable<Collection<Object>>() {
            @Override
            public Collection<Object> call() throws Exception {
                return new ArrayList<Object>();
            }
        })
        .test();

        assertTrue(pp.hasObservers());

        ts.dispose();

        assertFalse(pp.hasObservers());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void boundaryBoundarySupplierReturnsNull() {
        Observable.never()
        .buffer(new Callable<ObservableSource<Long>>() {
            int count;
            @Override
            public ObservableSource<Long> call() throws Exception {
                if (count++ == 1) {
                    return null;
                }
                return Observable.timer(1, TimeUnit.MILLISECONDS);
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
        Observable.never()
        .buffer(new Callable<ObservableSource<Long>>() {
            int count;
            @Override
            public ObservableSource<Long> call() throws Exception {
                if (count++ == 1) {
                    return null;
                }
                return Observable.empty();
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
        PublishSubject<Object> pp = PublishSubject.create();

        TestObserver<Collection<Object>> ts = pp
        .buffer(Functions.justCallable(Observable.never()), new Callable<Collection<Object>>() {
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
        PublishSubject<Object> pp = PublishSubject.create();

        TestObserver<Collection<Object>> ts = pp
        .buffer(Functions.justCallable(Observable.error(new TestException())), new Callable<Collection<Object>>() {
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
        TestHelper.checkDisposed(Observable.range(1, 5).buffer(1, TimeUnit.DAYS, Schedulers.single()));

        TestHelper.checkDisposed(Observable.range(1, 5).buffer(2, 1, TimeUnit.DAYS, Schedulers.single()));

        TestHelper.checkDisposed(Observable.range(1, 5).buffer(1, 2, TimeUnit.DAYS, Schedulers.single()));

        TestHelper.checkDisposed(Observable.range(1, 5)
                .buffer(1, TimeUnit.DAYS, Schedulers.single(), 2, Functions.<Integer>createArrayList(16), true));

        TestHelper.checkDisposed(Observable.range(1, 5).buffer(1));

        TestHelper.checkDisposed(Observable.range(1, 5).buffer(2, 1));

        TestHelper.checkDisposed(Observable.range(1, 5).buffer(1, 2));

        TestHelper.checkDisposed(PublishSubject.create().buffer(Observable.never()));

        TestHelper.checkDisposed(PublishSubject.create().buffer(Functions.justCallable(Observable.never())));

        TestHelper.checkDisposed(PublishSubject.create().buffer(Observable.never(), Functions.justFunction(Observable.never())));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void restartTimer() {
        Observable.range(1, 5)
        .buffer(1, TimeUnit.DAYS, Schedulers.single(), 2, Functions.<Integer>createArrayList(16), true)
        .test()
        .assertResult(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferSupplierCrash2() {
        Observable.range(1, 2)
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
        Observable.range(1, 2)
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
    public void bufferSkipError() {
        Observable.<Integer>error(new TestException())
        .buffer(2, 1)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferSkipOverlap() {
        Observable.range(1, 5)
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
        Observable.error(new TestException())
        .buffer(1, TimeUnit.DAYS)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedSkipError() {
        Observable.error(new TestException())
        .buffer(1, 2, TimeUnit.DAYS)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedOverlapError() {
        Observable.error(new TestException())
        .buffer(2, 1, TimeUnit.DAYS)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedExactEmpty() {
        Observable.empty()
        .buffer(1, TimeUnit.DAYS)
        .test()
        .assertResult(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedSkipEmpty() {
        Observable.empty()
        .buffer(1, 2, TimeUnit.DAYS)
        .test()
        .assertResult(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedOverlapEmpty() {
        Observable.empty()
        .buffer(2, 1, TimeUnit.DAYS)
        .test()
        .assertResult(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferTimedExactSupplierCrash() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<List<Integer>> to = ps
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

        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<List<Integer>> to = ps
        .buffer(1, TimeUnit.MILLISECONDS, scheduler, 1, Functions.<Integer>createArrayList(16), true)
        .test();

        ps.onError(new TestException());

        to
        .assertFailure(TestException.class);
    }

    @Test
    public void withTimeAndSizeCapacityRace() {
        for (int i = 0; i < 1000; i++) {
            final TestScheduler scheduler = new TestScheduler();

            final PublishSubject<Object> ps = PublishSubject.create();

            TestObserver<List<Object>> ts = ps.buffer(1, TimeUnit.SECONDS, scheduler, 5).test();

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
