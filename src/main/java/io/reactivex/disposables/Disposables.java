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

package io.reactivex.disposables;

import java.util.concurrent.Future;

import org.reactivestreams.Subscription;

import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.functions.Functions;

/**
 * Utility class to help create disposables by wrapping
 * other types.
 */
public final class Disposables {
    /** Utility class. */
    private Disposables() {
        throw new IllegalStateException("No instances!");
    }
    
    public static Disposable from(Runnable run) {
        return new RunnableDisposable(run);
    }

    public static Disposable from(Future<?> future) {
        return from(future, true);
    }

    public static Disposable from(Future<?> future, boolean allowInterrupt) {
        return new FutureDisposable(future, allowInterrupt);
    }

    public static Disposable from(Subscription subscription) {
        return new SubscriptionDisposable(subscription);
    }

    public static Disposable empty() {
        return from(Functions.EMPTY_RUNNABLE);
    }

    public static Disposable disposed() {
        return EmptyDisposable.INSTANCE;
    }
}
