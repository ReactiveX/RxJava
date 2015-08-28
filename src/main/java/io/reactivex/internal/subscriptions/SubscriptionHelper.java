/**
 * Copyright 2015 Netflix, Inc.
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

import org.reactivestreams.*;

import io.reactivex.plugins.RxJavaPlugins;

public enum SubscriptionHelper {
    ;
    
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
    
    public static void reportSubscriptionSet() {
        RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
    }

    /**
     * <p>
     * Make sure error reporting via s.onError is serialized.
     * 
     * @param current
     * @param next
     * @param s
     * @return
     */
    public static boolean validateSubscription(Subscription current, Subscription next, Subscriber<?> s) {
        if (next == null) {
            s.onError(new NullPointerException("next is null"));
            return true;
        }
        if (current != null) {
            next.cancel();
            reportSubscriptionSet();
            return true;
        }
        return false;
    }

    public static boolean validateRequest(long n) {
        if (n <= 0) {
            RxJavaPlugins.onError(new IllegalArgumentException("n > 0 required but it was " + n));
            return true;
        }
        return false;
    }
    
    /**
     * <p>
     * Make sure error reporting via s.onError is serialized.
     * 
     * @param n
     * @param current
     * @param s
     * @return
     */
    public static boolean validateRequest(long n, Subscription current, Subscriber<?> s) {
        if (n <= 0) {
            if (current != null) {
                current.cancel();
            }
            s.onError(new IllegalArgumentException("n > 0 required but it was " + n));
            return true;
        }
        return false;
    }
}
