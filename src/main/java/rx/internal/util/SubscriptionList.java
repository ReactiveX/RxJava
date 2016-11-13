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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import rx.Subscription;
import rx.doppl.J2objcWeakReference;
import rx.exceptions.Exceptions;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed together.
 *
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net equivalent CompositeDisposable</a>
 */
public final class SubscriptionList implements Subscription {

    private List<J2objcWeakReference<Subscription>> subscriptions;
    private volatile boolean    unsubscribed;

    /**
     * Constructs an empty SubscriptionList.
     */
    public SubscriptionList() {
        // nothing to do
    }

    /**
     * Constructs a SubscriptionList with the given initial child subscriptions.
     * @param subscriptions the array of subscriptions to start with
     */
    public SubscriptionList(final Subscription... subscriptions) {
        this.subscriptions = new LinkedList<J2objcWeakReference<Subscription>>();
        for(Subscription subscription : subscriptions)
        {
            this.subscriptions.add(new J2objcWeakReference<Subscription>(subscription));
        }
    }

    /**
     * Constructs a SubscriptionList with the given initial child subscription.
     * @param s the initial subscription instance
     */
    public SubscriptionList(Subscription s) {
        this.subscriptions = new LinkedList<J2objcWeakReference<Subscription>>();
        this.subscriptions.add(new J2objcWeakReference<Subscription>(s));
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
            synchronized (this) {
                if (!unsubscribed) {
                    List<J2objcWeakReference<Subscription>> subs = subscriptions;
                    if (subs == null) {
                        subs = new LinkedList<J2objcWeakReference<Subscription>>();
                        subscriptions = subs;
                    }
                    subs.add(new J2objcWeakReference<Subscription>(s));
                    return;
                }
            }
        }
        // call after leaving the synchronized block so we're not holding a lock while executing this
        s.unsubscribe();
    }

    public void remove(final Subscription s) {
        if (!unsubscribed) {
            boolean unsubscribe = false;
            synchronized (this) {
                List<J2objcWeakReference<Subscription>> subs = subscriptions;
                if (unsubscribed || subs == null) {
                    return;
                }

                Iterator<J2objcWeakReference<Subscription>> iterator = subs.iterator();
                while(iterator.hasNext())
                {
                    J2objcWeakReference<Subscription> next = iterator.next();
                    if(next.get() != null && next.equals(s))
                    {
                        unsubscribe = true;
                        iterator.remove();
                        break;
                    }
                }
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
            List<J2objcWeakReference<Subscription>> list;
            synchronized (this) {
                if (unsubscribed) {
                    return;
                }
                unsubscribed = true;
                list = subscriptions;
                subscriptions = null;
            }
            // we will only get here once
            unsubscribeFromAll(list);
        }
    }

    private static void unsubscribeFromAll(Collection<J2objcWeakReference<Subscription>> subscriptions) {
        if (subscriptions == null) {
            return;
        }
        List<Throwable> es = null;
        for (J2objcWeakReference<Subscription> wr : subscriptions) {
            Subscription s = wr.get();
            try {
                if(s != null)
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
            List<J2objcWeakReference<Subscription>> list;
            synchronized (this) {
                list = subscriptions;
                subscriptions = null;
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
            synchronized (this) {
                if(!unsubscribed && subscriptions != null)
                {
                    Iterator<J2objcWeakReference<Subscription>> iterator = subscriptions.iterator();
                    while(iterator.hasNext())
                    {
                        J2objcWeakReference<Subscription> next = iterator.next();
                        if(next.get() != null)
                            return true;
                    }
                }
            }
        }
        return false;
    }
}
