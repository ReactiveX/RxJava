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

package io.reactivex.rxjava3.core;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.internal.util.NotificationLite;
import java.util.Objects;

/**
 * Represents the reactive signal types: {@code onNext}, {@code onError} and {@code onComplete} and
 * holds their parameter values (a value, a {@link Throwable}, nothing).
 * @param <T> the value type
 */
public final class Notification<T> {

    final Object value;

    /** Not meant to be implemented externally. */
    private Notification(@Nullable Object value) {
        this.value = value;
    }

    /**
     * Returns true if this notification is an {@code onComplete} signal.
     * @return true if this notification is an {@code onComplete} signal
     */
    public boolean isOnComplete() {
        return value == null;
    }

    /**
     * Returns true if this notification is an {@code onError} signal and
     * {@link #getError()} returns the contained {@link Throwable}.
     * @return true if this notification is an {@code onError} signal
     * @see #getError()
     */
    public boolean isOnError() {
        return NotificationLite.isError(value);
    }

    /**
     * Returns true if this notification is an {@code onNext} signal and
     * {@link #getValue()} returns the contained value.
     * @return true if this notification is an {@code onNext} signal
     * @see #getValue()
     */
    public boolean isOnNext() {
        Object o = value;
        return o != null && !NotificationLite.isError(o);
    }

    /**
     * Returns the contained value if this notification is an {@code onNext}
     * signal, null otherwise.
     * @return the value contained or null
     * @see #isOnNext()
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T getValue() {
        Object o = value;
        if (o != null && !NotificationLite.isError(o)) {
            return (T)value;
        }
        return null;
    }

    /**
     * Returns the container {@link Throwable} error if this notification is an {@code onError}
     * signal, null otherwise.
     * @return the {@code Throwable} error contained or {@code null}
     * @see #isOnError()
     */
    @Nullable
    public Throwable getError() {
        Object o = value;
        if (NotificationLite.isError(o)) {
            return NotificationLite.getError(o);
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Notification) {
            Notification<?> n = (Notification<?>) obj;
            return Objects.equals(value, n.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        Object o = value;
        return o != null ? o.hashCode() : 0;
    }

    @Override
    public String toString() {
        Object o = value;
        if (o == null) {
            return "OnCompleteNotification";
        }
        if (NotificationLite.isError(o)) {
            return "OnErrorNotification[" + NotificationLite.getError(o) + "]";
        }
        return "OnNextNotification[" + value + "]";
    }

    /**
     * Constructs an onNext notification containing the given value.
     * @param <T> the value type
     * @param value the value to carry around in the notification, not {@code null}
     * @return the new Notification instance
     * @throws NullPointerException if value is {@code null}
     */
    @NonNull
    public static <@NonNull T> Notification<T> createOnNext(T value) {
        Objects.requireNonNull(value, "value is null");
        return new Notification<>(value);
    }

    /**
     * Constructs an onError notification containing the error.
     * @param <T> the value type
     * @param error the error Throwable to carry around in the notification, not null
     * @return the new Notification instance
     * @throws NullPointerException if error is {@code null}
     */
    @NonNull
    public static <T> Notification<T> createOnError(@NonNull Throwable error) {
        Objects.requireNonNull(error, "error is null");
        return new Notification<>(NotificationLite.error(error));
    }

    /**
     * Returns the empty and stateless shared instance of a notification representing
     * an {@code onComplete} signal.
     * @param <T> the target value type
     * @return the shared Notification instance representing an {@code onComplete} signal
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> Notification<T> createOnComplete() {
        return (Notification<T>)COMPLETE;
    }

    /** The singleton instance for createOnComplete. */
    static final Notification<Object> COMPLETE = new Notification<>(null);
}
