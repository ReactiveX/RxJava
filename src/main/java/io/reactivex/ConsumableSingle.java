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
 * A ConsumableSingle represents a provider of exactly one element or exactly one Exception to its
 * SingleSubscribers.
 *
 * A ConsumableSingle can serve multiple SingleSubscribers subscribed via subscribe(SingleSubscriber) dynamically
 * at various points in time.
 * 
 * A ConsumableSingle signalling events to its SingleSubscribers has to and will follow the following protocol:
 * onSubscribe (onSuccess | onError)?
 * 
 * @param <T> The type of the element signalled to the SingleSubscriber
 */
public interface ConsumableSingle<T> {
    /**
     * Request the ConsumableSingle to signal an element or Exception.
     * 
     * @param subscriber The SingleSubscriber instance that will receive the element or Exception.
     */
    void subscribe(SingleSubscriber<? super T> subscriber);
}