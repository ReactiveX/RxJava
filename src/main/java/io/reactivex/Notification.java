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

package io.reactivex;

import io.reactivex.annotations.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.util.NotificationLite;

/**
 * Represents the reactive signal types: onNext, onError and onComplete and
 * holds their parameter values (a value, a Throwable, nothing).
 * @param <T> the value type
 */
public final class Notification<T> {

    final Object value;

    /** Not meant to be implemented externally. */
    private Notification(Object value) {
        this.value = value;
    }

    /**
     * Returns true if this notification is an onComplete signal.
     * @return true if this notification is an onComplete signal
     */
    public boolean isOnComplete() {
        return value == null;
    }

    /**
     * Returns true if this notification is an onError signal and
     * {@link #getError()} returns the contained Throwable.
     * @return true if this notification is an onError signal
     * @see #getError()
     */
    public boolean isOnError() {
        return NotificationLite.isError(value);
    }

    /**
     * Returns true if this notification is an onNext signal and
     * {@link #getValue()} returns the contained value.
     * @return true if this notification is an onNext signal
     * @see #getValue()
     */
    public boolean isOnNext() {
        Object o = value;
        return o != null && !NotificationLite.isError(o);
    }

    /**
     * Returns the contained value if this notification is an onNext
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
     * Returns the container Throwable error if this notification is an onError
     * signal, null otherwise.
     * @return the Throwable error contained or null
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
            return ObjectHelper.equals(value, n.value);
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
     * @param value the value to carry around in the notification, not null
     * @return the new Notification instance
     * @throws NullPointerException if value is null
     */
    @NonNull
    public static <T> Notification<T> createOnNext(@NonNull T value) {
        ObjectHelper.requireNonNull(value, "value is null");
        return new Notification<T>(value);
    }

    /**
     * Constructs an onError notification containing the error.
     * @param <T> the value type
     * @param error the error Throwable to carry around in the notification, not null
     * @return the new Notification instance
     * @throws NullPointerException if error is null
     */
    @NonNull
    public static <T> Notification<T> createOnError(@NonNull Throwable error) {
        ObjectHelper.requireNonNull(error, "error is null");
        return new Notification<T>(NotificationLite.error(error));
    }

    /**
     * Returns the empty and stateless shared instance of a notification representing
     * an onComplete signal.
     * @param <T> the target value type
     * @return the shared Notification instance representing an onComplete signal
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> Notification<T> createOnComplete() {
        return (Notification<T>)COMPLETE;
    }

    /** The singleton instance for createOnComplete. */
    static final Notification<Object> COMPLETE = new Notification<Object>(null);
}
