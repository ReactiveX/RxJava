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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.functions.Action1;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed together.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net equivalent CompositeDisposable</a>
 */
public final class SubscriptionList implements Subscription {

    @SuppressWarnings("unchecked")
    private volatile IndexedRingBuffer<Subscription> subscriptions = IndexedRingBuffer.getInstance();
    private volatile int unsubscribed = 0;
    private final static AtomicIntegerFieldUpdater<SubscriptionList> UNSUBSCRIBED = AtomicIntegerFieldUpdater.newUpdater(SubscriptionList.class, "unsubscribed");

    public SubscriptionList() {
    }

    public SubscriptionList(final Subscription... subscriptions) {
        for (Subscription t : subscriptions) {
            this.subscriptions.add(t);
        }
    }

    @Override
    public synchronized boolean isUnsubscribed() {
        return unsubscribed == 1;
    }

    /**
     * Adds a new {@link Subscription} to this {@code SubscriptionList} if the {@code SubscriptionList} is
     * not yet unsubscribed. If the {@code SubscriptionList} <em>is</em> unsubscribed, {@code add} will
     * indicate this by explicitly unsubscribing the new {@code Subscription} as well.
     *
     * @param s
     *            the {@link Subscription} to add
     */
    public void add(final Subscription s) {
        if (unsubscribed == 1 || subscriptions == null) {
            s.unsubscribe();
        } else {
            subscriptions.add(s);
            // double check for race condition
            if (unsubscribed == 1) {
                s.unsubscribe();
            }
        }
    }

    @Override
    public void unsubscribe() {
        if (UNSUBSCRIBED.compareAndSet(this, 0, 1) && subscriptions != null) {
            // we will only get here once
            unsubscribeFromAll(subscriptions);

            IndexedRingBuffer<Subscription> s = subscriptions;
            subscriptions = null;
            s.unsubscribe();
        }
    }

    public List<Throwable> forEach(Action1<Subscription> action) {
        if (unsubscribed == 1 || subscriptions == null) {
            return Collections.emptyList();
        }
        return subscriptions.forEach(action);
    }

    private static void unsubscribeFromAll(IndexedRingBuffer<Subscription> subscriptions) {
        if (subscriptions == null) {
            return;
        }
        List<Throwable> es = subscriptions.forEach(UNSUBSCRIBE);

        if (!es.isEmpty()) {
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

    private final static Action1<Subscription> UNSUBSCRIBE = new Action1<Subscription>() {

        @Override
        public void call(Subscription s) {
            s.unsubscribe();
        }
    };
}
