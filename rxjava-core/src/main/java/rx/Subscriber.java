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
package rx;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.internal.util.SubscriptionList;
import rx.subscriptions.CompositeSubscription;

/**
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * After a Subscriber calls an {@link Observable}'s {@link Observable#subscribe subscribe} method, the {@link Observable} calls the Subscriber's {@link #onNext} method to emit items. A well-behaved
 * {@link Observable} will call a Subscriber's {@link #onCompleted} method exactly once or the Subscriber's {@link #onError} method exactly once.
 * 
 * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki: Observable</a>
 * @param <T>
 *            the type of items the Subscriber expects to observe
 */
public abstract class Subscriber<T> implements Observer<T>, Subscription {

    private final SubscriptionList cs;
    private final Subscriber<?> op;

    @Deprecated
    protected Subscriber(CompositeSubscription cs) {
        this.op = null;
        this.cs = new SubscriptionList();
        add(cs);
    }

    protected Subscriber() {
        this.op = null;
        this.cs = new SubscriptionList();
    }

    protected Subscriber(Subscriber<?> op) {
        this.op = op;
        this.cs = op.cs;
    }

    /**
     * Registers an unsubscribe callback.
     *
     * @warn param "s" undescribed
     */
    public final void add(Subscription s) {
        cs.add(s);
    }

    @Override
    public final void unsubscribe() {
        cs.unsubscribe();
    }

    /**
     * Indicates whether this Subscriber has unsubscribed from its Observable.
     * 
     * @return {@code true} if this Subscriber has unsubscribed from its Observable, {@code false} otherwise
     */
    public final boolean isUnsubscribed() {
        return cs.isUnsubscribed();
    }

    public void request(int n) {
        State previous;
        State intermediate;
        State newState;
        do {
            previous = state.get();
            newState = previous.request(n);
            intermediate = newState;
            if (intermediate.p != null) {
                // we want to try and claim
                newState = intermediate.claim();
            }
        } while (!state.compareAndSet(previous, newState));
        if (intermediate.p != null && intermediate.n > 0) {
            // we have both P and N and won the claim so invoke
            intermediate.p.request(intermediate.n);
        }
    }

    public void setProducer(Producer producer) {
        if (op == null) {
            // end of chain, we must run
            int claimed = claim(producer);
            if (claimed >= 0) {
                // use count if we have it
                producer.request(claimed);
            } else {
                // otherwise we must run when at the end of the chain
                producer.request(State.INFINITE);
            }
        } else {
            // middle operator ... we pass thru unless a request has been made

            // if we have a non-0 value we will run as that means this operator has expressed interest
            int claimed = claim(producer);
            if (claimed == State.NOT_SET) {
                // we pass-thru to the next producer as it has not been set
                op.setProducer(producer);
            } else if (claimed != State.PAUSED) {
                // it has been set and is not paused so we'll execute
                producer.request(claimed);
            }
        }
    }

    private int claim(Producer producer) {
        State previous;
        State newState;
        do {
            previous = state.get();
            newState = previous.claim(producer);
        } while (!state.compareAndSet(previous, newState));
        return previous.n;
    }

    private final AtomicReference<State> state = new AtomicReference<State>(State.create());

    private static class State {
        private final int n;
        private final Producer p;

        private final static int PAUSED = 0;
        private final static int INFINITE = -1;
        private final static int NOT_SET = -2;

        public State(int n, Producer p) {
            this.n = n;
            this.p = p;
        }

        public static State create() {
            return new State(-2, null); // not set
        }

        public State request(int _n) {
            int newN = _n;
            if (n > 0) {
                // add to existing if it hasn't been used yet
                newN = n + _n;
            }
            return new State(newN, p);
        }

        public State claim(Producer producer) {
            if (n == -2) {
                return new State(-2, producer);
            } else {
                return new State(0, producer);
            }
        }

        public State claim() {
            if (n == -2) {
                return this; // not set so return as is
            } else {
                return new State(0, p);
            }
        }

        public State producer(Producer _p) {
            return new State(n, _p);
        }

    }
}
