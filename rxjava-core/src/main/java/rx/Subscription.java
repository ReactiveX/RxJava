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
package rx;

import rx.subscriptions.Subscriptions;

/**
 * Subscription returns from {@link Observable#subscribe(Subscriber)} to allow unsubscribing.
 * <p>
 * See utilities in {@link Subscriptions} and implementations in the {@code rx.subscriptions} package.
 * <p>
 * This interface is the RxJava equivalent of {@code IDisposable} in Microsoft's Rx implementation.
 */
public interface Subscription {

    /**
     * Stop receiving notifications on the {@link Subscriber} that was registered when this Subscription was received.
     * <p>
     * This allows unregistering an {@link Subscriber} before it has finished receiving all events (ie. before onCompleted is called).
     */
    public void unsubscribe();
    
    public boolean isUnsubscribed();

}
