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
package rx.internal.util;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Subscription;
import rx.functions.Func1;

/**
 * Similar to CompositeSubscription but giving extra access to internals so we can reuse a datastructure.
 * <p>
 * NOTE: This purposefully is leaking the internal data structure through the API for efficiency reasons to avoid extra object allocations.
 */
public final class SubscriptionIndexedRingBuffer<T extends Subscription> implements Subscription {

    @SuppressWarnings("unchecked")
    private volatile IndexedRingBuffer<T> subscriptions = IndexedRingBuffer.getInstance();
    private volatile int unsubscribed = 0;
    @SuppressWarnings("rawtypes")
    private final static AtomicIntegerFieldUpdater<SubscriptionIndexedRingBuffer> UNSUBSCRIBED = AtomicIntegerFieldUpdater.newUpdater(SubscriptionIndexedRingBuffer.class, "unsubscribed");

    public SubscriptionIndexedRingBuffer() {
    }

    public SubscriptionIndexedRingBuffer(final T... subscriptions) {
        for (T t : subscriptions) {
            this.subscriptions.add(t);
        }
    }

    @Override
    public boolean isUnsubscribed() {
        return unsubscribed == 1;
    }

    /**
     * Adds a new {@link Subscription} to this {@code CompositeSubscription} if the {@code CompositeSubscription} is not yet unsubscribed. If the {@code CompositeSubscription} <em>is</em>
     * unsubscribed, {@code add} will indicate this by explicitly unsubscribing the new {@code Subscription} as
     * well.
     *
     * @param s
     *            the {@link Subscription} to add
     * 
     * @return int index that can be used to remove a Subscription
     */
    public synchronized int add(final T s) {
        // TODO figure out how to remove synchronized here. See https://github.com/ReactiveX/RxJava/issues/1420
        if (unsubscribed == 1 || subscriptions == null) {
            s.unsubscribe();
            return -1;
        } else {
            int n = subscriptions.add(s);
            // double check for race condition
            if (unsubscribed == 1) {
                s.unsubscribe();
            }
            return n;
        }
    }

    /**
     * Uses the Node received from `add` to remove this Subscription.
     * <p>
     * Unsubscribes the Subscription after removal
     */
    public void remove(final int n) {
        if (unsubscribed == 1 || subscriptions == null || n < 0) {
            return;
        }
        Subscription t = subscriptions.remove(n);
        if (t != null) {
            // if we removed successfully we then need to call unsubscribe on it
            if (t != null) {
                t.unsubscribe();
            }
        }
    }

    /**
     * Uses the Node received from `add` to remove this Subscription.
     * <p>
     * Does not unsubscribe the Subscription after removal.
     */
    public void removeSilently(final int n) {
        if (unsubscribed == 1 || subscriptions == null || n < 0) {
            return;
        }
        subscriptions.remove(n);
    }

    @Override
    public void unsubscribe() {
        if (UNSUBSCRIBED.compareAndSet(this, 0, 1) && subscriptions != null) {
            // we will only get here once
            unsubscribeFromAll(subscriptions);

            IndexedRingBuffer<T> s = subscriptions;
            subscriptions = null;
            s.unsubscribe();
        }
    }

    public int forEach(Func1<T, Boolean> action) {
        return forEach(action, 0);
    }

    /**
     * 
     * @param action
     * @return int of last index seen if forEach exited early
     */
    public synchronized int forEach(Func1<T, Boolean> action, int startIndex) {
        // TODO figure out how to remove synchronized here. See https://github.com/ReactiveX/RxJava/issues/1420
        if (unsubscribed == 1 || subscriptions == null) {
            return 0;
        }
        return subscriptions.forEach(action, startIndex);
    }

    private static void unsubscribeFromAll(IndexedRingBuffer<? extends Subscription> subscriptions) {
        if (subscriptions == null) {
            return;
        }

        // TODO migrate to drain (remove while we're doing this) so we don't have to immediately clear it in IndexedRingBuffer.releaseToPool?
        subscriptions.forEach(UNSUBSCRIBE);
    }

    private final static Func1<Subscription, Boolean> UNSUBSCRIBE = new Func1<Subscription, Boolean>() {

        @Override
        public Boolean call(Subscription s) {
            s.unsubscribe();
            return Boolean.TRUE;
        }
    };

}
