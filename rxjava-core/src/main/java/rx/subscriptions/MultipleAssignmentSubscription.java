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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscription;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to exit the loop if unsubscribed.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.multipleassignmentdisposable">Rx.Net equivalent MultipleAssignmentDisposable</a>
 */
public class MultipleAssignmentSubscription implements Subscription {
    private AtomicReference<Subscription> reference = new AtomicReference<Subscription>();
    /** Sentinel for the unsubscribed state. */
    private static final Subscription UNSUBSCRIBED_SENTINEL = new Subscription() {
        @Override
        public void unsubscribe() {
        }
    };
    public boolean isUnsubscribed() {
        return reference.get() == UNSUBSCRIBED_SENTINEL;
    }

    @Override
    public void unsubscribe() {
        Subscription s = reference.getAndSet(UNSUBSCRIBED_SENTINEL);
        if (s != null) {
            s.unsubscribe();
        }
    }

    public void setSubscription(Subscription s) {
        do {
            Subscription r = reference.get();
            if (r == UNSUBSCRIBED_SENTINEL) {
                s.unsubscribe();
                return;
            }
            if (reference.compareAndSet(r, s)) {
                break;
            }
        } while (true);
    }

    public Subscription getSubscription() {
        Subscription s = reference.get();
        return s != UNSUBSCRIBED_SENTINEL ? s : Subscriptions.empty();
    }

}
