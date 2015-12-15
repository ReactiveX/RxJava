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

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscription;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to exit the loop
 * if unsubscribed.
 */
public final class MultipleAssignmentSubscription implements Subscription {

    final AtomicReference<State> state = new AtomicReference<State>(new State(false, Subscriptions.empty()));
    
    private static final class State {
        final boolean isUnsubscribed;
        final Subscription subscription;

        State(boolean u, Subscription s) {
            this.isUnsubscribed = u;
            this.subscription = s;
        }

        State unsubscribe() {
            return new State(true, subscription);
        }

        State set(Subscription s) {
            return new State(isUnsubscribed, s);
        }

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
            } else {
                newState = oldState.unsubscribe();
            }
        } while (!localState.compareAndSet(oldState, newState));
        oldState.subscription.unsubscribe();
    }

    /**
     * Sets the underlying subscription. If the {@code MultipleAssignmentSubscription} is already unsubscribed,
     * setting a new subscription causes the new subscription to also be immediately unsubscribed.
     *
     * @param s the {@link Subscription} to set
     * @throws IllegalArgumentException if {@code s} is {@code null}
     */
    public void set(Subscription s) {
        if (s == null) {
            throw new IllegalArgumentException("Subscription can not be null");
        }
        State oldState;
        State newState;
        final AtomicReference<State> localState = this.state;
        do {
            oldState = localState.get();
            if (oldState.isUnsubscribed) {
                s.unsubscribe();
                return;
            } else {
                newState = oldState.set(s);
            }
        } while (!localState.compareAndSet(oldState, newState));
    }

    /**
     * Gets the underlying subscription.
     *
     * @return the {@link Subscription} that underlies the {@code MultipleAssignmentSubscription}
     */
    public Subscription get() {
        return state.get().subscription;
    }

}
