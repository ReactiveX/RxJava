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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.functions.Action1;

/**
 * Similar to CompositeSubscription but giving extra access to internals so we can reuse a datastructure.
 */
public final class SubscriptionSet<T extends Subscription> implements Subscription {

    // TODO find a better data structure
    private ConcurrentHashMap<T, T> subscriptions;
    private final AtomicBoolean unsubscribed = new AtomicBoolean(false);

    public SubscriptionSet() {
    }

    public SubscriptionSet(final T... subscriptions) {
        this.subscriptions = new ConcurrentHashMap<T, T>();
        for (T t : subscriptions) {
            this.subscriptions.put(t, t);
        }
    }

    @Override
    public boolean isUnsubscribed() {
        return unsubscribed.get();
    }

    /**
     * Adds a new {@link Subscription} to this {@code CompositeSubscription} if the {@code CompositeSubscription} is not yet unsubscribed. If the {@code CompositeSubscription} <em>is</em>
     * unsubscribed, {@code add} will indicate this by explicitly unsubscribing the new {@code Subscription} as
     * well.
     *
     * @param s
     *            the {@link Subscription} to add
     */
    public void add(final T s) {
        if (unsubscribed.get()) {
            s.unsubscribe();
        } else {
            if (subscriptions == null) {
                subscriptions = new ConcurrentHashMap<T, T>(4);
            }
            subscriptions.put(s, s);
            // double check for race condition
            if (unsubscribed.get()) {
                s.unsubscribe();
            }
        }
    }

    /**
     * Removes a {@link Subscription} from this {@code CompositeSubscription}, and unsubscribes the {@link Subscription}.
     *
     * @param s
     *            the {@link Subscription} to remove
     */
    public void remove(final T s) {
        if (unsubscribed.get() || subscriptions == null) {
            return;
        }
        if (subscriptions.remove(s) != null) {
            // if we removed successfully we then need to call unsubscribe on it
            s.unsubscribe();
        }
    }

    @Override
    public void unsubscribe() {
        if (unsubscribed.compareAndSet(false, true) && subscriptions != null) {
            // we will only get here once
            unsubscribeFromAll(subscriptions.keySet());
        }
    }

    private static void unsubscribeFromAll(Collection<? extends Subscription> subscriptions) {
        if (subscriptions == null) {
            return;
        }
        List<Throwable> es = null;
        for (Subscription s : subscriptions) {
            try {
                s.unsubscribe();
            } catch (Throwable e) {
                if (es == null) {
                    es = new ArrayList<Throwable>();
                }
                es.add(e);
            }
        }
        if (es != null) {
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
}
