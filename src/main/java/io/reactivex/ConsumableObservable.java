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
 * A ConsumableObservable represents a non-backpressured and unbounded sequence of values optionally followed
 * by a teraminal signal.
 *
 * A ConsumableObservable can serve multiple Observers subscribed via subscribe(Observer) dynamically
 * at various points in time.
 * <p>
 * A ConsumableObservable signalling events to its Observer has to and will follow the following protocol:
 *  onSubscribe onNext* (onError | onComplete)?
 *  
 * @param <T> The type of the element received from an ConsumableObservable
 */
public interface ConsumableObservable<T> {
    /**
     * Request the ConsumableObservable to start streaming data.
     * 
     * @param observer The Observer that will consume signals from this ConsumableObservable.
     */
    void subscribe(Observer<? super T> observer);
}