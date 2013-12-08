/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.subscriptions;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscription;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to exit the loop if unsubscribed.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.multipleassignmentdisposable">Rx.Net equivalent MultipleAssignmentDisposable</a>
 */
public class MultipleAssignmentSubscription implements Subscription {

    private final AtomicReference<Subscription> inner = new AtomicReference<Subscription>();
    private static final Subscription UNSUBSCRIBED = new Subscription() {
        @Override
        public void unsubscribe() {
        }
    };
    public boolean isUnsubscribed() {
        return inner.get() == UNSUBSCRIBED;
    }

    @Override
    public void unsubscribe() {
        Subscription s = inner.getAndSet(UNSUBSCRIBED);
        if (s != null) {
            s.unsubscribe();
        }
    }

    public void setSubscription(Subscription s) {
        do {
            Subscription r = inner.get();
            if (r == UNSUBSCRIBED) {
                s.unsubscribe();
                return;
            } else {
                if (inner.compareAndSet(r, s)) {
                    return;
                }
            }
        } while (true);
    }

    public Subscription getSubscription() {
        Subscription r = inner.get();
        // don't leak the unsubscription sentinel
        if (r == UNSUBSCRIBED) {
            return Subscriptions.empty();
        }
        return r;
    }

}
