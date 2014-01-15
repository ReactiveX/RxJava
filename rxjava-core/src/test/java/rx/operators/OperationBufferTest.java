/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.junit.Assert.*;
import static rx.operators.OperationBuffer.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import rx.IObservable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.schedulers.TestScheduler;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

public class OperationBufferTest {

    private Observer<List<String>> observer;
    private TestScheduler scheduler;

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        observer = Mockito.mock(Observer.class);
        scheduler = new TestScheduler();
    }

    @Test
    public void testComplete() {
        IObservable<String> source = new IObservable<String>() {
            @Override
            public Subscription subscribe(Observer<? super String> observer) {
                observer.onCompleted();
                return Subscriptions.empty();
            }
        };

        IObservable<List<String>> buffered = buffer(source, 3, 3);
        buffered.subscribe(observer);

        Mockito.verify(observer, Mockito.never()).onNext(Mockito.anyListOf(String.class));
        Mockito.verify(observer, Mockito.never()).onError(Mockito.any(Throwable.class));
        Mockito.verify(observer, Mockito.times(1)).onCompleted();
    }

    @Test
    public void testSkipAndCountOverlappingBuffers() {
        IObservable<String> source = new IObservable<String>() {
            @Override
            public Subscription subscribe(Observer<? super String> observer) {
                observer.onNext("one");
                observer.onNext("two");
                observer.onNext("three");
                observer.onNext("four");
                observer.onNext("five");
                return Subscriptions.empty();
            }
        };

        IObservable<List<String>> buffered = buffer(source, 3, 1);
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
        IObservable<String> source = new IObservable<String>() {
            @Override
            public Subscription subscribe(Observer<? super String> observer) {
                observer.onNext("one");
                observer.onNext("two");
                observer.onNext("three");
                observer.onNext("four");
                observer.onNext("five");
                observer.onCompleted();
                return Subscriptions.empty();
            }
        };

        IObservable<List<String>> buffered = buffer(source, 3, 3);
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
        IObservable<String> source = new IObservable<String>() {
            @Override
            public Subscription subscribe(Observer<? super String> observer) {
                observer.onNext("one");
                observer.onNext("two");
                observer.onNext("three");
                observer.onNext("four");
                observer.onNext("five");
                observer.onCompleted();
                return Subscriptions.empty();
            }
        };

        IObservable<List<String>> buffered = buffer(source, 2, 3);
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
        IObservable<String> source = new IObservable<String>() {
            @Override
            public Subscription subscribe(Observer<? super String> observer) {
                push(observer, "one", 10);
                push(observer, "two", 90);
                push(observer, "three", 110);
                push(observer, "four", 190);
                push(observer, "five", 210);
                complete(observer, 250);
                return Subscriptions.empty();
            }
        };

        IObservable<List<String>> buffered = buffer(source, 100, TimeUnit.MILLISECONDS, 2, scheduler);
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
        IObservable<String> source = new IObservable<String>() {
            @Override
            public Subscription subscribe(Observer<? super String> observer) {
                push(observer, "one", 98);
                push(observer, "two", 99);
                push(observer, "three", 100);
                push(observer, "four", 101);
                push(observer, "five", 102);
                complete(observer, 150);
                return Subscriptions.empty();
            }
        };

        IObservable<List<String>> buffered = buffer(source, 100, TimeUnit.MILLISECONDS, scheduler);
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
        IObservable<String> source = new IObservable<String>() {
            @Override
            public Subscription subscribe(Observer<? super String> observer) {
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 500);
                return Subscriptions.empty();
            }
        };

        IObservable<Object> openings = new IObservable<Object>() {
            @Override
            public Subscription subscribe(Observer<Object> observer) {
                push(observer, new Object(), 50);
                push(observer, new Object(), 200);
                complete(observer, 250);
                return Subscriptions.empty();
            }
        };

        Func1<Object, IObservable<Object>> closer = new Func1<Object, IObservable<Object>>() {
            @Override
            public IObservable<Object> call(Object opening) {
                return new IObservable<Object>() {
                    @Override
                    public Subscription subscribe(Observer<? super Object> observer) {
                        push(observer, new Object(), 100);
                        complete(observer, 101);
                        return Subscriptions.empty();
                    }
                };
            }
        };

        IObservable<List<String>> buffered = buffer(source, openings, closer);
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
        IObservable<String> source = new IObservable<String>() {
            @Override
            public Subscription subscribe(Observer<? super String> observer) {
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 250);
                return Subscriptions.empty();
            }
        };

        Func0<IObservable<Object>> closer = new Func0<IObservable<Object>>() {
            @Override
            public IObservable<Object> call() {
                return new IObservable<Object>() {
                    @Override
                    public Subscription subscribe(Observer<? super Object> observer) {
                        push(observer, new Object(), 100);
                        complete(observer, 101);
                        return Subscriptions.empty();
                    }
                };
            }
        };

        IObservable<List<String>> buffered = buffer(source, closer);
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
        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void complete(final Observer<?> observer, int delay) {
        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onCompleted();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    @Test
    public void testBufferStopsWhenUnsubscribed1() {
        Observable<Integer> source = Observable.never();
        
        Observer<List<Integer>> o = mock(Observer.class);
        
        Subscription s = source.buffer(100, 200, TimeUnit.MILLISECONDS, scheduler).subscribe(o);
        
        InOrder inOrder = Mockito.inOrder(o);
        
        scheduler.advanceTimeBy(1001, TimeUnit.MILLISECONDS);
        
        inOrder.verify(o, times(5)).onNext(Arrays.<Integer>asList());
        
        s.unsubscribe();

        scheduler.advanceTimeBy(999, TimeUnit.MILLISECONDS);
        
        inOrder.verifyNoMoreInteractions();
    }
}
