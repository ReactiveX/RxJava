/*
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
import rx.functions.Cancellable;

/**
 * Abstraction over a {@link CompletableSubscriber} that gets either an onCompleted or onError
 * signal and allows registering an cancellation/unsubscription callback.
 * <p>
 * All methods are thread-safe; calling onCompleted or onError twice or one after the other has
 * no effect.
 * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
 */
@Experimental
public interface CompletableEmitter {

    /**
     * Notifies the CompletableSubscriber that the {@link Completable} has finished
     * sending push-based notifications.
     * <p>
     * The {@link Observable} will not call this method if it calls {@link #onError}.
     */
    void onCompleted();

    /**
     * Notifies the CompletableSubscriber that the {@link Completable} has experienced an error condition.
     * <p>
     * If the {@link Completable} calls this method, it will not thereafter call
     * {@link #onCompleted}.
     *
     * @param t
     *          the exception encountered by the Observable
     */
    void onError(Throwable t);

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
}
