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

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

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
}
