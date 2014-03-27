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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.observers.SerializedObserver;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;

/**
 * This behaves like {@link OperatorMerge} except that if any of the merged Observables notify of
 * an error via <code>onError</code>, mergeDelayError will refrain from propagating that error
 * notification until all of the merged Observables have finished emitting items.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/mergeDelayError.png">
 * <p>
 * Even if multiple merged Observables send <code>onError</code> notifications, mergeDelayError will
 * only invoke the <code>onError</code> method of its Observers once.
 * <p>
 * This operation allows an Observer to receive all successfully emitted items from all of the
 * source Observables without being interrupted by an error notification from one of them.
 * <p>
 * NOTE: If this is used on an Observable that never completes, it will never call
 * <code>onError</code> and will effectively swallow errors.
 */
public final class OperationMergeDelayError {

    /**
     * Flattens the observable sequences from the list of Observables into one observable sequence
     * without any transformation and delays any onError calls until after all sequences have called
     * onError or onComplete so as to allow all successful onNext calls to be received.
     * 
     * @param sequences
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of flattening the output from the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">Observable.Merge(TSource) Method (IObservable(TSource)[])</a>
     */
    public static <T> OnSubscribeFunc<T> mergeDelayError(final Observable<? extends Observable<? extends T>> sequences) {
        // wrap in a Func so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new MergeDelayErrorObservable<T>(sequences).onSubscribe(observer);
            }
        };
    }

    public static <T> OnSubscribeFunc<T> mergeDelayError(final Observable<? extends T>... sequences) {
        return mergeDelayError(Observable.create(new OnSubscribeFunc<Observable<? extends T>>() {
            private final BooleanSubscription s = new BooleanSubscription();

            @Override
            public Subscription onSubscribe(Observer<? super Observable<? extends T>> observer) {
                for (Observable<? extends T> o : sequences) {
                    if (!s.isUnsubscribed()) {
                        observer.onNext(o);
                    } else {
                        // break out of the loop if we are unsubscribed
                        break;
                    }
                }
                if (!s.isUnsubscribed()) {
                    observer.onCompleted();
                }
                return s;
            }
        }));
    }

    public static <T> OnSubscribeFunc<T> mergeDelayError(final List<? extends Observable<? extends T>> sequences) {
        return mergeDelayError(Observable.create(new OnSubscribeFunc<Observable<? extends T>>() {

            private final BooleanSubscription s = new BooleanSubscription();

            @Override
            public Subscription onSubscribe(Observer<? super Observable<? extends T>> observer) {
                for (Observable<? extends T> o : sequences) {
                    if (!s.isUnsubscribed()) {
                        observer.onNext(o);
                    } else {
                        // break out of the loop if we are unsubscribed
                        break;
                    }
                }
                if (!s.isUnsubscribed()) {
                    observer.onCompleted();
                }

                return s;
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
    private static final class MergeDelayErrorObservable<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends Observable<? extends T>> sequences;
        private final MergeSubscription ourSubscription = new MergeSubscription();
        private AtomicBoolean stopped = new AtomicBoolean(false);
        private volatile boolean parentCompleted = false;
        private final ConcurrentHashMap<ChildObserver, ChildObserver> childObservers = new ConcurrentHashMap<ChildObserver, ChildObserver>();
        private final ConcurrentHashMap<ChildObserver, Subscription> childSubscriptions = new ConcurrentHashMap<ChildObserver, Subscription>();
        // onErrors we received that will be delayed until everything is completed and then sent
        private ConcurrentLinkedQueue<Throwable> onErrorReceived = new ConcurrentLinkedQueue<Throwable>();

        private MergeDelayErrorObservable(Observable<? extends Observable<? extends T>> sequences) {
            this.sequences = sequences;
        }

        public Subscription onSubscribe(Observer<? super T> actualObserver) {
            CompositeSubscription completeSubscription = new CompositeSubscription();
            SerializedObserver<T> synchronizedObserver = new SerializedObserver<T>(actualObserver);

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

            @Override
            public boolean isUnsubscribed() {
                return stopped.get();
            }
        }

        /**
         * Subscribe to the top level Observable to receive the sequence of Observable<T> children.
         * 
         * @param <T>
         */
        private class ParentObserver implements Observer<Observable<? extends T>> {
            private final Observer<? super T> actualObserver;

            public ParentObserver(Observer<? super T> actualObserver) {
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
                            if (onErrorReceived.size() == 1) {
                                // an onError was received from 1 ChildObserver so we now send it as a delayed error
                                actualObserver.onError(onErrorReceived.peek());
                            } else if (onErrorReceived.size() > 1) {
                                // an onError was received from more than 1 ChildObserver so we now send it as a delayed error
                                actualObserver.onError(new CompositeException(onErrorReceived));
                            } else {
                                // no delayed error so send onCompleted
                                actualObserver.onCompleted();
                            }
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

            private final Observer<? super T> actualObserver;
            private volatile boolean finished = false;

            public ChildObserver(Observer<? super T> actualObserver) {
                this.actualObserver = actualObserver;
            }

            @Override
            public void onCompleted() {
                // remove self from map of Observers
                childObservers.remove(this);
                // if there are now 0 Observers left, so if the parent is also completed we send the onComplete to the actualObserver
                // if the parent is not complete that means there is another sequence (and child Observer) to come
                if (!stopped.get()) {
                    finishObserver();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!stopped.get()) {
                    onErrorReceived.add(e);
                    // mark this ChildObserver as done
                    childObservers.remove(this);
                    // but do NOT forward to actualObserver as we want other ChildObservers to continue until completion
                    // and we'll delay the sending of onError until all others are done

                    // we mark finished==true as a safety to ensure that if further calls to onNext occur we ignore them
                    finished = true;

                    // check for whether the parent is completed and if so then perform the 'finishing' actions
                    finishObserver();
                }
            }

            /**
             * onComplete and onError when called need to check for the parent being complete and if so send the onCompleted or onError to the actualObserver.
             * <p>
             * This does NOT get invoked if synchronous execution occurs, but will when asynchronously executing.
             * <p>
             * TestCase testErrorDelayed4WithThreading specifically tests this use case.
             */
            private void finishObserver() {
                if (childObservers.size() == 0 && parentCompleted) {
                    if (ourSubscription.stop()) {
                        // this thread 'won' the race to unsubscribe/stop so let's send onError or onCompleted
                        if (onErrorReceived.size() == 1) {
                            // an onError was received from 1 ChildObserver so we now send it as a delayed error
                            actualObserver.onError(onErrorReceived.peek());
                        } else if (onErrorReceived.size() > 1) {
                            // an onError was received from more than 1 ChildObserver so we now send it as a delayed error
                            actualObserver.onError(new CompositeException(onErrorReceived));
                        } else {
                            // no delayed error so send onCompleted
                            actualObserver.onCompleted();
                        }
                    }
                }
            }

            @Override
            public void onNext(T args) {
                // in case the Observable is poorly behaved and doesn't listen to the unsubscribe request
                // we'll ignore anything that comes in after we've unsubscribed or an onError has been received and delayed
                if (!stopped.get() && !finished) {
                    actualObserver.onNext(args);
                }
            }

        }
    }
}
