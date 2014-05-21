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
package rx.subscriptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import rx.Subscription;
import rx.exceptions.CompositeException;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed
 * together.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net equivalent CompositeDisposable</a>
 */
public final class CompositeSubscription implements Subscription {
    /** The atomic state updater. */
    static final AtomicReferenceFieldUpdater<CompositeSubscription, State> STATE_UPDATER
            = AtomicReferenceFieldUpdater.newUpdater(CompositeSubscription.class, State.class, "state");
    /** The subscription state. */
    volatile State state;

    /** Empty initial state. */
    private static final State CLEAR_STATE;
    /** Unsubscribed empty state. */
    private static final State CLEAR_STATE_UNSUBSCRIBED;
    static {
        Subscription[] s0 = new Subscription[0];
        CLEAR_STATE = new State(false, s0);
        CLEAR_STATE_UNSUBSCRIBED = new State(true, s0);
    }

    private static final class State {
        final boolean isUnsubscribed;
        final Subscription[] subscriptions;

        State(boolean u, Subscription[] s) {
            this.isUnsubscribed = u;
            this.subscriptions = s;
        }

        State unsubscribe() {
            return CLEAR_STATE_UNSUBSCRIBED;
        }

        State add(Subscription s) {
            int idx = subscriptions.length;
            Subscription[] newSubscriptions = new Subscription[idx + 1];
            System.arraycopy(subscriptions, 0, newSubscriptions, 0, idx);
            newSubscriptions[idx] = s;
            return new State(isUnsubscribed, newSubscriptions);
        }

        State remove(Subscription s) {
            if ((subscriptions.length == 1 && subscriptions[0].equals(s)) || subscriptions.length == 0) {
                return clear();
            }
            Subscription[] newSubscriptions = new Subscription[subscriptions.length - 1];
            int idx = 0;
            for (Subscription _s : subscriptions) {
                if (!_s.equals(s)) {
                    // was not in this composite
                    if (idx == newSubscriptions.length) {
                        return this;
                    }
                    newSubscriptions[idx] = _s;
                    idx++;
                }
            }
            if (idx == 0) {
                return clear();
            }
            // subscription appeared more than once
            if (idx < newSubscriptions.length) {
                Subscription[] newSub2 = new Subscription[idx];
                System.arraycopy(newSubscriptions, 0, newSub2, 0, idx);
                return new State(isUnsubscribed, newSub2);
            }
            return new State(isUnsubscribed, newSubscriptions);
        }

        State clear() {
            return isUnsubscribed ? CLEAR_STATE_UNSUBSCRIBED : CLEAR_STATE;
        }
    }

    public CompositeSubscription() {
        // this creates only a store-store barrier which is generally faster when
        // CompositeSubscriptions are created in a tight loop.
        STATE_UPDATER.lazySet(this, CLEAR_STATE);
    }

    public CompositeSubscription(final Subscription... subscriptions) {
        STATE_UPDATER.lazySet(this, new State(false, subscriptions));
    }

    @Override
    public boolean isUnsubscribed() {
        return state.isUnsubscribed;
    }

    public void add(final Subscription s) {
        State oldState;
        State newState;
        do {
            oldState = state;
            if (oldState.isUnsubscribed) {
                s.unsubscribe();
                return;
            } else {
                newState = oldState.add(s);
            }
        } while (!STATE_UPDATER.compareAndSet(this, oldState, newState));
    }

    public void remove(final Subscription s) {
        State oldState;
        State newState;
        do {
            oldState = state;
            if (oldState.isUnsubscribed) {
                return;
            } else {
                newState = oldState.remove(s);
            }
        } while (!STATE_UPDATER.compareAndSet(this, oldState, newState));
        // if we removed successfully we then need to call unsubscribe on it
        s.unsubscribe();
    }

    public void clear() {
        State oldState;
        State newState;
        do {
            oldState = state;
            if (oldState.isUnsubscribed) {
                return;
            } else {
                newState = oldState.clear();
            }
        } while (!STATE_UPDATER.compareAndSet(this, oldState, newState));
        // if we cleared successfully we then need to call unsubscribe on all previous
        unsubscribeFromAll(oldState.subscriptions);
    }

    @Override
    public void unsubscribe() {
        State oldState = state;
        if (oldState.isUnsubscribed) {
            return;
        }
        // intrinsics may make this a single instruction and may prevent concurrent add/remove faster
        oldState = STATE_UPDATER.getAndSet(this, oldState.unsubscribe());
        unsubscribeFromAll(oldState.subscriptions);
    }

    private static void unsubscribeFromAll(Subscription[] subscriptions) {
        final List<Throwable> es = new ArrayList<Throwable>();
        for (Subscription s : subscriptions) {
            try {
                s.unsubscribe();
            } catch (Throwable e) {
                es.add(e);
            }
        }
        if (!es.isEmpty()) {
            if (es.size() == 1) {
                Throwable t = es.get(0);
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else {
                    throw new CompositeException(
                            "Failed to unsubscribe to 1 or more subscriptions.", es);
                }
            } else {
                throw new CompositeException(
                        "Failed to unsubscribe to 2 or more subscriptions.", es);
            }
        }
    }
}
