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

package io.reactivex.internal.subscriptions;

import org.reactivestreams.Subscription;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility methods to validate Subscriptions and Disposables in the various onSubscribe calls.
 */
public enum SubscriptionHelper {
    ;
    
    /**
     * Verifies that current is null, next is not null, otherwise signals errors
     * to the RxJavaPlugins and returns true
     * @param current the current Subscription, expected to be null
     * @param next the next Subscription, expected to be non-null
     * @return true if the validation failed
     */
    public static boolean validateSubscription(Subscription current, Subscription next) {
        if (next == null) {
            RxJavaPlugins.onError(new NullPointerException("next is null"));
            return true;
        }
        if (current != null) {
            next.cancel();
            reportSubscriptionSet();
            return true;
        }
        return false;
    }
    
    /**
     * Reports that the subscription is already set to the RxJavaPlugins error handler.
     */
    public static void reportSubscriptionSet() {
        RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
    }

    /**
     * Verifies that current is null, next is not null, otherwise signals errors
     * to the RxJavaPlugins and returns true
     * @param current the current Disposable, expected to be null
     * @param next the next Disposable, expected to be non-null
     * @return true if the validation failed
     */
    public static boolean validateDisposable(Disposable current, Disposable next) {
        if (next == null) {
            RxJavaPlugins.onError(new NullPointerException("next is null"));
            return true;
        }
        if (current != null) {
            next.dispose();
            reportDisposableSet();
            return true;
        }
        return false;
    }
    
    /**
     * Reports that the disposable is already set to the RxJavaPlugins error handler.
     */
    public static void reportDisposableSet() {
        RxJavaPlugins.onError(new IllegalStateException("Disposable already set!"));
    }

    /**
     * Validates that the n is positive.
     * @param n the request amount
     * @return false if n is non-positive.
     */
    public static boolean validateRequest(long n) {
        if (n <= 0) {
            RxJavaPlugins.onError(new IllegalArgumentException("n > 0 required but it was " + n));
            return true;
        }
        return false;
    }
    
    /** Singleton instance of a function which calls cancel on the supplied Subscription. */
    static final Consumer<Subscription> CONSUME_AND_CANCEL = new Consumer<Subscription>() {
        @Override
        public void accept(Subscription s) {
            s.cancel();
        }
    };
    
    /**
     * Returns a consumer which calls cancel on the supplied Subscription.
     * @return  a consumer which calls cancel on the supplied Subscription
     */
    public static Consumer<Subscription> consumeAndCancel() {
        return CONSUME_AND_CANCEL;
    }
}
