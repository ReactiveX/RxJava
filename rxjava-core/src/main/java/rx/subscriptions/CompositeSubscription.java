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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Subscription;
import rx.exceptions.CompositeException;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed
 * together.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net equivalent CompositeDisposable</a>
 */
public final class CompositeSubscription implements Subscription {
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe)f.get(null);
            stateOffset = UNSAFE.objectFieldOffset(CompositeSubscription.class.getDeclaredField("state"));
        } catch (Throwable e) {
            throw new Error(e);
        }
    }
    private volatile State state;

    /** Empty initial state. */
    private static final State CLEAR_STATE;
    /** Unsubscribed empty state. */
    private static final State CLEAR_STATE_UNSUBSCRIBED;
    /** Set mode threshold count. */
    private static final int SET_MODE_THRESHOLD = 16;
    /** Array mode threshold count. */
    private static final int ARRAY_MODE_THRESHOLD = SET_MODE_THRESHOLD * 3 / 4;
    static {
        Object[] s0 = new Object[0];
        CLEAR_STATE = new State(false, s0);
        CLEAR_STATE_UNSUBSCRIBED = new State(true, s0);
    }

    private static final class State {
        final boolean isUnsubscribed;
        final Object[] subscriptions;
        final Set<Object> subscriptionSet;

        State(boolean u, Object[] s) {
            this.isUnsubscribed = u;
            this.subscriptions = s;
            this.subscriptionSet = null;
        }
        State(Object[] s, Subscription add) {
            this.isUnsubscribed = false;
            this.subscriptions = null;
            this.subscriptionSet = new HashSet<Object>(s.length * 6 / 5);
            for (Object o : s) {
                this.subscriptionSet.add(o);
            }
            this.subscriptionSet.add(add);
        }
        State(Set<Object> s) {
            this.isUnsubscribed = false;
            this.subscriptions = null;
            this.subscriptionSet = s;
        }

        State unsubscribe() {
            return CLEAR_STATE_UNSUBSCRIBED;
        }

        State add(Subscription s) {
            if (subscriptions == null) {
                synchronized (subscriptionSet) {
                    subscriptionSet.add(s);
                    return this;
                }
            }
            int idx = subscriptions.length;
            if (idx == SET_MODE_THRESHOLD) {
                return new State(subscriptions, s);
            }
            Object[] newSubscriptions = new Object[idx + 1];
            System.arraycopy(subscriptions, 0, newSubscriptions, 0, idx);
            newSubscriptions[idx] = s;
            return new State(isUnsubscribed, newSubscriptions);
        }

        State remove(Subscription s) {
            if (subscriptions == null) {
                synchronized (subscriptionSet) {
                    subscriptionSet.remove(s);
                    if (subscriptionSet.size() == ARRAY_MODE_THRESHOLD) {
                        return new State(isUnsubscribed, subscriptionSet.toArray(new Object[subscriptionSet.size()]));
                    }
                    return this;
                }
            }
            if ((subscriptions.length == 1 && subscriptions[0].equals(s)) || subscriptions.length == 0) {
                return clear();
            }
            Object[] newSubscriptions = new Object[subscriptions.length - 1];
            int idx = 0;
            for (Object _s : subscriptions) {
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
                Object[] newSub2 = new Object[idx];
                System.arraycopy(newSubscriptions, 0, newSub2, 0, idx);
                return new State(isUnsubscribed, newSub2);
            }
            return new State(isUnsubscribed, newSubscriptions);
        }

        State clear() {
            return isUnsubscribed ? CLEAR_STATE_UNSUBSCRIBED : CLEAR_STATE;
        }
        void unsubscribeAll() {
            if (subscriptions == null) {
                unsubscribeFromAll(subscriptionSet);
            } else {
                unsubscribeFromAll(subscriptions);
            }
        }
        /* test support.*/ int size() {
            if (subscriptions == null) {
                synchronized (subscriptionSet) {
                    return subscriptionSet.size();
                }
            }
            return subscriptions.length;
        }
    }

    public CompositeSubscription() {
        setState(CLEAR_STATE);
    }

    public CompositeSubscription(final Subscription... subscriptions) {
        if (subscriptions.length > 0) {
            setState(new State(false, subscriptions));
        } else {
            setState(CLEAR_STATE);
        }
    }
    
    @Override
    public boolean isUnsubscribed() {
        return getState().isUnsubscribed;
    }

    public void add(final Subscription s) {
        State oldState;
        State newState;
        do {
            oldState = getState();
            if (oldState.isUnsubscribed) {
                s.unsubscribe();
                return;
            } else {
                newState = oldState.add(s);
            }
        } while (!casState(oldState, newState));
    }

    public void remove(final Subscription s) {
        State oldState;
        State newState;
        do {
            oldState = getState();
            if (oldState.isUnsubscribed) {
                return;
            } else {
                newState = oldState.remove(s);
            }
        } while (!casState(oldState, newState));
        // if we removed successfully we then need to call unsubscribe on it
        s.unsubscribe();
    }

    public void clear() {
        State oldState;
        State newState;
        do {
            oldState = getState();
            if (oldState.isUnsubscribed) {
                return;
            } else {
                newState = oldState.clear();
            }
        } while (!casState(oldState, newState));
        // if we cleared successfully we then need to call unsubscribe on all previous
        oldState.unsubscribeAll();
    }

    @Override
    public void unsubscribe() {
        State oldState;
        State newState;
        do {
            oldState = getState();
            if (oldState.isUnsubscribed) {
                return;
            } else {
                newState = oldState.unsubscribe();
            }
        } while (!casState(oldState, newState));
        oldState.unsubscribeAll();
    }

    static void unsubscribeFromAll(Object[] subscriptions) {
        unsubscribeFromAll(Arrays.asList(subscriptions));
    }
    static void unsubscribeFromAll(Iterable<Object> subscriptions) {
        final List<Throwable> es = new ArrayList<Throwable>();
        for (Object s : subscriptions) {
            if (s != null) {
                try {
                    ((Subscription)s).unsubscribe();
                } catch (Throwable e) {
                    es.add(e);
                }
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
    /* Test support. */ int size() {
        return getState().size();
    }
    // ------- Atomics ----------
    private void setState(State newState) {
        state = newState;
    }
    private State getState() {
        return state;
    }
    private boolean casState(State expected, State newState) {
        return UNSAFE.compareAndSwapObject(this, stateOffset, expected, newState);
    }
}