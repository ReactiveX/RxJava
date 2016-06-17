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

import io.reactivex.disposables.Disposable;

/**
 * A SingleSubscriber represents a consumer of exactly one value or exactly one exception
 * while allowing synchronous cancellation via a shared Disposable instance between it
 * and the ConsumableSingle producer.
 *
 * @param <T> the value type
 */
public interface SingleSubscriber<T> {
    
    /**
     * Invoked by the ConsumableSingle after the SingleSubscriber has been subscribed 
     * to it via ConsumableSingle.subscribe() method and receives an Disposable instance 
     * that can be used for cancelling the subscription.
     * @param d The IDisposable instance used for cancelling the subscription to the ConsumableSingle.
     */
    void onSubscribe(Disposable d);

    /**
     * Invoked by the ConsumableSingle when it produced the exactly one element.
     * <p>
     * The call to this method is mutually exclusive with onError.
     * 
     * @param value The element produced by the source Single.
     */
    void onSuccess(T value);

    /**
     * Invoked by the ConsumableSingle when it produced the exaclty one Exception.
     * <p>
     * The call to this method is mutually exclusive with onSuccess.
     * 
     * @param e The Exception produced by the source ConsumableSingle
     */
    void onError(Throwable e);
}