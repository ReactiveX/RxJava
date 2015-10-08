/**
 * Copyright 2014 Netflix, Inc.
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
package rx.internal.util;

import rx.Subscription;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.functions.Func5;
import rx.functions.Func6;
import rx.functions.Func7;
import rx.functions.Func8;
import rx.functions.Func9;
import rx.functions.FuncN;

import java.util.concurrent.CountDownLatch;

/**
 * Utility functions for internal use that we don't want part of the public API. 
 */
public final class UtilityFunctions {

    /**
     * Returns a function that always returns {@code true}.
     *
     * @return a {@link Func1} that accepts an Object and returns the Boolean {@code true}
     */
    public static <T> Func1<? super T, Boolean> alwaysTrue() {
        return AlwaysTrue.INSTANCE;
    }

    /**
     * Returns a function that always returns {@code false}.
     *
     * @return a {@link Func1} that accepts an Object and returns the Boolean {@code false}
     */
    public static <T> Func1<? super T, Boolean> alwaysFalse() {
        return AlwaysFalse.INSTANCE;
    }

    /**
     * Returns a function that always returns the Object it is passed.
     *
     * @return a {@link Func1} that accepts an Object and returns the same Object
     */
    public static <T> Func1<T, T> identity() {
        return new Func1<T, T>() {
            @Override
            public T call(T o) {
                return o;
            }
        };
    }

    /**
     * Waits for completion of the Observable/Single and stops the subscription in case of error.
     *
     * @param latch
     *         latch that waits for completion of the Observable/Single.
     * @param subscription
     *         subscription that needs to be unsubscribed in case of error.
     */
    public static void awaitForCompletion(CountDownLatch latch, Subscription subscription) {
        if (latch.getCount() == 0) {
            // Synchronous observable completes before awaiting for it.
            // Skip await so InterruptedException will never be thrown.
            return;
        }
        // block until the subscription completes and then return
        try {
            latch.await();
        } catch (InterruptedException e) {
            subscription.unsubscribe();
            // set the interrupted flag again so callers can still get it
            // for more information see https://github.com/ReactiveX/RxJava/pull/147#issuecomment-13624780
            Thread.currentThread().interrupt();
            // using Runtime so it is not checked
            throw new RuntimeException("Interrupted while waiting for subscription to complete.", e);
        }
    }

    /**
     * Throws the error if it's not {@code null}, or returns the value even if it's {@code null}.
     *
     * @param value
     *         value to return, can be {@code null}.
     * @param error
     *         error to throw, {@code null} means that there is no error.
     * @param <T>
     *         type of the value.
     * @return value if and only if error is {@code null}.
     */
    public static <T> T throwErrorOrReturnValue(T value, Throwable error) {
        if (error != null) {
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            } else {
                throw new RuntimeException(error);
            }
        } else {
            return value;
        }
    }

    private enum AlwaysTrue implements Func1<Object, Boolean> {
        INSTANCE;

        @Override
        public Boolean call(Object o) {
            return true;
        }
    }

    private enum AlwaysFalse implements Func1<Object, Boolean> {
        INSTANCE;

        @Override
        public Boolean call(Object o) {
            return false;
        }
    }

    /**
     * Returns a function that merely returns {@code null}, without side effects.
     *
     * @return a function that returns {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, R> NullFunction<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, R> returnNull() {
        return NULL_FUNCTION;
    }

    @SuppressWarnings("rawtypes")
    private static final NullFunction NULL_FUNCTION = new NullFunction();

    private static final class NullFunction<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, R> implements
            Func0<R>,
            Func1<T0, R>,
            Func2<T0, T1, R>,
            Func3<T0, T1, T2, R>,
            Func4<T0, T1, T2, T3, R>,
            Func5<T0, T1, T2, T3, T4, R>,
            Func6<T0, T1, T2, T3, T4, T5, R>,
            Func7<T0, T1, T2, T3, T4, T5, T6, R>,
            Func8<T0, T1, T2, T3, T4, T5, T6, T7, R>,
            Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, R>,
            FuncN<R> {
        @Override
        public R call() {
            return null;
        }

        @Override
        public R call(T0 t1) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6, T6 t7) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6, T6 t7, T7 t8) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6, T6 t7, T7 t8, T8 t9) {
            return null;
        }

        @Override
        public R call(Object... args) {
            return null;
        }
    }
    
}
