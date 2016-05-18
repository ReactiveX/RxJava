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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Subscription;

/**
 * Keeps track of the sub-subscriptions and unsubscribes the underlying subscription once all sub-subscriptions
 * have unsubscribed.
 */
public final class RefCountSubscription implements Subscription {
    private final Subscription actual;
    static final State EMPTY_STATE = new State(false, 0);
    final AtomicReference<State> state = new AtomicReference<State>(EMPTY_STATE);

    private static final class State {
        final boolean isUnsubscribed;
        final int children;

        State(boolean u, int c) {
            this.isUnsubscribed = u;
            this.children = c;
        }

        State addChild() {
            return new State(isUnsubscribed, children + 1);
        }

        State removeChild() {
            return new State(isUnsubscribed, children - 1);
        }

        State unsubscribe() {
            return new State(true, children);
        }

    }

    /**
     * Creates a {@code RefCountSubscription} by wrapping the given non-null {@code Subscription}.
     * 
     * @param s
     *          the {@link Subscription} to wrap
     * @throws IllegalArgumentException
     *          if {@code s} is {@code null}
     */
    public RefCountSubscription(Subscription s) {
        if (s == null) {
            throw new IllegalArgumentException("s");
        }
        this.actual = s;
    }

    /**
     * Returns a new sub-subscription
     *
     * @return a new sub-subscription.
     */
    public Subscription get() {
        State oldState;
        State newState;
        final AtomicReference<State> localState = this.state;
        do {
            oldState = localState.get();
            if (oldState.isUnsubscribed) {
                return Subscriptions.unsubscribed();
            } else {
                newState = oldState.addChild();
            }
        } while (!localState.compareAndSet(oldState, newState));

        return new InnerSubscription(this);
    }

    @Override
    public boolean isUnsubscribed() {
        return state.get().isUnsubscribed;
    }

    @Override
    public void unsubscribe() {
        State oldState;
        State newState;
        final AtomicReference<State> localState = this.state;
        do {
            oldState = localState.get();
            if (oldState.isUnsubscribed) {
                return;
            }
            newState = oldState.unsubscribe();
        } while (!localState.compareAndSet(oldState, newState));
        unsubscribeActualIfApplicable(newState);
    }

    private void unsubscribeActualIfApplicable(State state) {
        if (state.isUnsubscribed && state.children == 0) {
            actual.unsubscribe();
        }
    }
    void unsubscribeAChild() {
        State oldState;
        State newState;
        final AtomicReference<State> localState = this.state;
        do {
            oldState = localState.get();
            newState = oldState.removeChild();
        } while (!localState.compareAndSet(oldState, newState));
        unsubscribeActualIfApplicable(newState);
    }

    /** The individual sub-subscriptions. */
    private static final class InnerSubscription extends AtomicInteger implements Subscription {
        /** */
        private static final long serialVersionUID = 7005765588239987643L;
        
        final RefCountSubscription parent;
        public InnerSubscription(RefCountSubscription parent) {
            this.parent = parent;
        }
        @Override
        public void unsubscribe() {
            if (compareAndSet(0, 1)) {
                parent.unsubscribeAChild();
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return get() != 0;
        }
    }
}
