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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public final class OperationMerge {

    /**
     * Flattens the observable sequences from the list of Observables into one observable sequence without any transformation.
     * 
     * @param source
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of flattening the output from the list of Observables.
     * @see http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx
     */
    public static <T> Func1<Observer<T>, Subscription> merge(final Observable<Observable<T>> o) {
        // wrap in a Func so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new Func1<Observer<T>, Subscription>() {

            @Override
            public Subscription call(Observer<T> observer) {
                return new MergeObservable<T>(o).call(observer);
            }
        };
    }

    public static <T> Func1<Observer<T>, Subscription> merge(final Observable<T>... sequences) {
        return merge(Observable.create(new Func1<Observer<Observable<T>>, Subscription>() {
            private volatile boolean unsubscribed = false;

            @Override
            public Subscription call(Observer<Observable<T>> observer) {
                for (Observable<T> o : sequences) {
                    if (!unsubscribed) {
                        observer.onNext(o);
                    } else {
                        // break out of the loop if we are unsubscribed
                        break;
                    }
                }
                if (!unsubscribed) {
                    observer.onCompleted();
                }
                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                        unsubscribed = true;
                    }

                };
            }
        }));
    }

    public static <T> Func1<Observer<T>, Subscription> merge(final List<Observable<T>> sequences) {
        return merge(Observable.create(new Func1<Observer<Observable<T>>, Subscription>() {

            private volatile boolean unsubscribed = false;

            @Override
            public Subscription call(Observer<Observable<T>> observer) {
                for (Observable<T> o : sequences) {
                    if (!unsubscribed) {
                        observer.onNext(o);
                    } else {
                        // break out of the loop if we are unsubscribed
                        break;
                    }
                }
                if (!unsubscribed) {
                    observer.onCompleted();
                }

                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                        unsubscribed = true;
                    }

                };
            }
        }));
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
    private static final class MergeObservable<T> implements Func1<Observer<T>, Subscription> {
        private final Observable<Observable<T>> sequences;
        private final MergeSubscription ourSubscription = new MergeSubscription();
        private AtomicBoolean stopped = new AtomicBoolean(false);
        private volatile boolean parentCompleted = false;
        private final ConcurrentHashMap<ChildObserver, ChildObserver> childObservers = new ConcurrentHashMap<ChildObserver, ChildObserver>();
        private final ConcurrentHashMap<ChildObserver, Subscription> childSubscriptions = new ConcurrentHashMap<ChildObserver, Subscription>();

        private MergeObservable(Observable<Observable<T>> sequences) {
            this.sequences = sequences;
        }

        public MergeSubscription call(Observer<T> actualObserver) {
            /**
             * Subscribe to the parent Observable to get to the children Observables
             */
            sequences.subscribe(new ParentObserver(actualObserver));

            /* return our subscription to allow unsubscribing */
            return ourSubscription;
        }

        /**
         * Manage the internal subscription with a thread-safe means of stopping/unsubscribing so we don't unsubscribe twice.
         * <p>
         * Also has the stop() method returning a boolean so callers know if their thread "won" and should perform further actions.
         */
        private class MergeSubscription implements Subscription {

            @Override
            public void unsubscribe() {
                stop();
            }

            public boolean stop() {
                // try setting to false unless another thread beat us
                boolean didSet = stopped.compareAndSet(false, true);
                if (didSet) {
                    // this thread won the race to stop, so unsubscribe from the actualSubscription
                    for (Subscription _s : childSubscriptions.values()) {
                        _s.unsubscribe();
                    }
                    return true;
                } else {
                    // another thread beat us
                    return false;
                }
            }
        }

        /**
         * Subscribe to the top level Observable to receive the sequence of Observable<T> children.
         * 
         * @param <T>
         */
        private class ParentObserver implements Observer<Observable<T>> {
            private final Observer<T> actualObserver;

            public ParentObserver(Observer<T> actualObserver) {
                this.actualObserver = actualObserver;
            }

            @Override
            public void onCompleted() {
                parentCompleted = true;
                // this *can* occur before the children are done, so if it does we won't send onCompleted
                // but will let the child worry about it
                // if however this completes and there are no children processing, then we will send onCompleted

                if (childObservers.size() == 0) {
                    if (!stopped.get()) {
                        if (ourSubscription.stop()) {
                            actualObserver.onCompleted();
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                actualObserver.onError(e);
            }

            @Override
            public void onNext(Observable<T> childObservable) {
                if (stopped.get()) {
                    // we won't act on any further items
                    return;
                }

                if (childObservable == null) {
                    throw new IllegalArgumentException("Observable<T> can not be null.");
                }

                /**
                 * For each child Observable we receive we'll subscribe with a separate Observer
                 * that will each then forward their sequences to the actualObserver.
                 * <p>
                 * We use separate child Observers for each sequence to simplify the onComplete/onError handling so each sequence has its own lifecycle.
                 */
                ChildObserver _w = new ChildObserver(actualObserver);
                childObservers.put(_w, _w);
                Subscription _subscription = childObservable.subscribe(_w);
                // remember this Observer and the subscription from it
                childSubscriptions.put(_w, _subscription);
            }
        }

        /**
         * Subscribe to each child Observable<T> and forward their sequence of data to the actualObserver
         * 
         */
        private class ChildObserver implements Observer<T> {

            private final Observer<T> actualObserver;

            public ChildObserver(Observer<T> actualObserver) {
                this.actualObserver = actualObserver;
            }

            @Override
            public void onCompleted() {
                // remove self from map of Observers
                childObservers.remove(this);
                // if there are now 0 Observers left, so if the parent is also completed we send the onComplete to the actualObserver
                // if the parent is not complete that means there is another sequence (and child Observer) to come
                if (!stopped.get()) {
                    if (childObservers.size() == 0 && parentCompleted) {
                        if (ourSubscription.stop()) {
                            // this thread 'won' the race to unsubscribe/stop so let's send onCompleted
                            actualObserver.onCompleted();
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (!stopped.get()) {
                    if (ourSubscription.stop()) {
                        // this thread 'won' the race to unsubscribe/stop so let's send the error
                        actualObserver.onError(e);
                    }
                }
            }

            @Override
            public void onNext(T args) {
                // in case the Observable is poorly behaved and doesn't listen to the unsubscribe request
                // we'll ignore anything that comes in after we've unsubscribed
                if (!stopped.get()) {
                    actualObserver.onNext(args);
                }
            }

        }
    }

    public static class UnitTest {
        @Mock
        Observer<String> stringObserver;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testMergeObservableOfObservables() {
            final Observable<String> o1 = new TestSynchronousObservable();
            final Observable<String> o2 = new TestSynchronousObservable();

            Observable<Observable<String>> observableOfObservables = Observable.create(new Func1<Observer<Observable<String>>, Subscription>() {

                @Override
                public Subscription call(Observer<Observable<String>> observer) {
                    // simulate what would happen in an observable
                    observer.onNext(o1);
                    observer.onNext(o2);
                    observer.onCompleted();

                    return new Subscription() {

                        @Override
                        public void unsubscribe() {
                            // unregister ... will never be called here since we are executing synchronously
                        }

                    };
                }

            });
            Observable<String> m = Observable.create(merge(observableOfObservables));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(1)).onCompleted();
            verify(stringObserver, times(2)).onNext("hello");
        }

        @Test
        public void testMergeArray() {
            final Observable<String> o1 = new TestSynchronousObservable();
            final Observable<String> o2 = new TestSynchronousObservable();

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(merge(o1, o2));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(2)).onNext("hello");
            verify(stringObserver, times(1)).onCompleted();
        }

        @Test
        public void testMergeList() {
            final Observable<String> o1 = new TestSynchronousObservable();
            final Observable<String> o2 = new TestSynchronousObservable();
            List<Observable<String>> listOfObservables = new ArrayList<Observable<String>>();
            listOfObservables.add(o1);
            listOfObservables.add(o2);

            Observable<String> m = Observable.create(merge(listOfObservables));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(1)).onCompleted();
            verify(stringObserver, times(2)).onNext("hello");
        }

        @Test
        public void testUnSubscribe() {
            TestObservable tA = new TestObservable();
            TestObservable tB = new TestObservable();

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(merge(tA, tB));
            Subscription s = m.subscribe(stringObserver);

            tA.sendOnNext("Aone");
            tB.sendOnNext("Bone");
            s.unsubscribe();
            tA.sendOnNext("Atwo");
            tB.sendOnNext("Btwo");
            tA.sendOnCompleted();
            tB.sendOnCompleted();

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(1)).onNext("Aone");
            verify(stringObserver, times(1)).onNext("Bone");
            assertTrue(tA.unsubscribed);
            assertTrue(tB.unsubscribed);
            verify(stringObserver, never()).onNext("Atwo");
            verify(stringObserver, never()).onNext("Btwo");
            verify(stringObserver, never()).onCompleted();
        }

        @Test
        public void testMergeArrayWithThreading() {
            final TestASynchronousObservable o1 = new TestASynchronousObservable();
            final TestASynchronousObservable o2 = new TestASynchronousObservable();

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(merge(o1, o2));
            m.subscribe(stringObserver);

            try {
                o1.t.join();
                o2.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(2)).onNext("hello");
            verify(stringObserver, times(1)).onCompleted();
        }

        /**
         * unit test from OperationMergeDelayError backported here to show how these use cases work with normal merge
         */
        @Test
        public void testError1() {
            // we are using synchronous execution to test this exactly rather than non-deterministic concurrent behavior
            final Observable<String> o1 = new TestErrorObservable("four", null, "six"); // we expect to lose "six"
            final Observable<String> o2 = new TestErrorObservable("one", "two", "three"); // we expect to lose all of these since o1 is done first and fails

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(merge(o1, o2));
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(0)).onNext("one");
            verify(stringObserver, times(0)).onNext("two");
            verify(stringObserver, times(0)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(0)).onNext("five");
            verify(stringObserver, times(0)).onNext("six");
        }

        /**
         * unit test from OperationMergeDelayError backported here to show how these use cases work with normal merge
         */
        @Test
        public void testError2() {
            // we are using synchronous execution to test this exactly rather than non-deterministic concurrent behavior
            final Observable<String> o1 = new TestErrorObservable("one", "two", "three");
            final Observable<String> o2 = new TestErrorObservable("four", null, "six"); // we expect to lose "six"
            final Observable<String> o3 = new TestErrorObservable("seven", "eight", null);// we expect to lose all of these since o2 is done first and fails
            final Observable<String> o4 = new TestErrorObservable("nine");// we expect to lose all of these since o2 is done first and fails

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(merge(o1, o2, o3, o4));
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(1)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(0)).onNext("five");
            verify(stringObserver, times(0)).onNext("six");
            verify(stringObserver, times(0)).onNext("seven");
            verify(stringObserver, times(0)).onNext("eight");
            verify(stringObserver, times(0)).onNext("nine");
        }

        private static class TestSynchronousObservable extends Observable<String> {

            @Override
            public Subscription subscribe(Observer<String> observer) {

                observer.onNext("hello");
                observer.onCompleted();

                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                        // unregister ... will never be called here since we are executing synchronously
                    }

                };
            }
        }

        private static class TestASynchronousObservable extends Observable<String> {
            Thread t;

            @Override
            public Subscription subscribe(final Observer<String> observer) {
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        observer.onNext("hello");
                        observer.onCompleted();
                    }

                });
                t.start();

                return new Subscription() {

                    @Override
                    public void unsubscribe() {

                    }

                };
            }
        }

        /**
         * A Observable that doesn't do the right thing on UnSubscribe/Error/etc in that it will keep sending events down the pipe regardless of what happens.
         */
        private static class TestObservable extends Observable<String> {

            Observer<String> observer = null;
            volatile boolean unsubscribed = false;
            Subscription s = new Subscription() {

                @Override
                public void unsubscribe() {
                    unsubscribed = true;

                }

            };

            /* used to simulate subscription */
            public void sendOnCompleted() {
                observer.onCompleted();
            }

            /* used to simulate subscription */
            public void sendOnNext(String value) {
                observer.onNext(value);
            }

            /* used to simulate subscription */
            @SuppressWarnings("unused")
            public void sendOnError(Exception e) {
                observer.onError(e);
            }

            @Override
            public Subscription subscribe(final Observer<String> observer) {
                this.observer = observer;
                return s;
            }
        }

        private static class TestErrorObservable extends Observable<String> {

            String[] valuesToReturn;

            TestErrorObservable(String... values) {
                valuesToReturn = values;
            }

            @Override
            public Subscription subscribe(Observer<String> observer) {

                for (String s : valuesToReturn) {
                    if (s == null) {
                        System.out.println("throwing exception");
                        observer.onError(new NullPointerException());
                    } else {
                        observer.onNext(s);
                    }
                }
                observer.onCompleted();

                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                        // unregister ... will never be called here since we are executing synchronously
                    }

                };
            }
        }
    }
}
