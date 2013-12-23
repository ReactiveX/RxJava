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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import rx.Subscription;

/**
 * Keeps track of the sub-subscriptions and unsubscribes the underlying
 * subscription once all sub-subscriptions have unsubscribed.
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/system.reactive.disposables.refcountdisposable.aspx'>MSDN RefCountDisposable</a>
 */
public class RefCountSubscription implements Subscription {
    /** The state for the atomic operations. */
    private enum State {
        ACTIVE,
        MUTATING,
        UNSUBSCRIBED
    }

    /** The reference to the actual subscription. */
    private volatile Subscription main;
    /** The current state. */
    private final AtomicReference<State> state = new AtomicReference<State>();
    /** Counts the number of sub-subscriptions. */
    private final AtomicInteger count = new AtomicInteger();
    /** Indicate the request to unsubscribe from the main. */
    private final AtomicBoolean mainDone = new AtomicBoolean();

    /**
     * Create a RefCountSubscription by wrapping the given non-null Subscription.
     * 
     * @param s
     */
    public RefCountSubscription(Subscription s) {
        if (s == null) {
            throw new IllegalArgumentException("s");
        }
        this.main = s;
    }

    /**
     * Returns a new sub-subscription.
     */
    public Subscription getSubscription() {
        do {
            State s = state.get();
            if (s == State.UNSUBSCRIBED) {
                return Subscriptions.empty();
            }
            if (s == State.MUTATING) {
                continue;
            }
            if (state.compareAndSet(s, State.MUTATING)) {
                count.incrementAndGet();
                state.set(State.ACTIVE);
                return new InnerSubscription();
            }
        } while (true);
    }

    /**
     * Check if this subscription is already unsubscribed.
     */
    public boolean isUnsubscribed() {
        return state.get() == State.UNSUBSCRIBED;
    }

    @Override
    public void unsubscribe() {
        do {
            State s = state.get();
            if (s == State.UNSUBSCRIBED) {
                return;
            }
            if (s == State.MUTATING) {
                continue;
            }
            if (state.compareAndSet(s, State.MUTATING)) {
                if (mainDone.compareAndSet(false, true) && count.get() == 0) {
                    terminate();
                    return;
                }
                state.set(State.ACTIVE);
                break;
            }
        } while (true);
    }

    /**
     * Terminate this subscription by unsubscribing from main and setting the
     * state to UNSUBSCRIBED.
     */
    private void terminate() {
        state.set(State.UNSUBSCRIBED);
        Subscription r = main;
        main = null;
        r.unsubscribe();
    }

    /** Remove an inner subscription. */
    void innerDone() {
        do {
            State s = state.get();
            if (s == State.UNSUBSCRIBED) {
                return;
            }
            if (s == State.MUTATING) {
                continue;
            }
            if (state.compareAndSet(s, State.MUTATING)) {
                if (count.decrementAndGet() == 0 && mainDone.get()) {
                    terminate();
                    return;
                }
                state.set(State.ACTIVE);
                break;
            }
        } while (true);
    }

    /** The individual sub-subscriptions. */
    class InnerSubscription implements Subscription {
        final AtomicBoolean innerDone = new AtomicBoolean();

        @Override
        public void unsubscribe() {
            if (innerDone.compareAndSet(false, true)) {
                innerDone();
            }
        }
    };
}