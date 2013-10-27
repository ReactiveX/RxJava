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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

/**
 * Flattens a list of Observables into one Observable sequence, without any transformation.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/merge.png">
 * <p>
 * You can combine the items emitted by multiple Observables so that they act like a single
 * Observable, by using the merge operation.
 */
public final class OperationMerge {

    /**
     * Flattens the observable sequences from the list of Observables into one observable sequence without any transformation.
     * 
     * @param o
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of flattening the output from the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">Observable.Merge(TSource) Method (IObservable(TSource)[])</a>
     */
    public static <T> OnSubscribeFunc<T> merge(final Observable<? extends Observable<? extends T>> o) {
        // wrap in a Func so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new MergeObservable<T>(o).onSubscribe(observer);
            }
        };
    }

    public static <T> OnSubscribeFunc<T> merge(final Observable<? extends T>... sequences) {
        return merge(Arrays.asList(sequences));
    }

    public static <T> OnSubscribeFunc<T> merge(final Iterable<? extends Observable<? extends T>> sequences) {
        return merge(Observable.create(new OnSubscribeFunc<Observable<? extends T>>() {

            private volatile boolean unsubscribed = false;

            @Override
            public Subscription onSubscribe(Observer<? super Observable<? extends T>> observer) {
                for (Observable<? extends T> o : sequences) {
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
    private static final class MergeObservable<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends Observable<? extends T>> sequences;
        private final MergeSubscription ourSubscription = new MergeSubscription();
        private AtomicBoolean stopped = new AtomicBoolean(false);
        private volatile boolean parentCompleted = false;
        private final ConcurrentHashMap<ChildObserver, ChildObserver> childObservers = new ConcurrentHashMap<ChildObserver, ChildObserver>();
        private final ConcurrentHashMap<ChildObserver, Subscription> childSubscriptions = new ConcurrentHashMap<ChildObserver, Subscription>();

        private MergeObservable(Observable<? extends Observable<? extends T>> sequences) {
            this.sequences = sequences;
        }

        public Subscription onSubscribe(Observer<? super T> actualObserver) {
            CompositeSubscription completeSubscription = new CompositeSubscription();

            /**
             * We must synchronize a merge because we subscribe to multiple sequences in parallel that will each be emitting.
             * <p>
             * The calls from each sequence must be serialized.
             * <p>
             * Bug report: https://github.com/Netflix/RxJava/issues/200
             */
            SafeObservableSubscription subscription = new SafeObservableSubscription(ourSubscription);
            completeSubscription.add(subscription);
            SynchronizedObserver<T> synchronizedObserver = new SynchronizedObserver<T>(actualObserver, subscription);

            /**
             * Subscribe to the parent Observable to get to the children Observables
             */
            completeSubscription.add(sequences.subscribe(new ParentObserver(synchronizedObserver)));

            /* return our subscription to allow unsubscribing */
            return completeSubscription;
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
        private class ParentObserver implements Observer<Observable<? extends T>> {
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
            public void onError(Throwable e) {
                actualObserver.onError(e);
            }

            @Override
            public void onNext(Observable<? extends T> childObservable) {
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
            public void onError(Throwable e) {
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
            final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
            final Observable<String> o2 = Observable.create(new TestSynchronousObservable());

            Observable<Observable<String>> observableOfObservables = Observable.create(new OnSubscribeFunc<Observable<String>>() {

                @Override
                public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
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

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(1)).onCompleted();
            verify(stringObserver, times(2)).onNext("hello");
        }

        @Test
        public void testMergeArray() {
            final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
            final Observable<String> o2 = Observable.create(new TestSynchronousObservable());

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(merge(o1, o2));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(2)).onNext("hello");
            verify(stringObserver, times(1)).onCompleted();
        }

        @Test
        public void testMergeList() {
            final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
            final Observable<String> o2 = Observable.create(new TestSynchronousObservable());
            List<Observable<String>> listOfObservables = new ArrayList<Observable<String>>();
            listOfObservables.add(o1);
            listOfObservables.add(o2);

            Observable<String> m = Observable.create(merge(listOfObservables));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(1)).onCompleted();
            verify(stringObserver, times(2)).onNext("hello");
        }

        @Test
        public void testUnSubscribe() {
            TestObservable tA = new TestObservable();
            TestObservable tB = new TestObservable();

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(merge(Observable.create(tA), Observable.create(tB)));
            Subscription s = m.subscribe(stringObserver);

            tA.sendOnNext("Aone");
            tB.sendOnNext("Bone");
            s.unsubscribe();
            tA.sendOnNext("Atwo");
            tB.sendOnNext("Btwo");
            tA.sendOnCompleted();
            tB.sendOnCompleted();

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(1)).onNext("Aone");
            verify(stringObserver, times(1)).onNext("Bone");
            assertTrue(tA.unsubscribed);
            assertTrue(tB.unsubscribed);
            verify(stringObserver, never()).onNext("Atwo");
            verify(stringObserver, never()).onNext("Btwo");
            verify(stringObserver, never()).onCompleted();
        }

        @Test
        public void testUnSubscribeObservableOfObservables() throws InterruptedException {

            final AtomicBoolean unsubscribed = new AtomicBoolean();
            final CountDownLatch latch = new CountDownLatch(1);

            Observable<Observable<Long>> source = Observable.create(new OnSubscribeFunc<Observable<Long>>() {

                @Override
                public Subscription onSubscribe(final Observer<? super Observable<Long>> observer) {
                    // verbose on purpose so I can track the inside of it
                    final Subscription s = Subscriptions.create(new Action0() {

                        @Override
                        public void call() {
                            System.out.println("*** unsubscribed");
                            unsubscribed.set(true);
                        }

                    });

                    new Thread(new Runnable() {

                        @Override
                        public void run() {

                            while (!unsubscribed.get()) {
                                observer.onNext(Observable.from(1L, 2L));
                            }
                            System.out.println("Done looping after unsubscribe: " + unsubscribed.get());
                            observer.onCompleted();

                            // mark that the thread is finished
                            latch.countDown();
                        }
                    }).start();

                    return s;
                };

            });

            final AtomicInteger count = new AtomicInteger();
            Observable.create(merge(source)).take(6).toBlockingObservable().forEach(new Action1<Long>() {

                @Override
                public void call(Long v) {
                    System.out.println("Value: " + v);
                    int c = count.incrementAndGet();
                    if (c > 6) {
                        fail("Should be only 6");
                    }

                }
            });

            latch.await(1000, TimeUnit.MILLISECONDS);

            System.out.println("unsubscribed: " + unsubscribed.get());

            assertTrue(unsubscribed.get());

        }

        @Test
        public void testMergeArrayWithThreading() {
            final TestASynchronousObservable o1 = new TestASynchronousObservable();
            final TestASynchronousObservable o2 = new TestASynchronousObservable();

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(merge(Observable.create(o1), Observable.create(o2)));
            m.subscribe(stringObserver);

            try {
                o1.t.join();
                o2.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(2)).onNext("hello");
            verify(stringObserver, times(1)).onCompleted();
        }

        @Test
        public void testSynchronizationOfMultipleSequences() throws Throwable {
            final TestASynchronousObservable o1 = new TestASynchronousObservable();
            final TestASynchronousObservable o2 = new TestASynchronousObservable();

            // use this latch to cause onNext to wait until we're ready to let it go
            final CountDownLatch endLatch = new CountDownLatch(1);

            final AtomicInteger concurrentCounter = new AtomicInteger();
            final AtomicInteger totalCounter = new AtomicInteger();

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(merge(Observable.create(o1), Observable.create(o2)));
            m.subscribe(new Observer<String>() {

                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    throw new RuntimeException("failed", e);
                }

                @Override
                public void onNext(String v) {
                    totalCounter.incrementAndGet();
                    concurrentCounter.incrementAndGet();
                    try {
                        // wait here until we're done asserting
                        endLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException("failed", e);
                    } finally {
                        concurrentCounter.decrementAndGet();
                    }
                }

            });

            // wait for both observables to send (one should be blocked)
            o1.onNextBeingSent.await();
            o2.onNextBeingSent.await();

            // I can't think of a way to know for sure that both threads have or are trying to send onNext
            // since I can't use a CountDownLatch for "after" onNext since I want to catch during it
            // but I can't know for sure onNext is invoked
            // so I'm unfortunately reverting to using a Thread.sleep to allow the process scheduler time
            // to make sure after o1.onNextBeingSent and o2.onNextBeingSent are hit that the following
            // onNext is invoked.

            Thread.sleep(300);

            try { // in try/finally so threads are released via latch countDown even if assertion fails
                assertEquals(1, concurrentCounter.get());
            } finally {
                // release so it can finish
                endLatch.countDown();
            }

            try {
                o1.t.join();
                o2.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertEquals(2, totalCounter.get());
            assertEquals(0, concurrentCounter.get());
        }

        /**
         * unit test from OperationMergeDelayError backported here to show how these use cases work with normal merge
         */
        @Test
        public void testError1() {
            // we are using synchronous execution to test this exactly rather than non-deterministic concurrent behavior
            final Observable<String> o1 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six"
            final Observable<String> o2 = Observable.create(new TestErrorObservable("one", "two", "three")); // we expect to lose all of these since o1 is done first and fails

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
            final Observable<String> o1 = Observable.create(new TestErrorObservable("one", "two", "three"));
            final Observable<String> o2 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six"
            final Observable<String> o3 = Observable.create(new TestErrorObservable("seven", "eight", null));// we expect to lose all of these since o2 is done first and fails
            final Observable<String> o4 = Observable.create(new TestErrorObservable("nine"));// we expect to lose all of these since o2 is done first and fails

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

        private static class TestSynchronousObservable implements OnSubscribeFunc<String> {

            @Override
            public Subscription onSubscribe(Observer<? super String> observer) {

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

        private static class TestASynchronousObservable implements OnSubscribeFunc<String> {
            Thread t;
            final CountDownLatch onNextBeingSent = new CountDownLatch(1);

            @Override
            public Subscription onSubscribe(final Observer<? super String> observer) {
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        onNextBeingSent.countDown();
                        observer.onNext("hello");
                        // I can't use a countDownLatch to prove we are actually sending 'onNext'
                        // since it will block if synchronized and I'll deadlock
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
        private static class TestObservable implements OnSubscribeFunc<String> {

            Observer<? super String> observer = null;
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
            public void sendOnError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public Subscription onSubscribe(final Observer<? super String> observer) {
                this.observer = observer;
                return s;
            }
        }

        private static class TestErrorObservable implements OnSubscribeFunc<String> {

            String[] valuesToReturn;

            TestErrorObservable(String... values) {
                valuesToReturn = values;
            }

            @Override
            public Subscription onSubscribe(Observer<? super String> observer) {

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
