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

import org.reactivestreams.Subscriber;

/**
 * A ConsumableFlowable represents a backpressured sequence of values optionally followed
 * by a teraminal signal.
 *
 * A ConsumableFlowable can serve multiple Subscribers subscribed via subscribe(Subscriber) dynamically
 * at various points in time.
 * <p>
 * A ConsumableFlowable signalling events to its Subscriber has to and will follow the following protocol:
 *  onSubscribe onNext* (onError | onComplete)?
 *  
 * @param <T> The type of the element received from an ConsumableFlowable
 */
public interface ConsumableFlowable<T> {
    /**
     * Request the ConsumableFlowable to start streaming data.
     * 
     * @param subscriber The Subscriber that will consume signals from this ConsumableFlowable.
     */
    void subscribe(Subscriber<? super T> subscriber);
}