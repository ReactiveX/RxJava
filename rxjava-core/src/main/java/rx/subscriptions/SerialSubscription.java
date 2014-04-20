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

import rx.Subscription;

/**
 * Represents a subscription whose underlying subscription can be swapped for another subscription
 * which causes the previous underlying subscription to be unsubscribed.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.serialdisposable(v=vs.103).aspx">Rx.Net equivalent SerialDisposable</a>
 */
public final class SerialSubscription implements Subscription {

    private final AtomicReference<State> state = new AtomicReference<State>(new State(false, Subscriptions.empty()));

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

    public boolean isUnsubscribed() {
        return state.get().isUnsubscribed;
    }

    @Override
    public void unsubscribe() {
        State oldState;
        State newState;
        do {
            oldState = state.get();
            if (oldState.isUnsubscribed) {
                return;
            } else {
                newState = oldState.unsubscribe();
            }
        } while (!state.compareAndSet(oldState, newState));
        oldState.subscription.unsubscribe();
    }

    public void set(Subscription s) {
        if (s == null) {
            throw new IllegalArgumentException("Subscription can not be null");
        }
        State oldState;
        State newState;
        do {
            oldState = state.get();
            if (oldState.isUnsubscribed) {
                s.unsubscribe();
                return;
            } else {
                newState = oldState.set(s);
            }
        } while (!state.compareAndSet(oldState, newState));
        oldState.subscription.unsubscribe();
    }

    public Subscription get() {
        return state.get().subscription;
    }

}
