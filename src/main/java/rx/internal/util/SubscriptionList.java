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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import rx.Subscription;
import rx.exceptions.Exceptions;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed together.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net equivalent CompositeDisposable</a>
 */
public final class SubscriptionList implements Subscription {

    private LinkedList<Subscription> subscriptions;
    private volatile boolean unsubscribed;
    private final ReentrantLock lock = new ReentrantLock();

    public SubscriptionList() {
    }

    public SubscriptionList(final Subscription... subscriptions) {
        this.subscriptions = new LinkedList<Subscription>(Arrays.asList(subscriptions));
    }

    public SubscriptionList(Subscription s) {
        this.subscriptions = new LinkedList<Subscription>();
        this.subscriptions.add(s);
    }

    @Override
    public boolean isUnsubscribed() {
        return unsubscribed;
    }

    /**
     * Adds a new {@link Subscription} to this {@code SubscriptionList} if the {@code SubscriptionList} is
     * not yet unsubscribed. If the {@code SubscriptionList} <em>is</em> unsubscribed, {@code add} will
     * indicate this by explicitly unsubscribing the new {@code Subscription} as well.
     *
     * @param s
     *          the {@link Subscription} to add
     */
    public void add(final Subscription s) {
        if (s.isUnsubscribed()) {
            return;
        }
        if (!unsubscribed) {
            lock.lock();
            try {
                if (!unsubscribed) {
                    LinkedList<Subscription> subs = subscriptions;
                    if (subs == null) {
                        subs = new LinkedList<Subscription>();
                        subscriptions = subs;
                    }
                    subs.add(s);
                    return;
                }
            } finally {
                lock.unlock();
            }
        }
        // call after leaving the synchronized block so we're not holding a lock while executing this
        s.unsubscribe();
    }

    public void remove(final Subscription s) {
        if (!unsubscribed) {
            boolean unsubscribe = false;
            lock.lock();
            try {
                LinkedList<Subscription> subs = subscriptions;
                if (unsubscribed || subs == null) {
                    return;
                }
                unsubscribe = subs.remove(s);
            } finally {
                lock.unlock();
            }
            if (unsubscribe) {
                // if we removed successfully we then need to call unsubscribe on it (outside of the lock)
                s.unsubscribe();
            }
        }
    }

    /**
     * Unsubscribe from all of the subscriptions in the list, which stops the receipt of notifications on
     * the associated {@code Subscriber}.
     */
    @Override
    public void unsubscribe() {
        if (!unsubscribed) {
            List<Subscription> list;
            lock.lock();
            try {
                if (unsubscribed) {
                    return;
                }
                unsubscribed = true;
                list = subscriptions;
                subscriptions = null;
            } finally {
                lock.unlock();
            }
            // we will only get here once
            unsubscribeFromAll(list);
        }
    }

    private static void unsubscribeFromAll(Collection<Subscription> subscriptions) {
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
        Exceptions.throwIfAny(es);
    }
    /* perf support */
    public void clear() {
        if (!unsubscribed) {
            List<Subscription> list;
            lock.lock();
            try {
                list = subscriptions;
                subscriptions = null;
            } finally {
                lock.unlock();
            }
            unsubscribeFromAll(list);
        }
    }
    /**
     * Returns true if this composite is not unsubscribed and contains subscriptions.
     * @return {@code true} if this composite is not unsubscribed and contains subscriptions.
     */
    public boolean hasSubscriptions() {
        if (!unsubscribed) {
            lock.lock();
            try {
                return !unsubscribed && subscriptions != null && !subscriptions.isEmpty();
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}
