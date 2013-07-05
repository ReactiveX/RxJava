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

import java.util.concurrent.Future;

import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

public class Subscriptions {
    /**
     * A {@link Subscription} that does nothing.
     * 
     * @return {@link Subscription}
     */
    public static Subscription empty() {
        return EMPTY;
    }

    /**
     * A {@link Subscription} implemented via a Func
     * 
     * @return {@link Subscription}
     */
    public static Subscription create(final Action0 unsubscribe) {
        return new Subscription() {

            @Override
            public void unsubscribe() {
                unsubscribe.call();
            }

        };
    }

    /**
     * A {@link Subscription} that wraps a {@link Future} and cancels it when unsubscribed.
     * 
     * 
     * @param f
     *            {@link Future}
     * @return {@link Subscription}
     */
    public static Subscription create(final Future<?> f) {
        return new Subscription() {

            @Override
            public void unsubscribe() {
                f.cancel(true);
            }

        };
    }

    /**
     * A {@link Subscription} that groups multiple Subscriptions together and unsubscribes from all of them together.
     * 
     * @param subscriptions
     *            Subscriptions to group together
     * @return {@link Subscription}
     */

    public static CompositeSubscription create(Subscription... subscriptions) {
        return new CompositeSubscription(subscriptions);
    }

    /**
     * A {@link Subscription} that does nothing when its unsubscribe method is called.
     */
    private static Subscription EMPTY = new Subscription() {
        public void unsubscribe() {
        }
    };
}
