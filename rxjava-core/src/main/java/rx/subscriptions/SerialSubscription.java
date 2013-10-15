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

/**
 * Represents a subscription whose underlying subscription can be swapped for another subscription
 * which causes the previous underlying subscription to be unsubscribed.
 *
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.serialdisposable(v=vs.103).aspx">Rx.Net equivalent SerialDisposable</a>
 */
public class SerialSubscription implements Subscription {
    private boolean unsubscribed;
    private Subscription subscription;
    private final Object gate = new Object();

    @Override
    public void unsubscribe() {
        Subscription toUnsubscribe = null;
        synchronized (gate) {
            if (!unsubscribed) {
                if (subscription != null) {
                    toUnsubscribe = subscription;
                    subscription = null;
                }
                unsubscribed = true;
            }
        }
        if (toUnsubscribe != null) {
            toUnsubscribe.unsubscribe();
        }
    }

    public Subscription getSubscription() {
        synchronized (gate) {
            return subscription;
        }
    }

    public void setSubscription(Subscription subscription) {
        Subscription toUnsubscribe = null;
        synchronized (gate) {
            if (!unsubscribed) {
                if (this.subscription != null) {
                    toUnsubscribe = this.subscription;
                }
                this.subscription = subscription;
            } else {
                toUnsubscribe = subscription;
            }
        }
        if (toUnsubscribe != null) {
            toUnsubscribe.unsubscribe();
        }
    }
}
