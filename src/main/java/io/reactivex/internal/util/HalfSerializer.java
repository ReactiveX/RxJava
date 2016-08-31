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
package io.reactivex.internal.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscriber;

import io.reactivex.Observer;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility methods to perform half-serialization: a form of serialization
 * where onNext is guaranteed to be called from a single thread but
 * onError or onCompleted may be called from any threads.
 */
public final class HalfSerializer {
    /** Utility class. */
    private HalfSerializer() {
        throw new IllegalStateException("No instances!");
    }
    
    public static <T> void onNext(Subscriber<? super T> subscriber, T value, 
            AtomicInteger wip, AtomicThrowable error) {
        if (wip.get() == 0 && wip.compareAndSet(0, 1)) {
            subscriber.onNext(value);
            if (wip.decrementAndGet() != 0) {
                Throwable ex = error.terminate();
                if (ex != null) {
                    subscriber.onError(ex);
                } else {
                    subscriber.onComplete();
                }
            }
        }
    }
    
    public static void onError(Subscriber<?> subscriber, Throwable ex, 
            AtomicInteger wip, AtomicThrowable error) {
        if (error.addThrowable(ex)) {
            if (wip.getAndIncrement() == 0) {
                subscriber.onError(error.terminate());
            }
        } else {
            RxJavaPlugins.onError(ex);
        }
    }
    
    public static void onComplete(Subscriber<?> subscriber, AtomicInteger wip, AtomicThrowable error) {
        if (wip.getAndIncrement() == 0) {
            Throwable ex = error.terminate();
            if (ex != null) {
                subscriber.onError(ex);
            } else {
                subscriber.onComplete();
            }
        }
    }

    public static <T> void onNext(Observer<? super T> subscriber, T value, 
            AtomicInteger wip, AtomicThrowable error) {
        if (wip.get() == 0 && wip.compareAndSet(0, 1)) {
            subscriber.onNext(value);
            if (wip.decrementAndGet() != 0) {
                Throwable ex = error.terminate();
                if (ex != null) {
                    subscriber.onError(ex);
                } else {
                    subscriber.onComplete();
                }
            }
        }
    }
    
    public static void onError(Observer<?> subscriber, Throwable ex, 
            AtomicInteger wip, AtomicThrowable error) {
        if (error.addThrowable(ex)) {
            if (wip.getAndIncrement() == 0) {
                subscriber.onError(error.terminate());
            }
        } else {
            RxJavaPlugins.onError(ex);
        }
    }
    
    public static void onComplete(Observer<?> subscriber, AtomicInteger wip, AtomicThrowable error) {
        if (wip.getAndIncrement() == 0) {
            Throwable ex = error.terminate();
            if (ex != null) {
                subscriber.onError(ex);
            } else {
                subscriber.onComplete();
            }
        }
    }

}
