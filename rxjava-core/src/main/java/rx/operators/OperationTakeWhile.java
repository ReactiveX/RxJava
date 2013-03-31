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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.AtomicObservableSubscription;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.subjects.Subject;
/**
 * Returns values from an observable sequence as long as a specified condition is true, and then skips the remaining values.
 */
public final class OperationTakeWhile {

    /**
     * Returns a specified number of contiguous values from the start of an observable sequence.
     * 
     * @param items
     * @param predicate
     *            a function to test each source element for a condition
     * @return
     */
    public static <T> Func1<Observer<T>, Subscription> takeWhile(final Observable<T> items, final Func1<T, Boolean> predicate) {
        return takeWhileWithIndex(items, OperationTakeWhile.<T>skipIndex(predicate));
    }

    /**
     * Returns values from an observable sequence as long as a specified condition is true, and then skips the remaining values.
     * 
     * @param items
     * @param predicate
     *            a function to test each element for a condition; the second parameter of the function represents the index of the source element; otherwise, false.
     * @return
     */
    public static <T> Func1<Observer<T>, Subscription> takeWhileWithIndex(final Observable<T> items, final Func2<T, Integer, Boolean> predicate) {
        // wrap in a Func so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new Func1<Observer<T>, Subscription>() {

            @Override
            public Subscription call(Observer<T> observer) {
                return new TakeWhile<T>(items, predicate).call(observer);
            }

        };
    }

    private static <T> Func2<T, Integer, Boolean> skipIndex(final Func1<T, Boolean> underlying) {
        return new Func2<T, Integer, Boolean>() {
            @Override
            public Boolean call(T input, Integer index) {
                return underlying.call(input);
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
     * Note how the takeWhileWithIndex() factory method above protects us from a single instance being exposed with the Observable wrapper handling the subscribe flow.
     * 
     * @param <T>
     */
    private static class TakeWhile<T> implements Func1<Observer<T>, Subscription> {
        private final AtomicInteger counter = new AtomicInteger();
        private final Observable<T> items;
        private final Func2<T, Integer, Boolean> predicate;
        private final AtomicObservableSubscription subscription = new AtomicObservableSubscription();

        private TakeWhile(Observable<T> items, Func2<T, Integer, Boolean> predicate) {
            this.items = items;
            this.predicate = predicate;
        }

        @Override
        public Subscription call(Observer<T> observer) {
            return subscription.wrap(items.subscribe(new ItemObserver(observer)));
        }

        private class ItemObserver implements Observer<T> {
            private final Observer<T> observer;

            public ItemObserver(Observer<T> observer) {
                this.observer = observer;
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }

            @Override
            public void onError(Exception e) {
                observer.onError(e);
            }

            @Override
            public void onNext(T args) {
                if (predicate.call(args, counter.getAndIncrement())) {
                    observer.onNext(args);
                } else {
                    observer.onCompleted();
                    // this will work if the sequence is asynchronous, it will have no effect on a synchronous observable
                    subscription.unsubscribe();
                }
            }

        }

    }

    public static class UnitTest {

        @Test
        public void testTakeWhile1() {
            Observable<Integer> w = Observable.toObservable(1, 2, 3);
            Observable<Integer> take = Observable.create(takeWhile(w, new Func1<Integer, Boolean>()
            {
                @Override
                public Boolean call(Integer input)
                {
                    return input < 3;
                }
            }));

            @SuppressWarnings("unchecked")
            Observer<Integer> aObserver = mock(Observer.class);
            take.subscribe(aObserver);
            verify(aObserver, times(1)).onNext(1);
            verify(aObserver, times(1)).onNext(2);
            verify(aObserver, never()).onNext(3);
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testTakeWhileOnSubject1() {
            Subject<Integer> s = Subject.create();
            Observable<Integer> w = (Observable<Integer>)s;
            Observable<Integer> take = Observable.create(takeWhile(w, new Func1<Integer, Boolean>()
            {
                @Override
                public Boolean call(Integer input)
                {
                    return input < 3;
                }
            }));

            @SuppressWarnings("unchecked")
            Observer<Integer> aObserver = mock(Observer.class);
            take.subscribe(aObserver);

            s.onNext(1);
            s.onNext(2);
            s.onNext(3);
            s.onNext(4);
            s.onNext(5);
            s.onCompleted();

            verify(aObserver, times(1)).onNext(1);
            verify(aObserver, times(1)).onNext(2);
            verify(aObserver, never()).onNext(3);
            verify(aObserver, never()).onNext(4);
            verify(aObserver, never()).onNext(5);
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testTakeWhile2() {
            Observable<String> w = Observable.toObservable("one", "two", "three");
            Observable<String> take = Observable.create(takeWhileWithIndex(w, new Func2<String, Integer, Boolean>()
            {
                @Override
                public Boolean call(String input, Integer index)
                {
                    return index < 2;
                }
            }));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            take.subscribe(aObserver);
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, never()).onNext("three");
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testTakeWhileDoesntLeakErrors() {
            Observable<String> source = Observable.create(new Func1<Observer<String>, Subscription>()
            {
                @Override
                public Subscription call(Observer<String> observer)
                {
                    observer.onNext("one");
                    observer.onError(new Exception("test failed"));
                    return Subscriptions.empty();
                }
            });

            Observable.create(takeWhile(source, new Func1<String, Boolean>()
            {
                @Override
                public Boolean call(String s)
                {
                    return false;
                }
            })).last();
        }

        @Test
        public void testUnsubscribeAfterTake() {
            Subscription s = mock(Subscription.class);
            TestObservable w = new TestObservable(s, "one", "two", "three");

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            Observable<String> take = Observable.create(takeWhileWithIndex(w, new Func2<String, Integer, Boolean>()
            {
                @Override
                public Boolean call(String s, Integer index)
                {
                    return index < 1;
                }
            }));
            take.subscribe(aObserver);

            // wait for the Observable to complete
            try {
                w.t.join();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            System.out.println("TestObservable thread finished");
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, never()).onNext("two");
            verify(aObserver, never()).onNext("three");
            verify(s, times(1)).unsubscribe();
        }

        private static class TestObservable extends Observable<String> {

            final Subscription s;
            final String[] values;
            Thread t = null;

            public TestObservable(Subscription s, String... values) {
                this.s = s;
                this.values = values;
            }

            @Override
            public Subscription subscribe(final Observer<String> observer) {
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
                        } catch (Exception e) {
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
