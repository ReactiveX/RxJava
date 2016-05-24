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
 * An object representing a notification sent to an {@link Observable}.
 * @param <T> the actual value type held by the Notification
 */
public final class Notification<T> {

    private final Kind kind;
    private final Throwable throwable;
    private final T value;

    private static final Notification<Void> ON_COMPLETED = new Notification<Void>(Kind.OnCompleted, null, null);

    /**
     * Creates and returns a {@code Notification} of variety {@code Kind.OnNext}, and assigns it a value.
     *
     * @param <T> the actual value type held by the Notification
     * @param t
     *          the item to assign to the notification as its value
     * @return an {@code OnNext} variety of {@code Notification}
     */
    public static <T> Notification<T> createOnNext(T t) {
        return new Notification<T>(Kind.OnNext, t, null);
    }

    /**
     * Creates and returns a {@code Notification} of variety {@code Kind.OnError}, and assigns it an exception.
     *
     * @param <T> the actual value type held by the Notification
     * @param e
     *          the exception to assign to the notification
     * @return an {@code OnError} variety of {@code Notification}
     */
    public static <T> Notification<T> createOnError(Throwable e) {
        return new Notification<T>(Kind.OnError, null, e);
    }

    /**
     * Creates and returns a {@code Notification} of variety {@code Kind.OnCompleted}.
     *
     * @param <T> the actual value type held by the Notification
     * @return an {@code OnCompleted} variety of {@code Notification}
     */
    @SuppressWarnings("unchecked")
    public static <T> Notification<T> createOnCompleted() {
        return (Notification<T>) ON_COMPLETED;
    }

    /**
     * Creates and returns a {@code Notification} of variety {@code Kind.OnCompleted}.
     *
     * @param <T> the actual value type held by the Notification
     * @param type
     * @return an {@code OnCompleted} variety of {@code Notification}
     */
    @SuppressWarnings("unchecked")
    public static <T> Notification<T> createOnCompleted(Class<T> type) {
        return (Notification<T>) ON_COMPLETED;
    }

    private Notification(Kind kind, T value, Throwable e) {
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
     * Retrieves the item associated with this (onNext) notification.
     * 
     * @return the item associated with this (onNext) notification
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
        return isOnNext() && value != null;
// isn't "null" a valid item?
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
     * Retrieves the kind of this notification: {@code OnNext}, {@code OnError}, or {@code OnCompleted}
     * 
     * @return the kind of the notification: {@code OnNext}, {@code OnError}, or {@code OnCompleted}
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
     * Indicates whether this notification represents an {@code onCompleted} event.
     * 
     * @return a boolean indicating whether this notification represents an {@code onCompleted} event
     */
    public boolean isOnCompleted() {
        return getKind() == Kind.OnCompleted;
    }

    /**
     * Indicates whether this notification represents an {@code onNext} event.
     * 
     * @return a boolean indicating whether this notification represents an {@code onNext} event
     */
    public boolean isOnNext() {
        return getKind() == Kind.OnNext;
    }

    /**
     * Forwards this notification on to a specified {@link Observer}.
     * @param observer the target observer to call onXXX methods on based on the kind of this Notification instance
     */
    public void accept(Observer<? super T> observer) {
        if (isOnNext()) {
            observer.onNext(getValue());
        } else if (isOnCompleted()) {
            observer.onCompleted();
        } else if (isOnError()) {
            observer.onError(getThrowable());
        }
    }

    public enum Kind {
        OnNext, OnError, OnCompleted
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("[").append(super.toString()).append(" ").append(getKind());
        if (hasValue())
            str.append(" ").append(getValue());
        if (hasThrowable())
            str.append(" ").append(getThrowable().getMessage());
        str.append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int hash = getKind().hashCode();
        if (hasValue())
            hash = hash * 31 + getValue().hashCode();
        if (hasThrowable())
            hash = hash * 31 + getThrowable().hashCode();
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

        Notification<?> notification = (Notification<?>) obj;
        if (notification.getKind() != getKind()) {
            return false;
        }

        if (hasValue() && !getValue().equals(notification.getValue())) {
            return false;
        }

        if (hasThrowable() && !getThrowable().equals(notification.getThrowable())) {
            return false;
        }

        if (!hasValue() && !hasThrowable() && notification.hasValue()) {
            return false;
        }

        if (!hasValue() && !hasThrowable() && notification.hasThrowable()) {
            return false;
        }

        return true;
    }
}
