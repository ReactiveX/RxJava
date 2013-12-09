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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import rx.Subscription;
import rx.util.CompositeException;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed
 * together.
 * 
 * @see <a
 *      href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net
 *      equivalent CompositeDisposable</a>
 */
public class CompositeSubscription implements Subscription {
    private static final Set<Subscription> MUTATE_STATE = unmodifiableSet(new HashSet<Subscription>());
    private static final Set<Subscription> UNSUBSCRIBED_STATE = unmodifiableSet(new HashSet<Subscription>());

    private final AtomicReference<Set<Subscription>> reference = new AtomicReference<Set<Subscription>>();

    public CompositeSubscription(final Subscription... subscriptions) {
        reference.set(new HashSet<Subscription>(asList(subscriptions)));
    }

    public boolean isUnsubscribed() {
        return reference.get() == UNSUBSCRIBED_STATE;
    }

    public void add(final Subscription s) {
        do {
            final Set<Subscription> existing = reference.get();
            if (existing == UNSUBSCRIBED_STATE) {
                s.unsubscribe();
                break;
            }

            if (reference.compareAndSet(existing, MUTATE_STATE)) {
                existing.add(s);
                reference.set(existing);
                break;
            }
        } while (true);
    }

    public void remove(final Subscription s) {
        do {
            final Set<Subscription> subscriptions = reference.get();
            if (subscriptions == UNSUBSCRIBED_STATE) {
                s.unsubscribe();
                break;
            }

            if (reference.compareAndSet(subscriptions, MUTATE_STATE)) {
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
            if (subscriptions == UNSUBSCRIBED_STATE) {
                break;
            }

            if (reference.compareAndSet(subscriptions, MUTATE_STATE)) {
                final Set<Subscription> copy = new HashSet<Subscription>(
                        subscriptions);
                subscriptions.clear();
                reference.set(subscriptions);

                for (final Subscription subscription : copy) {
                    subscription.unsubscribe();
                }
                break;
            }
        } while (true);
    }

    @Override
    public void unsubscribe() {
        do {
            final Set<Subscription> subscriptions = reference.get();
            if (subscriptions == UNSUBSCRIBED_STATE) {
                break;
            }

            if (subscriptions == MUTATE_STATE) {
                continue;
            }

            if (reference.compareAndSet(subscriptions, UNSUBSCRIBED_STATE)) {
                final Collection<Throwable> es = new ArrayList<Throwable>();
                for (final Subscription s : subscriptions) {
                    try {
                        s.unsubscribe();
                    } catch (final Throwable e) {
                        es.add(e);
                    }
                }
                if (es.isEmpty()) {
                    break;
                }
                throw new CompositeException(
                        "Failed to unsubscribe to 1 or more subscriptions.", es);
            }
        } while (true);
    }
}
