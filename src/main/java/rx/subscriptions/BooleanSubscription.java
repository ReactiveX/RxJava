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
package rx.subscriptions;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to exit the loop
 * if unsubscribed.
 */
public final class BooleanSubscription implements Subscription {

    final AtomicReference<Action0> actionRef;

    public BooleanSubscription() {
        actionRef = new AtomicReference<Action0>();
    }

    private BooleanSubscription(Action0 action) {
        actionRef = new AtomicReference<Action0>(action);
    }

    /**
     * Creates a {@code BooleanSubscription} without unsubscribe behavior.
     *
     * @return the created {@code BooleanSubscription}
     */
    public static BooleanSubscription create() {
        return new BooleanSubscription();
    }

    /**
     * Creates a {@code BooleanSubscription} with a specified function to invoke upon unsubscribe.
     *
     * @param onUnsubscribe
     *          an {@link Action0} to invoke upon unsubscribe
     * @return the created {@code BooleanSubscription}
     */
    public static BooleanSubscription create(Action0 onUnsubscribe) {
        return new BooleanSubscription(onUnsubscribe);
    }

    @Override
    public boolean isUnsubscribed() {
        return actionRef.get() == EMPTY_ACTION;
    }

    @Override
    public final void unsubscribe() {
        Action0 action = actionRef.get();
        if (action != EMPTY_ACTION) {
            action = actionRef.getAndSet(EMPTY_ACTION);
            if (action != null && action != EMPTY_ACTION) {
                action.call();
            }
        }
    }

    static final Action0 EMPTY_ACTION = new Action0() {
        @Override
        public void call() {

        }
    };

}
