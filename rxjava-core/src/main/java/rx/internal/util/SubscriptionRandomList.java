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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.functions.Action1;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed together.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net equivalent CompositeDisposable</a>
 */
public final class SubscriptionRandomList<T extends Subscription> implements Subscription {

    private Set<T> subscriptions;
    private boolean unsubscribed = false;

    public SubscriptionRandomList() {
    }

    public SubscriptionRandomList(final T... subscriptions) {
        this.subscriptions = new HashSet<T>(Arrays.asList(subscriptions));
    }

    @Override
    public synchronized boolean isUnsubscribed() {
        return unsubscribed;
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
        Subscription unsubscribe = null;
        synchronized (this) {
            if (unsubscribed) {
                unsubscribe = s;
            } else {
                if (subscriptions == null) {
                    subscriptions = new HashSet<T>(4);
                }
                subscriptions.add(s);
            }
        }
        if (unsubscribe != null) {
            // call after leaving the synchronized block so we're not holding a lock while executing this
            unsubscribe.unsubscribe();
        }
    }

    /**
     * Removes a {@link Subscription} from this {@code CompositeSubscription}, and unsubscribes the {@link Subscription}.
     *
     * @param s
     *            the {@link Subscription} to remove
     */
    public void remove(final Subscription s) {
        boolean unsubscribe = false;
        synchronized (this) {
            if (unsubscribed || subscriptions == null) {
                return;
            }
            unsubscribe = subscriptions.remove(s);
        }
        if (unsubscribe) {
            // if we removed successfully we then need to call unsubscribe on it (outside of the lock)
            s.unsubscribe();
        }
    }

    /**
     * Unsubscribes any subscriptions that are currently part of this {@code CompositeSubscription} and remove
     * them from the {@code CompositeSubscription} so that the {@code CompositeSubscription} is empty and in
     * an unoperative state.
     */
    public void clear() {
        Collection<T> unsubscribe = null;
        synchronized (this) {
            if (unsubscribed || subscriptions == null) {
                return;
            } else {
                unsubscribe = subscriptions;
                subscriptions = null;
            }
        }
        unsubscribeFromAll(unsubscribe);
    }

    public void forEach(Action1<T> action) {
        T[] ss=null;
        synchronized (this) {
            if (unsubscribed || subscriptions == null) {
                return;
            }
            ss = subscriptions.toArray(ss);
        }
        for (T t : ss) {
            action.call(t);
        }
    }

    @Override
    public void unsubscribe() {
        Collection<T> unsubscribe = null;
        synchronized (this) {
            if (unsubscribed) {
                return;
            }
            unsubscribed = true;
            unsubscribe = subscriptions;
            subscriptions = null;
        }
        // we will only get here once
        unsubscribeFromAll(unsubscribe);
    }

    private static <T extends Subscription> void unsubscribeFromAll(Collection<T> subscriptions) {
        if (subscriptions == null) {
            return;
        }
        List<Throwable> es = null;
        for (T s : subscriptions) {
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
