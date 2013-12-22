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
package rx.subjects;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;

/* package */class SubjectSubscriptionManager<T> {

    private AtomicReference<State<T>> state = new AtomicReference<State<T>>(new State<T>());

    /**
     * 
     * @param onSubscribe
     *            Always runs at the beginning of 'subscribe' regardless of terminal state.
     * @param onTerminated
     *            Only runs if Subject is in terminal state and the Observer ends up not being registered.
     * @return
     */
    public OnSubscribeFunc<T> getOnSubscribeFunc(final Action1<Observer<? super T>> onSubscribe, final Action1<Observer<? super T>> onTerminated) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                // invoke onSubscribe logic 
                if (onSubscribe != null) {
                    onSubscribe.call(observer);
                }

                State<T> current = null;
                State<T> newState = null;
                boolean addedObserver = false;
                Subscription s;
                do {
                    current = state.get();
                    if (current.terminated) {
                        // we are terminated so don't need to do anything
                        s = Subscriptions.empty();
                        addedObserver = false;
                        // break out and don't try to modify state
                        newState = current;
                        // wait for termination to complete if 
                        try {
                            current.terminationLatch.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException("Interrupted waiting for termination.", e);
                        }
                        break;
                    } else {
                        final SafeObservableSubscription subscription = new SafeObservableSubscription();
                        s = subscription;
                        addedObserver = true;
                        subscription.wrap(new Subscription() {
                            @Override
                            public void unsubscribe() {
                                State<T> current;
                                State<T> newState;
                                do {
                                    current = state.get();
                                    // on unsubscribe remove it from the map of outbound observers to notify
                                    newState = current.removeObserver(subscription);
                                } while (!state.compareAndSet(current, newState));
                            }
                        });

                        // on subscribe add it to the map of outbound observers to notify
                        newState = current.addObserver(subscription, observer);
                    }
                } while (!state.compareAndSet(current, newState));

                /**
                 * Whatever happened above, if we are terminated we run `onTerminated`
                 */
                if (newState.terminated && !addedObserver) {
                    onTerminated.call(observer);
                }

                return s;
            }

        };
    }

    protected void terminate(Action1<Collection<Observer<? super T>>> onTerminate) {
        State<T> current = null;
        State<T> newState = null;
        do {
            current = state.get();
            if (current.terminated) {
                // already terminated so do nothing
                return;
            } else {
                newState = current.terminate();
            }
        } while (!state.compareAndSet(current, newState));

        /*
         * if we get here then we won setting the state to terminated
         * and have a deterministic set of Observers to emit to (concurrent subscribes
         * will have failed and will try again and see we are term
         * inated)
         */
        try {
            onTerminate.call(newState.observers.values());
        } finally {
            // mark that termination is completed
            newState.terminationLatch.countDown();
        }
    }

    /**
     * Current snapshot of 'state.observers.keySet()' so that concurrent modifications aren't included.
     * 
     * This makes it behave deterministically in a single-threaded execution when nesting subscribes.
     * 
     * In multi-threaded execution it will cause new subscriptions to wait until the following onNext instead
     * of possibly being included in the current onNext iteration.
     * 
     * @return List<Observer<T>>
     */
    public Collection<Observer<? super T>> snapshotOfObservers() {
        // we don't need to copy since state is immutable
        return state.get().observers.values();
    }

    protected static class State<T> {
        final boolean terminated;
        final CountDownLatch terminationLatch;
        final Map<Subscription, Observer<? super T>> observers;

        private State(boolean isTerminated, CountDownLatch terminationLatch, Map<Subscription, Observer<? super T>> observers) {
            this.terminationLatch = terminationLatch;
            this.terminated = isTerminated;
            this.observers = Collections.unmodifiableMap(observers);
        }

        State() {
            this.terminated = false;
            this.terminationLatch = null;
            this.observers = Collections.emptyMap();
        }

        public State<T> terminate() {
            if (terminated) {
                throw new IllegalStateException("Already terminated.");
            }
            return new State<T>(true, new CountDownLatch(1), observers);
        }

        public State<T> addObserver(Subscription s, Observer<? super T> observer) {
            Map<Subscription, Observer<? super T>> newMap = new HashMap<Subscription, Observer<? super T>>();
            newMap.putAll(observers);
            newMap.put(s, observer);
            return new State<T>(terminated, terminationLatch, newMap);
        }

        public State<T> removeObserver(Subscription s) {
            Map<Subscription, Observer<? super T>> newMap = new HashMap<Subscription, Observer<? super T>>(observers);
            newMap.remove(s);
            return new State<T>(terminated, terminationLatch, newMap);
        }
    }

}
