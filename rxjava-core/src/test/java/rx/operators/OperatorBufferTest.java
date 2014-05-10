/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class OperatorBufferTest {

    private Observer<List<String>> observer;
    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        observer = Mockito.mock(Observer.class);
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void testComplete() {
        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                observer.onCompleted();
            }
        });

        Observable<List<String>> buffered = source.buffer(3, 3);
        buffered.subscribe(observer);

        Mockito.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        Mockito.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        Mockito.verify(observer, Mockito.times(1)).onCompleted();
    }

    @Test
    public void testSkipAndCountOverlappingBuffers() {
        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
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
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.never()).onCompleted();
    }

    @Test
    public void testSkipAndCountGaplessBuffers() {
        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                observer.onNext("one");
                observer.onNext("two");
                observer.onNext("three");
                observer.onNext("four");
                observer.onNext("five");
                observer.onCompleted();
            }
        });

        Observable<List<String>> buffered = source.buffer(3, 3);
        buffered.subscribe(observer);

        InOrder inOrder = Mockito.inOrder(observer);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two", "three"));
        inOrder.verify(observer, Mockito.times(1)).onNext(list("four", "five"));
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onCompleted();
    }

    @Test
    public void testSkipAndCountBuffersWithGaps() {
        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                observer.onNext("one");
                observer.onNext("two");
                observer.onNext("three");
                observer.onNext("four");
                observer.onNext("five");
                observer.onCompleted();
            }
        });

        Observable<List<String>> buffered = source.buffer(2, 3);
        buffered.subscribe(observer);

        InOrder inOrder = Mockito.inOrder(observer);
        inOrder.verify(observer, Mockito.times(1)).onNext(list("one", "two"));
        inOrder.verify(observer, Mockito.times(1)).onNext(list("four", "five"));
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onCompleted();
    }

    @Test
    public void testTimedAndCount() {
        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                push(observer, "one", 10);
                push(observer, "two", 90);
                push(observer, "three", 110);
                push(observer, "four", 190);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Observable<List<String>> buffered = source.buffer(100, TimeUnit.MILLISECONDS, 2, scheduler);
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
        inOrder.verify(observer, Mockito.times(1)).onCompleted();
    }

    @Test
    public void testTimed() {
        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
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
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onCompleted();
    }

    @Test
    public void testObservableBasedOpenerAndCloser() {
        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 500);
            }
        });

        Observable<Object> openings = Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<Object> observer) {
                push(observer, new Object(), 50);
                push(observer, new Object(), 200);
                complete(observer, 250);
            }
        });

        Func1<Object, Observable<Object>> closer = new Func1<Object, Observable<Object>>() {
            @Override
            public Observable<Object> call(Object opening) {
                return Observable.create(new Observable.OnSubscribe<Object>() {
                    @Override
                    public void call(Subscriber<? super Object> observer) {
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
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onCompleted();
    }

    @Test
    public void testObservableBasedCloser() {
        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Func0<Observable<Object>> closer = new Func0<Observable<Object>>() {
            @Override
            public Observable<Object> call() {
                return Observable.create(new Observable.OnSubscribe<Object>() {
                    @Override
                    public void call(Subscriber<? super Object> observer) {
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
        inOrder.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        inOrder.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        inOrder.verify(observer, Mockito.times(1)).onCompleted();
    }

    @Test
    public void testLongTimeAction() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        LongTimeAction action = new LongTimeAction(latch);
        Observable.from(1).buffer(10, TimeUnit.MILLISECONDS, 10)
                .subscribe(action);
        latch.await();
        assertFalse(action.fail);
    }

    private static class LongTimeAction implements Action1<List<Integer>> {

        CountDownLatch latch;
        boolean fail = false;

        public LongTimeAction(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void call(List<Integer> t1) {
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
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void complete(final Observer<?> observer, int delay) {
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onCompleted();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testBufferStopsWhenUnsubscribed1() {
        Observable<Integer> source = Observable.never();

        @SuppressWarnings("unchecked")
        Observer<List<Integer>> o = mock(Observer.class);

        Subscription s = source.buffer(100, 200, TimeUnit.MILLISECONDS, scheduler).subscribe(o);

        InOrder inOrder = Mockito.inOrder(o);

        scheduler.advanceTimeBy(1001, TimeUnit.MILLISECONDS);

        inOrder.verify(o, times(5)).onNext(Arrays.<Integer> asList());

        s.unsubscribe();

        scheduler.advanceTimeBy(999, TimeUnit.MILLISECONDS);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void bufferWithBONormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
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
        boundary.onCompleted();

        inOrder.verify(o, times(1)).onNext(Arrays.asList(6));

        inOrder.verify(o).onCompleted();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void bufferWithBOEmptyLastViaBoundary() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = Mockito.inOrder(o);

        source.buffer(boundary).subscribe(o);

        boundary.onCompleted();

        inOrder.verify(o, times(1)).onNext(Arrays.asList());

        inOrder.verify(o).onCompleted();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void bufferWithBOEmptyLastViaSource() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = Mockito.inOrder(o);

        source.buffer(boundary).subscribe(o);

        source.onCompleted();

        inOrder.verify(o, times(1)).onNext(Arrays.asList());

        inOrder.verify(o).onCompleted();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void bufferWithBOEmptyLastViaBoth() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = Mockito.inOrder(o);

        source.buffer(boundary).subscribe(o);

        source.onCompleted();
        boundary.onCompleted();

        inOrder.verify(o, times(1)).onNext(Arrays.asList());

        inOrder.verify(o).onCompleted();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void bufferWithBOSourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.buffer(boundary).subscribe(o);
        source.onNext(1);
        source.onError(new TestException());

        verify(o).onError(any(TestException.class));
        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
    }

    @Test
    public void bufferWithBOBoundaryThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.buffer(boundary).subscribe(o);

        source.onNext(1);
        boundary.onError(new TestException());

        verify(o).onError(any(TestException.class));
        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
    }
    @Test(timeout = 2000)
    public void bufferWithSizeTake1() {
        Observable<Integer> source = Observable.from(1).repeat();
        
        Observable<List<Integer>> result = source.buffer(2).take(1);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        result.subscribe(o);
        
        verify(o).onNext(Arrays.asList(1, 1));
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test(timeout = 2000)
    public void bufferWithSizeSkipTake1() {
        Observable<Integer> source = Observable.from(1).repeat();
        
        Observable<List<Integer>> result = source.buffer(2, 3).take(1);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        result.subscribe(o);
        
        verify(o).onNext(Arrays.asList(1, 1));
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test(timeout = 2000)
    public void bufferWithTimeTake1() {
        Observable<Long> source = Observable.timer(40, 40, TimeUnit.MILLISECONDS, scheduler);
        
        Observable<List<Long>> result = source.buffer(100, TimeUnit.MILLISECONDS, scheduler).take(1);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        result.subscribe(o);
        
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        
        verify(o).onNext(Arrays.asList(0L, 1L));
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test(timeout = 2000)
    public void bufferWithTimeSkipTake2() {
        Observable<Long> source = Observable.timer(40, 40, TimeUnit.MILLISECONDS, scheduler);
        
        Observable<List<Long>> result = source.buffer(100, 60, TimeUnit.MILLISECONDS, scheduler).take(2);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        result.subscribe(o);
        
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        
        inOrder.verify(o).onNext(Arrays.asList(0L, 1L));
        inOrder.verify(o).onNext(Arrays.asList(1L, 2L));
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test(timeout = 2000)
    public void bufferWithBoundaryTake2() {
        Observable<Long> boundary = Observable.timer(60, 60, TimeUnit.MILLISECONDS, scheduler);
        Observable<Long> source = Observable.timer(40, 40, TimeUnit.MILLISECONDS, scheduler);
        
        Observable<List<Long>> result = source.buffer(boundary).take(2);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        result.subscribe(o);
        
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        
        inOrder.verify(o).onNext(Arrays.asList(0L));
        inOrder.verify(o).onNext(Arrays.asList(1L));
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
        
    }
    
    @Test(timeout = 2000)
    public void bufferWithStartEndBoundaryTake2() {
        Observable<Long> start = Observable.timer(61, 61, TimeUnit.MILLISECONDS, scheduler);
        Func1<Long, Observable<Long>> end = new Func1<Long, Observable<Long>>() {
            @Override
            public Observable<Long> call(Long t1) {
                return Observable.timer(100, 100, TimeUnit.MILLISECONDS, scheduler);
            }
        };
        
        Observable<Long> source = Observable.timer(40, 40, TimeUnit.MILLISECONDS, scheduler);
        
        Observable<List<Long>> result = source.buffer(start, end).take(2);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        result.subscribe(o);
        
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        
        inOrder.verify(o).onNext(Arrays.asList(1L, 2L, 3L));
        inOrder.verify(o).onNext(Arrays.asList(3L, 4L));
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void bufferWithSizeThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        
        Observable<List<Integer>> result = source.buffer(2);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
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
        verify(o, never()).onCompleted();
                
    }
    
    @Test
    public void bufferWithTimeThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        
        Observable<List<Integer>> result = source.buffer(100, TimeUnit.MILLISECONDS, scheduler);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
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
        verify(o, never()).onCompleted();
                
    }
    @Test
    public void bufferWithTimeAndSize() {
        Observable<Long> source = Observable.timer(30, 30, TimeUnit.MILLISECONDS, scheduler);
        
        Observable<List<Long>> result = source.buffer(100, TimeUnit.MILLISECONDS, 2, scheduler).take(3);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        result.subscribe(o);
        
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        
        inOrder.verify(o).onNext(Arrays.asList(0L, 1L));
        inOrder.verify(o).onNext(Arrays.asList(2L));
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void bufferWithStartEndStartThrows() {
        PublishSubject<Integer> start = PublishSubject.create();
        
        Func1<Integer, Observable<Integer>> end = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return Observable.never();
            }
        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<List<Integer>> result = source.buffer(start, end);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        result.subscribe(o);
        
        start.onNext(1);
        source.onNext(1);
        source.onNext(2);
        start.onError(new TestException());
        
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }
    @Test
    public void bufferWithStartEndEndFunctionThrows() {
        PublishSubject<Integer> start = PublishSubject.create();
        
        Func1<Integer, Observable<Integer>> end = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                throw new TestException();
            }
        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<List<Integer>> result = source.buffer(start, end);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        result.subscribe(o);
        
        start.onNext(1);
        source.onNext(1);
        source.onNext(2);
        
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }
    @Test
    public void bufferWithStartEndEndThrows() {
        PublishSubject<Integer> start = PublishSubject.create();
        
        Func1<Integer, Observable<Integer>> end = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return Observable.error(new TestException());
            }
        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<List<Integer>> result = source.buffer(start, end);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        result.subscribe(o);
        
        start.onNext(1);
        source.onNext(1);
        source.onNext(2);
        
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }
}
