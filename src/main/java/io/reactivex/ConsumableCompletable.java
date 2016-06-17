/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex;

/**
 * A ConsumableCompletable represents the provider of exactly one completion or exactly one error signal
 * and is mainly useful for composing side-effecting computation that don't generate values.
 * <p>
 * A ConsumableCompletable can serve multiple CompletableSubscriber subscribed via subscribe(CompletableSubscriber) dynamically
 * at various points in time.
 * <p>
 * A ConsumableCompletable signalling events to its CompletableSubscribers has to and will follow the following protocol:
 *  onSubscribe (onError | onComplete)?
 */
public interface ConsumableCompletable {
    
    /**
     * Request the ConsumableCompletable to signal a completion or an error signal.
     * 
     * @param subscriber The CompletableSubscriber instance that will receive either the completion
     * signal or the error signal.
     */
    void subscribe(CompletableSubscriber subscriber);
}