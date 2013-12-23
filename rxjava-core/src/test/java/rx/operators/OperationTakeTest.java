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

import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationTake.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.operators.OperationSkipTest.CustomException;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

public class OperationTakeTest {

    @Test
    public void testTake1() {
        Observable<String> w = Observable.from("one", "two", "three");
        Observable<String> take = Observable.create(take(w, 2));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, never()).onNext("three");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testTake2() {
        Observable<String> w = Observable.from("one", "two", "three");
        Observable<String> take = Observable.create(take(w, 1));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, never()).onNext("two");
        verify(aObserver, never()).onNext("three");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTakeWithError() {
        Observable.from(1, 2, 3).take(1).map(new Func1<Integer, Integer>() {
            public Integer call(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        }).toBlockingObservable().single();
    }

    @Test
    public void testTakeWithErrorHappeningInOnNext() {
        Observable<Integer> w = Observable.from(1, 2, 3).take(2).map(new Func1<Integer, Integer>() {
            public Integer call(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        w.subscribe(observer);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(any(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTakeWithErrorHappeningInTheLastOnNext() {
        Observable<Integer> w = Observable.from(1, 2, 3).take(1).map(new Func1<Integer, Integer>() {
            public Integer call(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        w.subscribe(observer);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(any(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTakeDoesntLeakErrors() {
        Observable<String> source = Observable.create(new Observable.OnSubscribeFunc<String>() {
            @Override
            public Subscription onSubscribe(Observer<? super String> observer) {
                observer.onNext("one");
                observer.onError(new Throwable("test failed"));
                return Subscriptions.empty();
            }
        });

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);

        Observable.create(take(source, 1)).subscribe(aObserver);

        verify(aObserver, times(1)).onNext("one");
        // even though onError is called we take(1) so shouldn't see it
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
        verifyNoMoreInteractions(aObserver);
    }

    @Test
    public void testTakeZeroDoesntLeakError() {
        final AtomicBoolean subscribed = new AtomicBoolean(false);
        final AtomicBoolean unSubscribed = new AtomicBoolean(false);
        Observable<String> source = Observable.create(new Observable.OnSubscribeFunc<String>() {
            @Override
            public Subscription onSubscribe(Observer<? super String> observer) {
                subscribed.set(true);
                observer.onError(new Throwable("test failed"));
                return new Subscription() {
                    @Override
                    public void unsubscribe() {
                        unSubscribed.set(true);
                    }
                };
            }
        });

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);

        Observable.create(take(source, 0)).subscribe(aObserver);
        assertTrue("source subscribed", subscribed.get());
        assertTrue("source unsubscribed", unSubscribed.get());

        verify(aObserver, never()).onNext(anyString());
        // even though onError is called we take(0) so shouldn't see it
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
        verifyNoMoreInteractions(aObserver);
    }

    @Test
    public void testUnsubscribeAfterTake() {
        final Subscription s = mock(Subscription.class);
        TestObservableFunc f = new TestObservableFunc(s, "one", "two", "three");
        Observable<String> w = Observable.create(f);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        Observable<String> take = Observable.create(take(w, 1));
        take.subscribe(aObserver);

        // wait for the Observable to complete
        try {
            f.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        System.out.println("TestObservable thread finished");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, never()).onNext("two");
        verify(aObserver, never()).onNext("three");
        verify(aObserver, times(1)).onCompleted();
        verify(s, times(1)).unsubscribe();
        verifyNoMoreInteractions(aObserver);
    }

    private static class TestObservableFunc implements Observable.OnSubscribeFunc<String> {

        final Subscription s;
        final String[] values;
        Thread t = null;

        public TestObservableFunc(Subscription s, String... values) {
            this.s = s;
            this.values = values;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super String> observer) {
            System.out.println("TestObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        observer.onCompleted();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
            return s;
        }
    }
    
    @Test
    public void testTakeTimed() {
        TestScheduler scheduler = new TestScheduler();
        
        PublishSubject<Integer> source = PublishSubject.create();
        
        Observable<Integer> result = source.take(1, TimeUnit.SECONDS, scheduler);
        
        Observer<Object> o = mock(Observer.class);
        
        result.subscribe(o);
        
        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        source.onNext(4);
        
        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
        
        verify(o, never()).onNext(4);
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testTakeTimedErrorBeforeTime() {
        TestScheduler scheduler = new TestScheduler();
        
        PublishSubject<Integer> source = PublishSubject.create();
        
        Observable<Integer> result = source.take(1, TimeUnit.SECONDS, scheduler);
        
        Observer<Object> o = mock(Observer.class);
        
        result.subscribe(o);
        
        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onError(new CustomException());
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        source.onNext(4);
        
        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onError(any(CustomException.class));
        inOrder.verifyNoMoreInteractions();
        
        verify(o, never()).onCompleted();
        verify(o, never()).onNext(4);
    }
    
    @Test
    public void testTakeTimedErrorAfterTime() {
        TestScheduler scheduler = new TestScheduler();
        
        PublishSubject<Integer> source = PublishSubject.create();
        
        Observable<Integer> result = source.take(1, TimeUnit.SECONDS, scheduler);
        
        Observer<Object> o = mock(Observer.class);
        
        result.subscribe(o);
        
        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        source.onNext(4);
        source.onError(new CustomException());
        
        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
        
        verify(o, never()).onNext(4);
        verify(o, never()).onError(any(CustomException.class));
    }
}
