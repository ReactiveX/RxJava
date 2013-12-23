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

import rx.Observable;
import rx.Subscription;
import rx.util.functions.Action0;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to exit the loop if unsubscribed.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.booleandisposable.aspx">Rx.Net equivalent BooleanDisposable</a>
 */
public class BooleanSubscription implements Subscription {
    /** The subscription state. */
    private final AtomicBoolean unsubscribed = new AtomicBoolean();

    public boolean isUnsubscribed() {
        return unsubscribed.get();
    }
    /**
     * Override this method to perform any action once if this BooleanSubscription
     * is unsubscribed.
     */
    protected void onUnsubscribe() { }
    
    @Override
    public void unsubscribe() {
        if (unsubscribed.compareAndSet(false, true)) {
            onUnsubscribe();
        }
    }
    /**
     * Returns a BooleanSubscription which calls the given action once
     * it is unsubscribed.
     * @param action the action to call when unsubscribing
     * @return the BooleanSubscription which calls the given action once
     * it is unsubscribed
     */
    public static BooleanSubscription withAction(final Action0 action) {
        if (action == null) {
            throw new NullPointerException("action");
        }
        return new BooleanSubscription() {
            @Override
            protected void onUnsubscribe() {
                action.call();
            }
        };
    }
}
