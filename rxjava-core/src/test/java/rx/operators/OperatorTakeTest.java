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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.Subscribers;
import rx.schedulers.Schedulers;

public class OperatorTakeTest {

    @Test
    public void testTake1() {
        Observable<String> w = Observable.from(Arrays.asList("one", "two", "three"));
        Observable<String> take = w.lift(new OperatorTake<String>(2));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        take.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testTake2() {
        Observable<String> w = Observable.from(Arrays.asList("one", "two", "three"));
        Observable<String> take = w.lift(new OperatorTake<String>(1));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        take.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTakeWithError() {
        Observable.from(Arrays.asList(1, 2, 3)).take(1).map(new Func1<Integer, Integer>() {
            public Integer call(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        }).toBlocking().single();
    }

    @Test
    public void testTakeWithErrorHappeningInOnNext() {
        Observable<Integer> w = Observable.from(Arrays.asList(1, 2, 3)).take(2).map(new Func1<Integer, Integer>() {
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
        Observable<Integer> w = Observable.from(Arrays.asList(1, 2, 3)).take(1).map(new Func1<Integer, Integer>() {
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
        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                observer.onNext("one");
                observer.onError(new Throwable("test failed"));
            }
        });

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        source.lift(new OperatorTake<String>(1)).subscribe(observer);

        verify(observer, times(1)).onNext("one");
        // even though onError is called we take(1) so shouldn't see it
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testTakeZeroDoesntLeakError() {
        final AtomicBoolean subscribed = new AtomicBoolean(false);
        final AtomicBoolean unSubscribed = new AtomicBoolean(false);
        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                subscribed.set(true);
                observer.add(new Subscription() {
                    @Override
                    public void unsubscribe() {
                        unSubscribed.set(true);
                    }

                    @Override
                    public boolean isUnsubscribed() {
                        return unSubscribed.get();
                    }
                });
                observer.onError(new Throwable("test failed"));
            }
        });

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        source.lift(new OperatorTake<String>(0)).subscribe(observer);
        assertTrue("source subscribed", subscribed.get());
        assertTrue("source unsubscribed", unSubscribed.get());

        verify(observer, never()).onNext(anyString());
        // even though onError is called we take(0) so shouldn't see it
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testUnsubscribeAfterTake() {
        final Subscription s = mock(Subscription.class);
        TestObservableFunc f = new TestObservableFunc("one", "two", "three");
        Observable<String> w = Observable.create(f);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        
        Subscriber<String> subscriber = Subscribers.from(observer);
        subscriber.add(s);
        
        Observable<String> take = w.lift(new OperatorTake<String>(1));
        take.subscribe(subscriber);

        // wait for the Observable to complete
        try {
            f.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        System.out.println("TestObservable thread finished");
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, times(1)).onCompleted();
        verify(s, times(1)).unsubscribe();
        verifyNoMoreInteractions(observer);
    }

    @Test(timeout = 2000)
    public void testUnsubscribeFromSynchronousInfiniteObservable() {
        final AtomicLong count = new AtomicLong();
        INFINITE_OBSERVABLE.take(10).subscribe(new Action1<Long>() {

            @Override
            public void call(Long l) {
                count.set(l);
            }

        });
        assertEquals(10, count.get());
    }

    @Test(timeout = 2000)
    public void testMultiTake() {
        final AtomicInteger count = new AtomicInteger();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> s) {
                for (int i = 0; !s.isUnsubscribed(); i++) {
                    System.out.println("Emit: " + i);
                    count.incrementAndGet();
                    s.onNext(i);
                }
            }

        }).take(100).take(1).toBlocking().forEach(new Action1<Integer>() {

            @Override
            public void call(Integer t1) {
                System.out.println("Receive: " + t1);

            }

        });

        assertEquals(1, count.get());
    }

    private static class TestObservableFunc implements Observable.OnSubscribe<String> {

        final String[] values;
        Thread t = null;

        public TestObservableFunc(String... values) {
            this.values = values;
        }

        @Override
        public void call(final Subscriber<? super String> observer) {
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
        }
    }

    private static Observable<Long> INFINITE_OBSERVABLE = Observable.create(new OnSubscribe<Long>() {

        @Override
        public void call(Subscriber<? super Long> op) {
            long l = 1;
            while (!op.isUnsubscribed()) {
                op.onNext(l++);
            }
            op.onCompleted();
        }

    });
    
    @Test(timeout = 2000)
    public void testTakeObserveOn() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        INFINITE_OBSERVABLE.observeOn(Schedulers.newThread()).take(1).subscribe(o);
        
        verify(o).onNext(1L);
        verify(o, never()).onNext(2L);
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
}
