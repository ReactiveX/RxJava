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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observables.GroupedObservable;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

public class OperatorPivot<K1, K2, T> implements Operator<GroupedObservable<K2, GroupedObservable<K1, T>>, GroupedObservable<K1, GroupedObservable<K2, T>>> {

    @Override
    public Subscriber<? super GroupedObservable<K1, GroupedObservable<K2, T>>> call(final Subscriber<? super GroupedObservable<K2, GroupedObservable<K1, T>>> child) {
        final AtomicReference<State> state = new AtomicReference<State>(State.create());
        final OperatorPivot<K1, K2, T>.PivotSubscriber pivotSubscriber = new PivotSubscriber(new CompositeSubscription(), child, state);
        child.add(Subscriptions.create(new Action0() {

            @Override
            public void call() {
                State current;
                State newState = null;
                do {
                    current = state.get();
                    newState = current.unsubscribe();
                } while (!state.compareAndSet(current, newState));

                // If all outer/inner groups are completed/unsubscribed then we can shut it all down
                if (newState.shouldComplete()) {
                    pivotSubscriber.groups.completeAll(newState);
                }
                // otherwise it is just marked as unsubscribed and groups being completed/unsubscribed will allow it to cleanup
            }

        }));

        return pivotSubscriber;
    }

    private final class PivotSubscriber extends Subscriber<GroupedObservable<K1, GroupedObservable<K2, T>>> {
        /*
         * needs to decouple the subscription as the inner subscriptions need a separate lifecycle
         * and will unsubscribe on this parent if they are all unsubscribed
         */
        private final CompositeSubscription parentSubscription;
        private final Subscriber<? super GroupedObservable<K2, GroupedObservable<K1, T>>> child;
        private final AtomicReference<State> state;
        private final GroupState<K1, K2, T> groups;

        private PivotSubscriber(CompositeSubscription parentSubscription, Subscriber<? super GroupedObservable<K2, GroupedObservable<K1, T>>> child, AtomicReference<State> state) {
            super(parentSubscription);
            this.parentSubscription = parentSubscription;
            this.child = child;
            this.state = state;
            this.groups = new GroupState<K1, K2, T>(parentSubscription, child);
        }

        @Override
        public void onCompleted() {
            State current;
            State newState = null;
            do {
                current = state.get();
                newState = current.complete();
            } while (!state.compareAndSet(current, newState));

            // special case for empty (no groups emitted) or all groups already done
            if (newState.shouldComplete()) {
                groups.completeAll(newState);
            }
        }

        @Override
        public void onError(Throwable e) {
            // we immediately tear everything down if we receive an error
            child.onError(e);
        }

        @Override
        public void onNext(final GroupedObservable<K1, GroupedObservable<K2, T>> k1Group) {
            groups.startK1Group(state, k1Group.getKey());
            k1Group.unsafeSubscribe(new Subscriber<GroupedObservable<K2, T>>(parentSubscription) {

                @Override
                public void onCompleted() {
                    groups.completeK1Group(state, k1Group.getKey());
                }

                @Override
                public void onError(Throwable e) {
                    child.onError(e);
                }

                @Override
                public void onNext(final GroupedObservable<K2, T> k2Group) {
                    /*
                     * In this scope once pivoted, k2 == outer and k2.k1 == inner
                     */
                    final Inner<K1, K2, T> inner = groups.getOrCreateFor(state, child, k1Group.getKey(), k2Group.getKey());
                    if (inner == null) {
                        // we have been unsubscribed
                        return;
                    }
                    k2Group.unsafeSubscribe(new Subscriber<T>(parentSubscription) {

                        @Override
                        public void onCompleted() {
                            /*
                             * we don't actually propagate onCompleted to the 'inner.subscriber' here
                             * since multiple upstream groups will be sent to a single downstream
                             * and a single upstream group completing does not indicate total completion
                             */
                        }

                        @Override
                        public void onError(Throwable e) {
                            inner.subscriber.onError(e);
                        }

                        @Override
                        public void onNext(T t) {
                            inner.subscriber.onNext(t);
                        }

                    });

                }

            });

        }

    }

    private static class GroupState<K1, K2, T> {
        private final ConcurrentHashMap<KeyPair<K1, K2>, Inner<K1, K2, T>> innerSubjects = new ConcurrentHashMap<KeyPair<K1, K2>, Inner<K1, K2, T>>();
        private final ConcurrentHashMap<K2, Outer<K1, K2, T>> outerSubjects = new ConcurrentHashMap<K2, Outer<K1, K2, T>>();
        private final AtomicBoolean completeEmitted = new AtomicBoolean();
        private final CompositeSubscription parentSubscription;
        private final Subscriber<? super GroupedObservable<K2, GroupedObservable<K1, T>>> child;

        public GroupState(CompositeSubscription parentSubscription, Subscriber<? super GroupedObservable<K2, GroupedObservable<K1, T>>> child) {
            this.parentSubscription = parentSubscription;
            this.child = child;
        }

        public void startK1Group(AtomicReference<State> state, K1 key) {
            State current;
            State newState = null;
            do {
                current = state.get();
                newState = current.addK1(key);
            } while (!state.compareAndSet(current, newState));
        }

        public void completeK1Group(AtomicReference<State> state, K1 key) {
            State current;
            State newState = null;
            do {
                current = state.get();
                newState = current.removeK1(key);
            } while (!state.compareAndSet(current, newState));

            if (newState.shouldComplete()) {
                completeAll(newState);
            }
        }

        public void startK1K2Group(AtomicReference<State> state, KeyPair<K1, K2> keyPair) {
            State current;
            State newState = null;
            do {
                current = state.get();
                newState = current.addK1k2(keyPair);
            } while (!state.compareAndSet(current, newState));
        }

        public void completeK1K2Group(AtomicReference<State> state, KeyPair<K1, K2> keyPair) {
            State current;
            State newState = null;
            do {
                current = state.get();
                newState = current.removeK1k2(keyPair);
            } while (!state.compareAndSet(current, newState));

            if (newState.shouldComplete()) {
                completeAll(newState);
            }
        }

        public void completeAll(State state) {
            if (completeEmitted.compareAndSet(false, true)) {
                /*
                 * after we are completely done emitting we can now shut down the groups
                 */
                for (Entry<K2, Outer<K1, K2, T>> outer : outerSubjects.entrySet()) {
                    outer.getValue().subscriber.onCompleted();
                }
                for (Entry<KeyPair<K1, K2>, Inner<K1, K2, T>> inner : innerSubjects.entrySet()) {
                    inner.getValue().subscriber.onCompleted();
                }
                // unsubscribe eagerly
                if (state.unsubscribed) {
                    parentSubscription.unsubscribe(); // unsubscribe from parent
                }
                child.onCompleted();
            }
        }

        private Inner<K1, K2, T> getOrCreateFor(final AtomicReference<State> state, final Subscriber<? super GroupedObservable<K2, GroupedObservable<K1, T>>> child, K1 key1, K2 key2) {
            Outer<K1, K2, T> outer = getOrCreateOuter(state, child, key2);
            if (outer == null) {
                // we have been unsubscribed
                return null;
            }

            Inner<K1, K2, T> orCreateInnerSubject = getOrCreateInnerSubject(state, outer, key1, key2);
            return orCreateInnerSubject;
        }

        private Inner<K1, K2, T> getOrCreateInnerSubject(final AtomicReference<State> state, final Outer<K1, K2, T> outer, final K1 key1, final K2 key2) {
            KeyPair<K1, K2> keyPair = new KeyPair<K1, K2>(key1, key2);
            Inner<K1, K2, T> innerSubject = innerSubjects.get(keyPair);
            if (innerSubject != null) {
                return innerSubject;
            } else {
                Inner<K1, K2, T> newInner = Inner.create(this, state, outer, keyPair);
                Inner<K1, K2, T> existing = innerSubjects.putIfAbsent(keyPair, newInner);
                if (existing != null) {
                    // we lost the race to create so return the one that did
                    return existing;
                } else {
                    startK1K2Group(state, keyPair);
                    outer.subscriber.onNext(newInner.group);
                    return newInner;
                }
            }
        }

        private Outer<K1, K2, T> getOrCreateOuter(final AtomicReference<State> state, final Subscriber<? super GroupedObservable<K2, GroupedObservable<K1, T>>> child, final K2 key2) {
            Outer<K1, K2, T> outerSubject = outerSubjects.get(key2);
            if (outerSubject != null) {
                return outerSubject;
            } else {
                // this group doesn't exist
                if (child.isUnsubscribed()) {
                    // we have been unsubscribed on the outer so won't send any  more groups 
                    return null;
                }

                Outer<K1, K2, T> newOuter = Outer.<K1, K2, T> create(this, state, key2);
                Outer<K1, K2, T> existing = outerSubjects.putIfAbsent(key2, newOuter);
                if (existing != null) {
                    // we lost the race to create so return the one that did
                    return existing;
                } else {
                    child.onNext(newOuter.group);
                    return newOuter;
                }
            }
        }
    }

    private static class Inner<K1, K2, T> {

        private final BufferUntilSubscriber<T> subscriber;
        private final GroupedObservable<K1, T> group;

        private Inner(BufferUntilSubscriber<T> subscriber, GroupedObservable<K1, T> group) {
            this.subscriber = subscriber;
            this.group = group;
        }

        public static <K1, K2, T> Inner<K1, K2, T> create(final GroupState<K1, K2, T> groupState, final AtomicReference<State> state, final Outer<K1, K2, T> outer, final KeyPair<K1, K2> keyPair) {
            final BufferUntilSubscriber<T> subject = BufferUntilSubscriber.create();
            GroupedObservable<K1, T> group = new GroupedObservable<K1, T>(keyPair.k1, new OnSubscribe<T>() {

                @Override
                public void call(final Subscriber<? super T> o) {
                    o.add(Subscriptions.create(new Action0() {

                        @Override
                        public void call() {
                            groupState.completeK1K2Group(state, keyPair);
                        }

                    }));
                    subject.unsafeSubscribe(new Subscriber<T>(o) {

                        @Override
                        public void onCompleted() {
                            groupState.completeK1K2Group(state, keyPair);
                            o.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            o.onError(e);
                        }

                        @Override
                        public void onNext(T t) {
                            if (!isUnsubscribed()) {
                                o.onNext(t);
                            }
                        }

                    });
                }

            });
            return new Inner<K1, K2, T>(subject, group);
        }
    }

    private static class Outer<K1, K2, T> {

        private final BufferUntilSubscriber<GroupedObservable<K1, T>> subscriber;
        private final GroupedObservable<K2, GroupedObservable<K1, T>> group;

        private Outer(BufferUntilSubscriber<GroupedObservable<K1, T>> subscriber, GroupedObservable<K2, GroupedObservable<K1, T>> group) {
            this.subscriber = subscriber;
            this.group = group;
        }

        public static <K1, K2, T> Outer<K1, K2, T> create(final GroupState<K1, K2, T> groups, final AtomicReference<State> state, final K2 key2) {
            final BufferUntilSubscriber<GroupedObservable<K1, T>> subject = BufferUntilSubscriber.create();
            GroupedObservable<K2, GroupedObservable<K1, T>> group = new GroupedObservable<K2, GroupedObservable<K1, T>>(key2, new OnSubscribe<GroupedObservable<K1, T>>() {

                @Override
                public void call(final Subscriber<? super GroupedObservable<K1, T>> o) {
                    subject.unsafeSubscribe(new Subscriber<GroupedObservable<K1, T>>(o) {

                        @Override
                        public void onCompleted() {
                            o.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            o.onError(e);
                        }

                        @Override
                        public void onNext(GroupedObservable<K1, T> t) {
                            if (!isUnsubscribed()) {
                                o.onNext(t);
                            }
                        }

                    });
                }

            });

            return new Outer<K1, K2, T>(subject, group);
        }
    }

    private static class State {
        private final boolean unsubscribed;
        private final boolean completed;
        private final Set<Object> k1Keys;
        private final Set<KeyPair<?, ?>> k1k2Keys;

        private State(boolean completed, boolean unsubscribed, Set<Object> k1Keys, Set<KeyPair<?, ?>> k1k2Keys) {
            this.completed = completed;
            this.unsubscribed = unsubscribed;
            this.k1Keys = k1Keys;
            this.k1k2Keys = k1k2Keys;
        }

        public static State create() {
            return new State(false, false, Collections.emptySet(), Collections.<KeyPair<?, ?>> emptySet());
        }

        public State addK1(Object key) {
            Set<Object> newKeys = new HashSet<Object>(k1Keys);
            newKeys.add(key);
            return new State(completed, unsubscribed, newKeys, k1k2Keys);
        }

        public State removeK1(Object key) {
            Set<Object> newKeys = new HashSet<Object>(k1Keys);
            newKeys.remove(key);
            return new State(completed, unsubscribed, newKeys, k1k2Keys);
        }

        public State addK1k2(KeyPair<?, ?> key) {
            Set<KeyPair<?, ?>> newKeys = new HashSet<KeyPair<?, ?>>(k1k2Keys);
            newKeys.add(key);
            return new State(completed, unsubscribed, k1Keys, newKeys);
        }

        public State removeK1k2(KeyPair<?, ?> key) {
            Set<KeyPair<?, ?>> newKeys = new HashSet<KeyPair<?, ?>>(k1k2Keys);
            newKeys.remove(key);
            return new State(completed, unsubscribed, k1Keys, newKeys);
        }

        public State complete() {
            return new State(true, unsubscribed, k1Keys, k1k2Keys);
        }

        public State unsubscribe() {
            return new State(completed, true, k1Keys, k1k2Keys);
        }

        public boolean shouldComplete() {
            if (k1Keys.size() == 0 && completed) {
                return true;
            } else if (unsubscribed) {
                // if unsubscribed and all groups are completed/unsubscribed we can complete
                return k1k2Keys.size() == 0;
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "State =>  k1: " + k1Keys.size() + " k1k2: " + k1k2Keys.size() + " completed: " + completed + " unsubscribed: " + unsubscribed;
        }
    }

    private static class KeyPair<K1, K2> {
        private final K1 k1;
        private final K2 k2;

        KeyPair(K1 k1, K2 k2) {
            this.k1 = k1;
            this.k2 = k2;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((k1 == null) ? 0 : k1.hashCode());
            result = prime * result + ((k2 == null) ? 0 : k2.hashCode());
            return result;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            KeyPair other = (KeyPair) obj;
            if (k1 == null) {
                if (other.k1 != null)
                    return false;
            } else if (!k1.equals(other.k1))
                return false;
            if (k2 == null) {
                if (other.k2 != null)
                    return false;
            } else if (!k2.equals(other.k2))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return k2 + "." + k1;
        }

    }

}
