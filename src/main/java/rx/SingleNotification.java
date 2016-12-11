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
package rx;

/**
 * An object representing a notification sent to a {@link Single}.
 * @param <T> the actual value type held by the SingleNotification
 */
public final class SingleNotification<T> {

    private final Kind kind;
    private final Throwable throwable;
    private final T value;

    /**
     * Creates and returns a {@code SingleNotification} of variety {@code Kind.OnSuccess}, and assigns it a value.
     *
     * @param <T> the actual value type held by the SingleNotification
     * @param t
     *          the item to assign to the notification as its value
     * @return an {@code OnSuccess} variety of {@code SingleNotification}
     */
    public static <T> SingleNotification<T> createOnSuccess(T t) {
        return new SingleNotification<T>(Kind.OnSuccess, t, null);
    }

    /**
     * Creates and returns a {@code SingleNotification} of variety {@code Kind.OnError}, and assigns it an exception.
     *
     * @param <T> the actual value type held by the SingleNotification
     * @param e
     *          the exception to assign to the notification
     * @return an {@code OnError} variety of {@code SingleNotification}
     */
    public static <T> SingleNotification<T> createOnError(Throwable e) {
        return new SingleNotification<T>(Kind.OnError, null, e);
    }

    private SingleNotification(Kind kind, T value, Throwable e) {
        this.value = value;
        this.throwable = e;
        this.kind = kind;
    }

    /**
     * Retrieves the exception associated with this (onError) notification.
     *
     * @return the Throwable associated with this (onError) notification
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Retrieves the item associated with this (onSuccess) notification.
     *
     * @return the item associated with this (onSuccess) notification
     */
    public T getValue() {
        return value;
    }

    /**
     * Indicates whether this notification has an item associated with it.
     *
     * @return a boolean indicating whether or not this notification has an item associated with it
     */
    public boolean hasValue() {
        return isOnSuccess() && value != null;
    }

    /**
     * Indicates whether this notification has an exception associated with it.
     *
     * @return a boolean indicating whether this notification has an exception associated with it
     */
    public boolean hasThrowable() {
        return isOnError() && throwable != null;
    }

    /**
     * Retrieves the kind of this notification: {@code OnSuccess}, or {@code OnError}
     *
     * @return the kind of the notification: {@code OnSuccess}, or {@code OnError}
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Indicates whether this notification represents an {@code onError} event.
     *
     * @return a boolean indicating whether this notification represents an {@code onError} event
     */
    public boolean isOnError() {
        return getKind() == Kind.OnError;
    }

    /**
     * Indicates whether this notification represents an {@code onSuccess} event.
     *
     * @return a boolean indicating whether this notification represents an {@code onSuccess} event
     */
    public boolean isOnSuccess() {
        return getKind() == Kind.OnSuccess;
    }

    /**
     * Forwards this notification on to a specified {@link SingleSubscriber}.
     * @param singleSubscriber the target singleSubscriber to call onXXX methods on based on the kind of this SingleNotification instance
     */
    public void accept(SingleSubscriber<? super T> singleSubscriber) {
        if (kind == Kind.OnSuccess) {
            singleSubscriber.onSuccess(getValue());
        } else if (kind == Kind.OnError) {
            singleSubscriber.onError(getThrowable());
        }
    }

    /**
     * Specifies the kind of the notification: an element or an error notification.
     */
    public enum Kind {
        OnSuccess, OnError
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(64).append('[').append(super.toString())
            .append(' ').append(getKind());
        if (hasValue()) {
            str.append(' ').append(getValue());
        }
        if (hasThrowable()) {
            str.append(' ').append(getThrowable().getMessage());
        }
        str.append(']');
        return str.toString();
    }

    @Override
    public int hashCode() {
        int hash = getKind().hashCode();
        if (hasValue()) {
            hash = hash * 31 + getValue().hashCode();
        }
        if (hasThrowable()) {
            hash = hash * 31 + getThrowable().hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }
        SingleNotification<?> singleNotification = (SingleNotification<?>) obj;
        return singleNotification.getKind() == getKind() && (value == singleNotification.value || (value != null && value.equals(singleNotification.value))) && (throwable == singleNotification.throwable || (throwable != null && throwable.equals(singleNotification.throwable)));
    }
}