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

import rx.*;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.*;

/**
 * Helper methods and utilities for creating and working with {@link Subscriber} objects.
 */
public final class Subscribers {
    private Subscribers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Returns an inert {@link Subscriber} that does nothing in response to the emissions or notifications 
     * from any {@code Observable} it subscribes to.  Will throw an {@link OnErrorNotImplementedException} if {@link Subscriber#onError onError} 
     * method is called
     *
     * @param <T> the observed value type
     * @return an inert {@code Observer}
     */
    public static <T> Subscriber<T> empty() {
        return from(Observers.empty());
    }

    /**
     * Converts an {@link Observer} into a {@link Subscriber}.
     *
     * @param <T> the observed value type
     * @param o
     *          the {@link Observer} to convert
     * @return a {@link Subscriber} version of {@code o}
     */
    public static <T> Subscriber<T> from(final Observer<? super T> o) {
        return new Subscriber<T>() {

            @Override
            public void onCompleted() {
                o.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onNext(T t) {
                o.onNext(t);
            }

        };
    }

    /**
     * Creates a {@link Subscriber} that receives the emissions of any {@code Observable} it subscribes to via
     * {@link Subscriber#onNext onNext} but ignores {@link Subscriber#onCompleted onCompleted} notifications;
     * it will throw an {@link OnErrorNotImplementedException} if {@link Subscriber#onError onError} is invoked.
     *
     * @param <T> the observed value type
     * @param onNext
     *          a function that handles each item emitted by an {@code Observable}
     * @throws IllegalArgumentException
     *          if {@code onNext} is {@code null}
     * @return a {@code Subscriber} that calls {@code onNext} for each emitted item from the {@code Observable}
     *         the {@code Subscriber} subscribes to
     */
    public static <T> Subscriber<T> create(final Action1<? super T> onNext) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }

        return new Subscriber<T>() {

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
     * Creates an {@link Subscriber} that receives the emissions of any {@code Observable} it subscribes to via
     * {@link Subscriber#onNext onNext} and handles any {@link Subscriber#onError onError} notification but
     * ignores an {@link Subscriber#onCompleted onCompleted} notification.
     * 
     * @param <T> the observed value type
     * @param onNext
     *          a function that handles each item emitted by an {@code Observable}
     * @param onError
     *          a function that handles an error notification if one is sent by an {@code Observable}
     * @throws IllegalArgumentException
     *          if either {@code onNext} or {@code onError} are {@code null}
     * @return an {@code Subscriber} that calls {@code onNext} for each emitted item from the {@code Observable}
     *         the {@code Subscriber} subscribes to, and calls {@code onError} if the {@code Observable}
     *         notifies of an error
     */
    public static <T> Subscriber<T> create(final Action1<? super T> onNext, final Action1<Throwable> onError) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }

        return new Subscriber<T>() {

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
     * Creates an {@link Subscriber} that receives the emissions of any {@code Observable} it subscribes to via
     * {@link Subscriber#onNext onNext} and handles any {@link Subscriber#onError onError} or
     * {@link Subscriber#onCompleted onCompleted} notifications.
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
     * @return an {@code Subscriber} that calls {@code onNext} for each emitted item from the {@code Observable}
     *         the {@code Subscriber} subscribes to, calls {@code onError} if the {@code Observable} notifies
     *         of an error, and calls {@code onComplete} if the {@code Observable} notifies that the observable
     *         sequence is complete
     */
    public static <T> Subscriber<T> create(final Action1<? super T> onNext, final Action1<Throwable> onError, final Action0 onComplete) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }
        if (onComplete == null) {
            throw new IllegalArgumentException("onComplete can not be null");
        }

        return new Subscriber<T>() {

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

    /**
     * Returns a new {@link Subscriber} that passes all events to
     * <code>subscriber</code>, has backpressure controlled by
     * <code>subscriber</code> and uses the subscription list of
     * <code>subscriber</code> when {@link Subscriber#add(rx.Subscription)} is
     * called.
     * 
     * @param <T> the observed value type
     * @param subscriber
     *            the Subscriber to wrap.
     * 
     * @return a new Subscriber that passes all events to
     *         <code>subscriber</code>, has backpressure controlled by
     *         <code>subscriber</code> and uses <code>subscriber</code> to
     *         manage unsubscription.
     * @since 1.1.0
     */
    public static <T> Subscriber<T> wrap(final Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {

            @Override
            public void onCompleted() {
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onNext(T t) {
                subscriber.onNext(t);
            }
            
        };
    }
}
