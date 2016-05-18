/**
 * Copyright 2014 Netflix, Inc.
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
package rx.observers;

import rx.Observer;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Helper methods and utilities for creating and working with {@link Observer} objects.
 */
public final class Observers {
    private Observers() {
        throw new IllegalStateException("No instances!");
    }

    private static final Observer<Object> EMPTY = new Observer<Object>() {

        @Override
        public final void onCompleted() {
            // do nothing
        }

        @Override
        public final void onError(Throwable e) {
            throw new OnErrorNotImplementedException(e);
        }

        @Override
        public final void onNext(Object args) {
            // do nothing
        }

    };

    /**
     * Returns an inert {@link Observer} that does nothing in response to the emissions or notifications from
     * any {@code Observable} it subscribes to but will throw an exception if its
     * {@link Observer#onError onError} method is called.
     * @param <T> the observed value type
     * @return an inert {@code Observer}
     */
    @SuppressWarnings("unchecked")
    public static <T> Observer<T> empty() {
        return (Observer<T>) EMPTY;
    }

    /**
     * Creates an {@link Observer} that receives the emissions of any {@code Observable} it subscribes to via
     * {@link Observer#onNext onNext} but ignores {@link Observer#onCompleted onCompleted} notifications; 
     * it will throw an {@link OnErrorNotImplementedException} if {@link Observer#onError onError} is invoked.
     *
     * @param <T> the observed value type
     * @param onNext
     *          a function that handles each item emitted by an {@code Observable}
     * @throws IllegalArgumentException
     *          if {@code onNext} is {@code null}
     * @return an {@code Observer} that calls {@code onNext} for each emitted item from the {@code Observable}
     *         the {@code Observer} subscribes to
     */
    public static <T> Observer<T> create(final Action1<? super T> onNext) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }

        return new Observer<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        };
    }

    /**
     * Creates an {@link Observer} that receives the emissions of any {@code Observable} it subscribes to via
     * {@link Observer#onNext onNext} and handles any {@link Observer#onError onError} notification but ignores
     * an {@link Observer#onCompleted onCompleted} notification.
     * 
     * @param <T> the observed value type
     * @param onNext
     *          a function that handles each item emitted by an {@code Observable}
     * @param onError
     *          a function that handles an error notification if one is sent by an {@code Observable}
     * @throws IllegalArgumentException
     *          if either {@code onNext} or {@code onError} are {@code null}
     * @return an {@code Observer} that calls {@code onNext} for each emitted item from the {@code Observable}
     *         the {@code Observer} subscribes to, and calls {@code onError} if the {@code Observable} notifies
     *         of an error
     */
    public static <T> Observer<T> create(final Action1<? super T> onNext, final Action1<Throwable> onError) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }

        return new Observer<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        };
    }

    /**
     * Creates an {@link Observer} that receives the emissions of any {@code Observable} it subscribes to via
     * {@link Observer#onNext onNext} and handles any {@link Observer#onError onError} or
     * {@link Observer#onCompleted onCompleted} notifications.
     * 
     * @param <T> the observed value type
     * @param onNext
     *          a function that handles each item emitted by an {@code Observable}
     * @param onError
     *          a function that handles an error notification if one is sent by an {@code Observable}
     * @param onComplete
     *          a function that handles a sequence complete notification if one is sent by an {@code Observable}
     * @throws IllegalArgumentException
     *          if either {@code onNext}, {@code onError}, or {@code onComplete} are {@code null}
     * @return an {@code Observer} that calls {@code onNext} for each emitted item from the {@code Observable}
     *         the {@code Observer} subscribes to, calls {@code onError} if the {@code Observable} notifies
     *         of an error, and calls {@code onComplete} if the {@code Observable} notifies that the observable
     *         sequence is complete
     */
    public static <T> Observer<T> create(final Action1<? super T> onNext, final Action1<Throwable> onError, final Action0 onComplete) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }
        if (onComplete == null) {
            throw new IllegalArgumentException("onComplete can not be null");
        }

        return new Observer<T>() {

            @Override
            public final void onCompleted() {
                onComplete.call();
            }

            @Override
            public final void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public final void onNext(T args) {
                onNext.call(args);
            }

        };
    }

}
