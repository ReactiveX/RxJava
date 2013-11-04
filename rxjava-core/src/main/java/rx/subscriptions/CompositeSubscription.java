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

import rx.Subscription;
import rx.util.CompositeException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed together.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net equivalent CompositeDisposable</a>
 */
public class CompositeSubscription implements Subscription {

    /*
     * The reason 'synchronized' is used on 'add' and 'unsubscribe' is because AtomicBoolean/ConcurrentLinkedQueue are both being modified so it needs to be done atomically.
     * 
     * TODO evaluate whether use of synchronized is a performance issue here and if it's worth using an atomic state machine or other non-locking approach
     */
    private AtomicBoolean unsubscribed = new AtomicBoolean(false);
    private final ConcurrentHashMap<Subscription, Boolean> subscriptions = new ConcurrentHashMap<Subscription, Boolean>();

    public CompositeSubscription(List<Subscription> subscriptions) {
        for (Subscription s : subscriptions) {
            this.subscriptions.put(s, Boolean.TRUE);
        }
    }

    public CompositeSubscription(Subscription... subscriptions) {
        for (Subscription s : subscriptions) {
            this.subscriptions.put(s, Boolean.TRUE);
        }
    }

    public void remove(Subscription s) {
        this.subscriptions.remove(s);
    }

    public boolean isUnsubscribed() {
        return unsubscribed.get();
    }

    public synchronized void add(Subscription s) {
        if (unsubscribed.get()) {
            s.unsubscribe();
        } else {
            subscriptions.put(s, Boolean.TRUE);
        }
    }

    @Override
    public synchronized void unsubscribe() {
        if (unsubscribed.compareAndSet(false, true)) {
            Collection<Throwable> es = null;
            for (Subscription s : subscriptions.keySet()) {
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
                throw new CompositeException("Failed to unsubscribe to 1 or more subscriptions.", es);
            }
        }
    }
}
