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

import java.util.LinkedList;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.observers.SerializedObserver;
import rx.subscriptions.CompositeSubscription;

/**
 * Flattens a list of Observables into one Observable sequence, without any transformation.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/merge.png">
 * <p>
 * You can combine the items emitted by multiple Observables so that they act like a single
 * Observable, by using the merge operation.
 */
public final class OperationMergeMaxConcurrent {

    public static <T> OnSubscribeFunc<T> merge(final Observable<? extends Observable<? extends T>> o, final int maxConcurrent) {
        if (maxConcurrent <= 0) {
            throw new IllegalArgumentException("maxConcurrent must be positive");
        }
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new MergeObservable<T>(o, maxConcurrent).onSubscribe(observer);
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
    private static final class MergeObservable<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends Observable<? extends T>> sequences;
        private final CompositeSubscription ourSubscription = new CompositeSubscription();
        private volatile boolean parentCompleted = false;
        private final LinkedList<Observable<? extends T>> pendingObservables = new LinkedList<Observable<? extends T>>();
        private volatile int activeObservableCount = 0;
        private final int maxConcurrent;
        /**
         * Protect both pendingObservables and activeObservableCount from concurrent accesses.
         */
        private final Object gate = new Object();

        private MergeObservable(Observable<? extends Observable<? extends T>> sequences, int maxConcurrent) {
            this.sequences = sequences;
            this.maxConcurrent = maxConcurrent;
        }

        public Subscription onSubscribe(Observer<? super T> actualObserver) {

            /**
             * We must synchronize a merge because we subscribe to multiple sequences in parallel that will each be emitting.
             * <p>
             * The calls from each sequence must be serialized.
             * <p>
             * Bug report: https://github.com/Netflix/RxJava/issues/200
             */
            SafeObservableSubscription subscription = new SafeObservableSubscription(ourSubscription);
            SerializedObserver<T> synchronizedObserver = new SerializedObserver<T>(
                    new SafeObserver<T>(subscription, actualObserver)); // Create a SafeObserver as SynchronizedObserver does not automatically unsubscribe

            /**
             * Subscribe to the parent Observable to get to the children Observables
             */
            ourSubscription.add(sequences.subscribe(new ParentObserver(synchronizedObserver)));

            return subscription;
        }

        /**
         * Subscribe to the top level Observable to receive the sequence of Observable<T> children.
         * 
         * @param <T>
         */
        private class ParentObserver implements Observer<Observable<? extends T>> {
            private final SerializedObserver<T> serializedObserver;

            public ParentObserver(SerializedObserver<T> serializedObserver) {
                this.serializedObserver = serializedObserver;
            }

            @Override
            public void onCompleted() {
                parentCompleted = true;
                if (ourSubscription.isUnsubscribed()) {
                    return;
                }
                // this *can* occur before the children are done, so if it does we won't send onCompleted
                // but will let the child worry about it
                // if however this completes and there are no children processing, then we will send onCompleted
                if (isStopped()) {
                    serializedObserver.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                serializedObserver.onError(e);
            }

            @Override
            public void onNext(Observable<? extends T> childObservable) {
                if (ourSubscription.isUnsubscribed()) {
                    // we won't act on any further items
                    return;
                }

                if (childObservable == null) {
                    throw new IllegalArgumentException("Observable<T> can not be null.");
                }

                Observable<? extends T> observable = null;
                synchronized (gate) {
                    if (activeObservableCount >= maxConcurrent) {
                        pendingObservables.add(childObservable);
                    }
                    else {
                        observable = childObservable;
                        activeObservableCount++;
                    }
                }
                if (observable != null) {
                    ourSubscription.add(observable.subscribe(new ChildObserver(
                            serializedObserver)));
                }
            }
        }

        /**
         * Subscribe to each child Observable<T> and forward their sequence of data to the actualObserver
         * 
         */
        private class ChildObserver implements Observer<T> {

            private final SerializedObserver<T> serializedObserver;

            public ChildObserver(SerializedObserver<T> serializedObserver) {
                this.serializedObserver = serializedObserver;
            }

            @Override
            public void onCompleted() {
                if (ourSubscription.isUnsubscribed()) {
                    return;
                }

                Observable<? extends T> childObservable = null;
                // Try to fetch a pending observable
                synchronized (gate) {
                    childObservable = pendingObservables.poll();
                    if (childObservable == null) {
                        // There is no pending observable, decrease activeObservableCount.
                        activeObservableCount--;
                    }
                    else {
                        // Fetch an observable successfully.
                        // We will subscribe(this) at once. So don't change activeObservableCount.
                    }
                }
                if (childObservable != null) {
                    ourSubscription.add(childObservable.subscribe(this));
                } else {
                    // No pending observable. Need to check if it's necessary to emit an onCompleted
                    if (isStopped()) {
                        serializedObserver.onCompleted();
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                serializedObserver.onError(e);
            }

            @Override
            public void onNext(T args) {
                serializedObserver.onNext(args);
            }

        }

        private boolean isStopped() {
            synchronized (gate) {
                return parentCompleted && activeObservableCount == 0
                        && pendingObservables.size() == 0;
            }
        }
    }
}