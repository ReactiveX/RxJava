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

import rx.Subscription;
import rx.exceptions.CompositeException;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed together.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net equivalent CompositeDisposable</a>
 */
public final class SubscriptionList implements Subscription {

    private List<Subscription> subscriptions;
    private boolean unsubscribed = false;

    public SubscriptionList() {
    }

    public SubscriptionList(final Subscription... subscriptions) {
        this.subscriptions = new LinkedList<Subscription>(Arrays.asList(subscriptions));
    }

    @Override
    public synchronized boolean isUnsubscribed() {
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
        Subscription unsubscribe = null;
        synchronized (this) {
            if (unsubscribed) {
                unsubscribe = s;
            } else {
                if (subscriptions == null) {
                    subscriptions = new LinkedList<Subscription>();
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
     * Unsubscribe from all of the subscriptions in the list, which stops the receipt of notifications on
     * the associated {@code Subscriber}.
     */
    @Override
    public void unsubscribe() {
        synchronized (this) {
            if (unsubscribed) {
                return;
            }
            unsubscribed = true;
        }
        // we will only get here once
        unsubscribeFromAll(subscriptions);
        subscriptions = null;
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
