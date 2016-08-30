/**
 * Copyright 2016 Netflix, Inc.
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

import rx.annotations.Experimental;

/**
 * Abstraction over a RxJava Subscriber that allows associating
 * a resource with it and exposes the current number of downstream
 * requested amount.
 * <p>
 * The onNext, onError and onCompleted methods should be called 
 * in a sequential manner, just like the Observer's methods. The
 * other methods are threadsafe.
 *
 * @param <T> the value type to emit
 * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
 */
@Experimental
public interface AsyncEmitter<T> extends Observer<T> {

    /**
     * Sets a Subscription on this emitter; any previous Subscription
     * or Cancellation will be unsubscribed/cancelled.
     * @param s the subscription, null is allowed
     */
    void setSubscription(Subscription s);
    
    /**
     * Sets a Cancellable on this emitter; any previous Subscription
     * or Cancellation will be unsubscribed/cancelled.
     * @param c the cancellable resource, null is allowed
     */
    void setCancellation(Cancellable c);
    /**
     * The current outstanding request amount.
     * <p>This method it threadsafe.
     * @return the current outstanding request amount
     */
    long requested();
    
    /**
     * A functional interface that has a single close method
     * that can throw.
     */
    interface Cancellable {
        
        /**
         * Cancel the action or free a resource.
         * @throws Exception on error
         */
        void cancel() throws Exception;
    }
    
    /**
     * Options to handle backpressure in the emitter.
     */
    enum BackpressureMode {
        /**
         * No backpressure is applied an the onNext calls pass through the AsyncEmitter;
         * note that this may cause {@link rx.exceptions.MissingBackpressureException} or {@link IllegalStateException}
         * somewhere downstream.
         */
        NONE,
        /**
         * Signals a {@link rx.exceptions.MissingBackpressureException} if the downstream can't keep up.
         */
        ERROR,
        /**
         * Buffers (unbounded) all onNext calls until the dowsntream can consume them.
         */
        BUFFER,
        /**
         * Drops the incoming onNext value if the downstream can't keep up.
         */
        DROP,
        /**
         * Keeps the latest onNext value and overwrites it with newer ones until the downstream
         * can consume it.
         */
        LATEST
    }
}
