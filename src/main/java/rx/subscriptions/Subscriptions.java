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

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Actions;

/**
 * Helper methods and utilities for creating and working with {@link Subscription} objects
 */
public final class Subscriptions {
    private Subscriptions() {
        throw new IllegalStateException("No instances!");
    }
    /**
     * Returns a {@link Subscription} that does nothing.
     * 
     * @return a {@link Subscription} that does nothing
     */
    public static Subscription empty() {
        return EMPTY;
    }

    /**
     * Creates and returns a {@link Subscription} that invokes the given {@link Action0} when unsubscribed.
     * 
     * @param unsubscribe
     *            Action to invoke on unsubscribe.
     * @return {@link Subscription}
     */
    public static Subscription create(final Action0 unsubscribe) {
        return BooleanSubscription.create(unsubscribe);
    }

    /**
     * Converts a {@link Future} into a {@link Subscription} and cancels it when unsubscribed.
     * 
     * @param f
     *            the {@link Future} to convert
     * @return a {@link Subscription} that wraps {@code f}
     */
    public static Subscription from(final Future<?> f) {
        return new FutureSubscription(f);
    }

        /** Naming classes helps with debugging. */
    private static final class FutureSubscription implements Subscription {
        final Future<?> f;

        public FutureSubscription(Future<?> f) {
            this.f = f;
        }
        @Override
        public void unsubscribe() {
            f.cancel(true);
        }

        @Override
        public boolean isUnsubscribed() {
            return f.isCancelled();
        }
    }

    /**
     * Converts a set of {@link Subscription}s into a {@link CompositeSubscription} that groups the multiple
     * Subscriptions together and unsubscribes from all of them together.
     * 
     * @param subscriptions
     *            the Subscriptions to group together
     * @return a {@link CompositeSubscription} representing the {@code subscriptions} set
     */

    public static CompositeSubscription from(Subscription... subscriptions) {
        return new CompositeSubscription(subscriptions);
    }

    /**
     * A {@link Subscription} that does nothing when its unsubscribe method is called.
     */
    private static final Empty EMPTY = new Empty();
        /** Naming classes helps with debugging. */
    private static final class Empty implements Subscription {
        @Override
        public void unsubscribe() {
        }

        @Override
        public boolean isUnsubscribed() {
            return false;
        }
    }
}
