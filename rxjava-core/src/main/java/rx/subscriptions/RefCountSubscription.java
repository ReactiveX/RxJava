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

import java.util.concurrent.atomic.AtomicBoolean;
import rx.Subscription;

/**
 * Keeps track of the sub-subscriptions and unsubscribes the underlying
 * subscription once all sub-subscriptions have unsubscribed.
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/system.reactive.disposables.refcountdisposable.aspx'>MSDN RefCountDisposable</a>
 */
public class RefCountSubscription implements Subscription {
    private final Object guard = new Object();
    private Subscription main;
    private boolean done;
    private int count;
    public RefCountSubscription(Subscription s) {
        if (s == null) {
            throw new IllegalArgumentException("s");
        }
        this.main = s;
    }
    /**
     * Returns a new sub-subscription.
     */
    public Subscription getSubscription() {
        synchronized (guard) {
            if (main == null) {
                return Subscriptions.empty();
            } else {
                count++;
                return new InnerSubscription();
            }
        }
    }
    /**
     * Check if this subscription is already unsubscribed.
     */
    public boolean isUnsubscribed() {
        synchronized (guard) {
            return main == null;
        }
    }
    @Override
    public void unsubscribe() {
        Subscription s = null;
        synchronized (guard) {
            if (main != null && !done) {
                done = true;
                if (count == 0) {
                    s = main;
                    main = null;
                }
            }
        }
        if (s != null) {
            s.unsubscribe();
        }
    }
    /** Remove an inner subscription. */
    void innerDone() {
        Subscription s = null;
        synchronized (guard) {
            if (main != null) {
                count--;
                if (done && count == 0) {
                    s = main;
                    main = null;
                }
            }
        }
        if (s != null) {
            s.unsubscribe();
        }
    }
    /** The individual sub-subscriptions. */
    class InnerSubscription implements Subscription {
        final AtomicBoolean innerDone = new AtomicBoolean();
        @Override
        public void unsubscribe() {
            if (innerDone.compareAndSet(false, true)) {
                innerDone();
            }
        }
    };
}