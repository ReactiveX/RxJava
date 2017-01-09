/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
 * onError or onComplete may be called from any threads.
 */
public final class HalfSerializer {
    /** Utility class. */
    private HalfSerializer() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Emits the given value if possible and terminates if there was an onComplete or onError
     * while emitting, drops the value otherwise.
     * @param <T> the value type
     * @param subscriber the target Subscriber to emit to
     * @param value the value to emit
     * @param wip the serialization work-in-progress counter/indicator
     * @param error the holder of Throwables
     */
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

    /**
     * Emits the given exception if possible or adds it to the given error container to
     * be emitted by a concurrent onNext if one is running.
     * Undeliverable exceptions are sent to the RxJavaPlugins.onError.
     * @param subscriber the target Subscriber to emit to
     * @param ex the Throwable to emit
     * @param wip the serialization work-in-progress counter/indicator
     * @param error the holder of Throwables
     */
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


    /**
     * Emits an onComplete signal or an onError signal with the given error or indicates
     * the concurrently running onNext should do that.
     * @param subscriber the target Subscriber to emit to
     * @param wip the serialization work-in-progress counter/indicator
     * @param error the holder of Throwables
     */
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

    /**
     * Emits the given value if possible and terminates if there was an onComplete or onError
     * while emitting, drops the value otherwise.
     * @param <T> the value type
     * @param observer the target Observer to emit to
     * @param value the value to emit
     * @param wip the serialization work-in-progress counter/indicator
     * @param error the holder of Throwables
     */
    public static <T> void onNext(Observer<? super T> observer, T value,
            AtomicInteger wip, AtomicThrowable error) {
        if (wip.get() == 0 && wip.compareAndSet(0, 1)) {
            observer.onNext(value);
            if (wip.decrementAndGet() != 0) {
                Throwable ex = error.terminate();
                if (ex != null) {
                    observer.onError(ex);
                } else {
                    observer.onComplete();
                }
            }
        }
    }

    /**
     * Emits the given exception if possible or adds it to the given error container to
     * be emitted by a concurrent onNext if one is running.
     * Undeliverable exceptions are sent to the RxJavaPlugins.onError.
     * @param observer the target Subscriber to emit to
     * @param ex the Throwable to emit
     * @param wip the serialization work-in-progress counter/indicator
     * @param error the holder of Throwables
     */
    public static void onError(Observer<?> observer, Throwable ex,
            AtomicInteger wip, AtomicThrowable error) {
        if (error.addThrowable(ex)) {
            if (wip.getAndIncrement() == 0) {
                observer.onError(error.terminate());
            }
        } else {
            RxJavaPlugins.onError(ex);
        }
    }

    /**
     * Emits an onComplete signal or an onError signal with the given error or indicates
     * the concurrently running onNext should do that.
     * @param observer the target Subscriber to emit to
     * @param wip the serialization work-in-progress counter/indicator
     * @param error the holder of Throwables
     */
    public static void onComplete(Observer<?> observer, AtomicInteger wip, AtomicThrowable error) {
        if (wip.getAndIncrement() == 0) {
            Throwable ex = error.terminate();
            if (ex != null) {
                observer.onError(ex);
            } else {
                observer.onComplete();
            }
        }
    }

}
