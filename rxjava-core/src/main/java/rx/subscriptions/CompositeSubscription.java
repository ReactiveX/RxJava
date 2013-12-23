/**
 * Copyright 2013 Netflix, Inc.
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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import rx.Subscription;
import rx.util.CompositeException;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed
 * together.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net equivalent CompositeDisposable</a>
 */
public class CompositeSubscription implements Subscription {
    /** Sentinel to indicate a thread is modifying the subscription set. */
    private static final Set<Subscription> MUTATE_SENTINEL = unmodifiableSet(Collections.<Subscription> emptySet());
    /** Sentinel to indicate the entire CompositeSubscription has been unsubscribed. */
    private static final Set<Subscription> UNSUBSCRIBED_SENTINEL = unmodifiableSet(Collections.<Subscription> emptySet());
    /** The reference to the set of subscriptions. */
    private final AtomicReference<Set<Subscription>> reference = new AtomicReference<Set<Subscription>>();

    public CompositeSubscription(final Subscription... subscriptions) {
        reference.set(new HashSet<Subscription>(asList(subscriptions)));
    }

    public boolean isUnsubscribed() {
        return reference.get() == UNSUBSCRIBED_SENTINEL;
    }

    public void add(final Subscription s) {
        do {
            final Set<Subscription> existing = reference.get();
            if (existing == UNSUBSCRIBED_SENTINEL) {
                s.unsubscribe();
                break;
            }

            if (existing == MUTATE_SENTINEL) {
                continue;
            }

            if (reference.compareAndSet(existing, MUTATE_SENTINEL)) {
                existing.add(s);
                reference.set(existing);
                break;
            }
        } while (true);
    }

    public void remove(final Subscription s) {
        do {
            final Set<Subscription> subscriptions = reference.get();
            if (subscriptions == UNSUBSCRIBED_SENTINEL) {
                s.unsubscribe();
                break;
            }

            if (subscriptions == MUTATE_SENTINEL) {
                continue;
            }

            if (reference.compareAndSet(subscriptions, MUTATE_SENTINEL)) {
                // also unsubscribe from it:
                // http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable.remove(v=vs.103).aspx
                subscriptions.remove(s);
                reference.set(subscriptions);
                s.unsubscribe();
                break;
            }
        } while (true);
    }

    public void clear() {
        do {
            final Set<Subscription> subscriptions = reference.get();
            if (subscriptions == UNSUBSCRIBED_SENTINEL) {
                break;
            }

            if (subscriptions == MUTATE_SENTINEL) {
                continue;
            }

            if (reference.compareAndSet(subscriptions, MUTATE_SENTINEL)) {
                final Set<Subscription> copy = new HashSet<Subscription>(
                        subscriptions);
                subscriptions.clear();
                reference.set(subscriptions);

                unsubscribeAll(copy);
                break;
            }
        } while (true);
    }

    /**
     * Unsubscribe from the collection of subscriptions.
     * <p>
     * Exceptions thrown by any of the {@code unsubscribe()} methods are
     * collected into a {@link CompositeException} and thrown once
     * all unsubscriptions have been attempted.
     * 
     * @param subs
     *            the collection of subscriptions
     */
    private void unsubscribeAll(Collection<Subscription> subs) {
        final Collection<Throwable> es = new ArrayList<Throwable>();
        for (final Subscription s : subs) {
            try {
                s.unsubscribe();
            } catch (final Throwable e) {
                es.add(e);
            }
        }
        if (!es.isEmpty()) {
            throw new CompositeException(
                    "Failed to unsubscribe to 1 or more subscriptions.", es);
        }
    }

    @Override
    public void unsubscribe() {
        do {
            final Set<Subscription> subscriptions = reference.get();
            if (subscriptions == UNSUBSCRIBED_SENTINEL) {
                break;
            }

            if (subscriptions == MUTATE_SENTINEL) {
                continue;
            }

            if (reference.compareAndSet(subscriptions, UNSUBSCRIBED_SENTINEL)) {
                unsubscribeAll(subscriptions);
                break;
            }
        } while (true);
    }
}
