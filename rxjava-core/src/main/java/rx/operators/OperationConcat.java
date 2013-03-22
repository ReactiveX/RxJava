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
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.util.AtomicObservableSubscription;
import rx.util.Exceptions;
import rx.util.functions.Func1;

public final class OperationConcat {

    /**
     * Combine the observable sequences from the list of Observables into one
     * observable sequence without any transformation.  If either the outer
     * observable or an inner observable calls onError, we will call onError.
     *
     * <p/>
     *
     * The outer observable might run on a separate thread from (one of) the
     * inner observables; in this case care must be taken to avoid a deadlock.
     * The Concat operation may block the outer thread while servicing an inner
     * thread in order to ensure a well-defined ordering of elements; therefore
     * none of the inner threads must be implemented in a way that might wait on
     * the outer thread.
     *
     * @param sequences An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of combining the output from the list of Observables.
     */
    public static <T> Func1<Observer<T>, Subscription> concat(final Observable<T>... sequences) {
        return concat(Observable.toObservable(sequences));
    }

    public static <T> Func1<Observer<T>, Subscription> concat(final List<Observable<T>> sequences) {
        return concat(Observable.toObservable(sequences));
    }

    public static <T> Func1<Observer<T>, Subscription> concat(final Observable<Observable<T>> sequences) {
        return new Func1<Observer<T>, Subscription>() {

            @Override
            public Subscription call(Observer<T> observer) {
                return new ConcatSubscription<T>(sequences, observer);
            }
        };
    }

    private static class ConcatSubscription<T> extends BooleanSubscription {
        // Might be updated by an inner thread's onError during the outer
        // thread's onNext, then read in the outer thread's onComplete.
        final AtomicBoolean innerError = new AtomicBoolean(false);

        public ConcatSubscription(Observable<Observable<T>> sequences, final Observer<T> observer) {
            final AtomicObservableSubscription outerSubscription = new AtomicObservableSubscription();
            outerSubscription.wrap(sequences.subscribe(new Observer<Observable<T>>() {
                @Override
                public void onNext(Observable<T> nextSequence) {
                    // We will not return from onNext until the inner observer completes.
                    // NB: while we are in onNext, the well-behaved outer observable will not call onError or onCompleted.
                    final CountDownLatch latch = new CountDownLatch(1);
                    final AtomicObservableSubscription innerSubscription = new AtomicObservableSubscription();
                    innerSubscription.wrap(nextSequence.subscribe(new Observer<T>() {
                        @Override
                        public void onNext(T item) {
                            // If the Concat's subscriber called unsubscribe() before the return of onNext, we must do so also.
                            observer.onNext(item);
                            if (isUnsubscribed()) {
                                innerSubscription.unsubscribe();
                                outerSubscription.unsubscribe();
                            }
                        }
                        @Override
                        public void onError(Exception e) {
                            outerSubscription.unsubscribe();
                            innerError.set(true);
                            observer.onError(e);
                            latch.countDown();
                        }
                        @Override
                        public void onCompleted() {
                            // Continue on to the next sequence
                            latch.countDown();
                        }
                    }));
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw Exceptions.propagate(e);
                    }
                }
                @Override
                public void onError(Exception e) {
                    // NB: a well-behaved observable will not interleave on{Next,Error,Completed} calls.
                    observer.onError(e);
                }
                @Override
                public void onCompleted() {
                    // NB: a well-behaved observable will not interleave on{Next,Error,Completed} calls.
                    if (!innerError.get()) {
                        observer.onCompleted();
                    }
                }
            }));
        }                
    }

    public static class UnitTest {
        private final static String[] expected = { "1", "3", "5", "7", "2", "4", "6" };
        private int index = 0;

        Observer<String> observer = new Observer<String>() {

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Exception e) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onNext(String args) {
                Assert.assertEquals(expected[index], args);
                index++;
            }
        };

        @Before
        public void before() {
            index = 0;
        }

        @Test
        public void testConcat() {
            final String[] o = { "1", "3", "5", "7" };
            final String[] e = { "2", "4", "6" };

            final Observable<String> odds = Observable.toObservable(o);
            final Observable<String> even = Observable.toObservable(e);

            @SuppressWarnings("unchecked")
            Observable<String> concat = Observable.create(concat(odds, even));
            concat.subscribe(observer);
            Assert.assertEquals(expected.length, index);

        }

        @Test
        public void testConcatWithList() {
            final String[] o = { "1", "3", "5", "7" };
            final String[] e = { "2", "4", "6" };

            final Observable<String> odds = Observable.toObservable(o);
            final Observable<String> even = Observable.toObservable(e);
            final List<Observable<String>> list = new ArrayList<Observable<String>>();
            list.add(odds);
            list.add(even);
            Observable<String> concat = Observable.create(concat(list));
            concat.subscribe(observer);
            Assert.assertEquals(expected.length, index);

        }

        @Test
        public void testConcatUnsubscribe() {
            final CountDownLatch callOnce = new CountDownLatch(1);
            final CountDownLatch okToContinue = new CountDownLatch(1);
            final TestObservable<String> w1 = new TestObservable<String>(null, null, "one", "two", "three");
            final TestObservable<String> w2 = new TestObservable<String>(callOnce, okToContinue, "four", "five", "six");

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            @SuppressWarnings("unchecked")
            Observable<String> concat = Observable.create(concat(w1, w2));
            Subscription s1 = concat.subscribe(aObserver);

            try {
                //Block main thread to allow observable "w1" to complete and observable "w2" to call onNext once.
                callOnce.await();
                s1.unsubscribe();
                //Unblock the observable to continue.
                okToContinue.countDown();
                w1.t.join();
                w2.t.join();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, times(1)).onNext("four");
            verify(aObserver, never()).onNext("five");
            verify(aObserver, never()).onNext("six");
        }

        @Test
        public void testMergeObservableOfObservables() {
            final String[] o = { "1", "3", "5", "7" };
            final String[] e = { "2", "4", "6" };

            final Observable<String> odds = Observable.toObservable(o);
            final Observable<String> even = Observable.toObservable(e);

            Observable<Observable<String>> observableOfObservables = Observable.create(new Func1<Observer<Observable<String>>, Subscription>() {

                @Override
                public Subscription call(Observer<Observable<String>> observer) {
                    // simulate what would happen in an observable
                    observer.onNext(odds);
                    observer.onNext(even);
                    observer.onCompleted();

                    return new Subscription() {

                        @Override
                        public void unsubscribe() {
                            // unregister ... will never be called here since we are executing synchronously
                        }

                    };
                }

            });
            Observable<String> concat = Observable.create(concat(observableOfObservables));
            concat.subscribe(observer);
            Assert.assertEquals(expected.length, index);
        }

        @Test
        public void testBlockedObservableOfObservables() {
            final String[] o = { "1", "3", "5", "7" };
            final String[] e = { "2", "4", "6" };
            final Observable<String> odds = Observable.toObservable(o);
            final Observable<String> even = Observable.toObservable(e);
            final CountDownLatch callOnce = new CountDownLatch(1);
            final CountDownLatch okToContinue = new CountDownLatch(1);
            TestObservable<Observable<String>> observableOfObservables = new TestObservable<Observable<String>>(callOnce, okToContinue, odds, even);
            Func1<Observer<String>, Subscription> concatF = concat(observableOfObservables);
            Observable<String> concat = Observable.create(concatF);
            concat.subscribe(observer);
            try {
                //Block main thread to allow observables to serve up o1.
                callOnce.await();
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
            // The concated observable should have served up all of the odds.
            Assert.assertEquals(o.length, index);
            try {
                // unblock observables so it can serve up o2 and complete
                okToContinue.countDown();
                observableOfObservables.t.join();
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
            // The concatenated observable should now have served up all the evens.
            Assert.assertEquals(expected.length, index);
        }

        private static class TestObservable<T> extends Observable<T> {

            private final Subscription s = new Subscription() {

                @Override
                public void unsubscribe() {
                    subscribed = false;
                }

            };
            private final List<T> values;
            private Thread t = null;
            private int count = 0;
            private boolean subscribed = true;
            private final CountDownLatch once;
            private final CountDownLatch okToContinue;

            public TestObservable(CountDownLatch once, CountDownLatch okToContinue, T... values) {
                this.values = Arrays.asList(values);
                this.once = once;
                this.okToContinue = okToContinue;
            }

            @Override
            public Subscription subscribe(final Observer<T> observer) {
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            while (count < values.size() && subscribed) {
                                observer.onNext(values.get(count));
                                count++;
                                //Unblock the main thread to call unsubscribe.
                                if (null != once)
                                    once.countDown();
                                //Block until the main thread has called unsubscribe.
                                if (null != once)
                                    okToContinue.await();
                            }
                            if (subscribed)
                                observer.onCompleted();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            fail(e.getMessage());
                        }
                    }

                });
                t.start();
                return s;
            }

        }

    }
}