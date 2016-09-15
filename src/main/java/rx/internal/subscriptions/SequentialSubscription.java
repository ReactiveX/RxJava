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
package rx.internal.subscriptions;

import java.util.concurrent.atomic.AtomicReference;

import rx.Subscription;
import rx.subscriptions.Subscriptions;

/**
 * A container of a Subscription that supports operations of SerialSubscription
 * and MultipleAssignmentSubscription via methods (update, replace) and extends
 * AtomicReference to reduce allocation count (beware the API leak of AtomicReference!).
 * @since 1.1.9
 */
public final class SequentialSubscription extends AtomicReference<Subscription> implements Subscription {

    /** */
    private static final long serialVersionUID = 995205034283130269L;

    /**
     * Create an empty SequentialSubscription.
     */
    public SequentialSubscription() {

    }

    /**
     * Create a SequentialSubscription with the given initial Subscription.
     * @param initial the initial Subscription, may be null
     */
    public SequentialSubscription(Subscription initial) {
        lazySet(initial);
    }

    /**
     * Returns the current contained Subscription (may be null).
     * <p>(Remark: named as such because get() is final).
     * @return the current contained Subscription (may be null)
     */
    public Subscription current() {
        Subscription current = super.get();
        if (current == Unsubscribed.INSTANCE) {
            return Subscriptions.unsubscribed();
        }
        return current;
    }

    /**
     * Atomically sets the contained Subscription to the provided next value and unsubscribes
     * the previous value or unsubscribes the next value if this container is unsubscribed.
     * <p>(Remark: named as such because set() is final).
     * @param next the next Subscription to contain, may be null
     * @return true if the update succeeded, false if the container was unsubscribed
     */
    public boolean update(Subscription next) {
        for (;;) {
            Subscription current = get();

            if (current == Unsubscribed.INSTANCE) {
                if (next != null) {
                    next.unsubscribe();
                }
                return false;
            }

            if (compareAndSet(current, next)) {
                if (current != null) {
                    current.unsubscribe();
                }
                return true;
            }
        }
    }

    /**
     * Atomically replaces the contained Subscription to the provided next value but
     * does not unsubscribe the previous value or unsubscribes the next value if this
     * container is unsubscribed.
     * @param next the next Subscription to contain, may be null
     * @return true if the update succeeded, false if the container was unsubscribed
     */
    public boolean replace(Subscription next) {
        for (;;) {
            Subscription current = get();

            if (current == Unsubscribed.INSTANCE) {
                if (next != null) {
                    next.unsubscribe();
                }
                return false;
            }

            if (compareAndSet(current, next)) {
                return true;
            }
        }
    }

    /**
     * Atomically tries to set the contained Subscription to the provided next value and unsubscribes
     * the previous value or unsubscribes the next value if this container is unsubscribed.
     * <p>
     * Unlike {@link #update(Subscription)}, this doesn't retry if the replace failed
     * because a concurrent operation changed the underlying contained object.
     * @param next the next Subscription to contain, may be null
     * @return true if the update succeeded, false if the container was unsubscribed
     */
    public boolean updateWeak(Subscription next) {
        Subscription current = get();
        if (current == Unsubscribed.INSTANCE) {
            if (next != null) {
                next.unsubscribe();
            }
            return false;
        }
        if (compareAndSet(current, next)) {
            return true;
        }

        current = get();

        if (next != null) {
            next.unsubscribe();
        }
        return current == Unsubscribed.INSTANCE;
    }

    /**
     * Atomically tries to replace the contained Subscription to the provided next value but
     * does not unsubscribe the previous value or unsubscribes the next value if this container
     * is unsubscribed.
     * <p>
     * Unlike {@link #replace(Subscription)}, this doesn't retry if the replace failed
     * because a concurrent operation changed the underlying contained object.
     * @param next the next Subscription to contain, may be null
     * @return true if the update succeeded, false if the container was unsubscribed
     */
    public boolean replaceWeak(Subscription next) {
        Subscription current = get();
        if (current == Unsubscribed.INSTANCE) {
            if (next != null) {
                next.unsubscribe();
            }
            return false;
        }
        if (compareAndSet(current, next)) {
            return true;
        }

        current = get();
        if (current == Unsubscribed.INSTANCE) {
            if (next != null) {
                next.unsubscribe();
            }
            return false;
        }
        return true;
    }

    @Override
    public void unsubscribe() {
        Subscription current = get();
        if (current != Unsubscribed.INSTANCE) {
            current = getAndSet(Unsubscribed.INSTANCE);
            if (current != null && current != Unsubscribed.INSTANCE) {
                current.unsubscribe();
            }
        }
    }

    @Override
    public boolean isUnsubscribed() {
        return get() == Unsubscribed.INSTANCE;
    }
}
