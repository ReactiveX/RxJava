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
package rx.subscriptions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Subscription;

/**
 * Keeps track of the sub-subscriptions and unsubscribes the underlying
 * subscription once all sub-subscriptions have unsubscribed.
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/system.reactive.disposables.refcountdisposable.aspx'>MSDN RefCountDisposable</a>
 */
public class RefCountSubscription implements Subscription {
    /** The reference to the actual subscription. */
    private final Subscription actual;
    /** Counts the number of subscriptions (1 parent + multiple children) */
    private final AtomicReference<State> state = new AtomicReference<State>(new State(false, 0));

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
     * Create a RefCountSubscription by wrapping the given non-null Subscription.
     * 
     * @param s
     */
    public RefCountSubscription(Subscription s) {
        if (s == null) {
            throw new IllegalArgumentException("s");
        }
        this.actual = s;
    }

    /**
     * Returns a new sub-subscription.
     */
    public Subscription getSubscription() {
        State current;
        State newState;
        do {
            current = state.get();
            if (current.isUnsubscribed) {
                return Subscriptions.empty();
            } else {
                newState = current.addChild();
            }
        } while (!state.compareAndSet(current, newState));

        return new InnerSubscription();
    }

    /**
     * Check if this subscription is already unsubscribed.
     */
    public boolean isUnsubscribed() {
        return state.get().isUnsubscribed;
    }

    @Override
    public void unsubscribe() {
        State current;
        State newState;
        do {
            current = state.get();
            if (current.isUnsubscribed) {
                return;
            }
            newState = current.unsubscribe();
        } while (!state.compareAndSet(current, newState));
        unsubscribeActualIfApplicable(newState);
    }

    private void unsubscribeActualIfApplicable(State state) {
        if (state.isUnsubscribed && state.children == 0) {
            actual.unsubscribe();
        }
    }

    /** The individual sub-subscriptions. */
    class InnerSubscription implements Subscription {
        final AtomicBoolean innerDone = new AtomicBoolean();

        @Override
        public void unsubscribe() {
            if (innerDone.compareAndSet(false, true)) {
                State current;
                State newState;
                do {
                    current = state.get();
                    newState = current.removeChild();
                } while (!state.compareAndSet(current, newState));
                unsubscribeActualIfApplicable(newState);
            }
        }
    };
}