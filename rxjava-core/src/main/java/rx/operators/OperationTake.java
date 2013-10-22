/**
 * Copyright 2013 Netflix, Inc.
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

/**
 * Returns an Observable that emits the first <code>num</code> items emitted by the source
 * Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/take.png">
 * <p>
 * You can choose to pay attention only to the first <code>num</code> items emitted by an
 * Observable by using the take operation. This operation returns an Observable that will invoke a
 * subscribing Observer's <code>onNext</code> function a maximum of <code>num</code> times before
 * invoking <code>onCompleted</code>.
 */
public final class OperationTake {

    /**
     * Returns a specified number of contiguous values from the start of an observable sequence.
     * 
     * @param items
     * @param num
     * @return the specified number of contiguous values from the start of the given observable sequence
     */
    public static <T> OnSubscribeFunc<T> take(final Observable<? extends T> items, final int num) {
        // wrap in a Func so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new Take<T>(items, num).onSubscribe(observer);
            }

        };
    }

    /**
     * This class is NOT thread-safe if invoked and referenced multiple times. In other words, don't subscribe to it multiple times from different threads.
     * <p>
     * It IS thread-safe from within it while receiving onNext events from multiple threads.
     * <p>
     * This should all be fine as long as it's kept as a private class and a new instance created from static factory method above.
     * <p>
     * Note how the take() factory method above protects us from a single instance being exposed with the Observable wrapper handling the subscribe flow.
     * 
     * @param <T>
     */
    private static class Take<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> items;
        private final int num;
        private final SafeObservableSubscription subscription = new SafeObservableSubscription();

        private Take(Observable<? extends T> items, int num) {
            this.items = items;
            this.num = num;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            if (num < 1) {
                items.subscribe(new Observer<T>()
                {
                    @Override
                    public void onCompleted()
                    {
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                    }

                    @Override
                    public void onNext(T args)
                    {
                    }
                }).unsubscribe();
                observer.onCompleted();
                return Subscriptions.empty();
            }

            return subscription.wrap(items.subscribe(new ItemObserver(observer)));
        }

        private class ItemObserver implements Observer<T> {
            private final Observer<? super T> observer;

            private final AtomicInteger counter = new AtomicInteger();
            private volatile boolean hasEmitedError = false;

            public ItemObserver(Observer<? super T> observer) {
                this.observer = observer;
            }

            @Override
            public void onCompleted() {
                if (hasEmitedError) {
                    return;
                }
                if (counter.getAndSet(num) < num) {
                    observer.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (hasEmitedError) {
                    return;
                }
                if (counter.getAndSet(num) < num) {
                    observer.onError(e);
                }
            }

            @Override
            public void onNext(T args) {
                if (hasEmitedError) {
                    return;
                }
                final int count = counter.incrementAndGet();
                if (count <= num) {
                    try {
                        observer.onNext(args);
                    } catch (Throwable ex) {
                        hasEmitedError = true;
                        observer.onError(ex);
                        subscription.unsubscribe();
                        return;
                    }
                    if (count == num) {
                        observer.onCompleted();
                    }
                }
                if (count >= num) {
                    // this will work if the sequence is asynchronous, it will have no effect on a synchronous observable
                    subscription.unsubscribe();
                }
            }

        }

    }

    public static class UnitTest {

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
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>()
            {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer)
                {
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
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>()
            {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer)
                {
                    subscribed.set(true);
                    observer.onError(new Throwable("test failed"));
                    return new Subscription()
                    {
                        @Override
                        public void unsubscribe()
                        {
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

        private static class TestObservableFunc implements OnSubscribeFunc<String> {

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

    }

}
