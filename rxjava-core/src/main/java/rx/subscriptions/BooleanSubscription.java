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

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to exit the loop if unsubscribed.
 * 
 * @see Rx.Net equivalent BooleanDisposable at http://msdn.microsoft.com/en-us/library/system.reactive.disposables.booleandisposable(v=vs.103).aspx
 */
public class BooleanSubscription implements Subscription {

    private final AtomicBoolean unsubscribed = new AtomicBoolean(false);

    public boolean isUnsubscribed() {
        return unsubscribed.get();
    }

    @Override
    public void unsubscribe() {
        unsubscribed.set(true);
    }

}
