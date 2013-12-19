/**
 * Copyright 2013 Netflix, Inc.
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

import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

/**
 * An object representing a notification sent to an {@link Observable}.
 * 
 * For the Microsoft Rx equivalent see: http://msdn.microsoft.com/en-us/library/hh229462(v=vs.103).aspx
 */
public class Notification<T> {

    private final Kind kind;
    private final Throwable throwable;
    private final T value;

    /**
     * A constructor used to represent an onNext notification.
     * 
     * @param value
     *            The data passed to the onNext method.
     */
    public Notification(T value) {
        this.value = value;
        this.throwable = null;
        this.kind = Kind.OnNext;
    }

    /**
     * A constructor used to represent an onError notification.
     * 
     * @param exception
     *            The exception passed to the onError notification.
     */
    public Notification(Throwable exception) {
        this.throwable = exception;
        this.value = null;
        this.kind = Kind.OnError;
    }

    /**
     * A constructor used to represent an onCompleted notification.
     * @deprecated use the static #createOnCompleted() method since an
     *             onCompleted notification doesn't hold any state so
     *             there is no need to have more than one instance of it.
     */
    public Notification() {
        this.throwable = null;
        this.value = null;
        this.kind = Kind.OnCompleted;
    }
    /** A single instanceof a completed notification. */
    private static final Notification<Object> ON_COMPLETED_NOTIFICATION;
    static {
        ON_COMPLETED_NOTIFICATION = new Notification<Object>();
    }
    
    /**
     * Return an Notificication representing an onCompleted event.
     * @param <T> the value type of the notification
     * @return an Notificication representing an onCompleted event
     */
    @SuppressWarnings("unchecked")
    public static <T> Notification<T> createOnCompleted() {
        return (Notification<T>)ON_COMPLETED_NOTIFICATION;
    }
    
    /**
     * Return an Notificication representing an onCompleted event
     * with the help of a type witness.
     * @param <T> the value type of the notification
     * @param typeWitness the object to help the compiler determine the return type
     * @return an Notificication representing an onCompleted event
     */
    @SuppressWarnings("unchecked")
    public static <T> Notification<T> createOnCompleted(T typeWitness) {
        return (Notification<T>)ON_COMPLETED_NOTIFICATION;
    }
    
    /**
     * Retrieves the exception associated with an onError notification.
     * 
     * @return Throwable associated with an onError notification.
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Retrieves the data associated with an onNext notification.
     * 
     * @return The data associated with an onNext notification.
     */
    public T getValue() {
        return value;
    }

    /**
     * Retrieves a value indicating whether this notification has a value.
     * 
     * @return a value indicating whether this notification has a value.
     */
    public boolean hasValue() {
        return isOnNext() && value != null;
    }

    /**
     * Retrieves a value indicating whether this notification has an exception.
     * 
     * @return a value indicating whether this notification has an exception.
     */
    public boolean hasThrowable() {
        return isOnError() && throwable != null;
    }

    /**
     * Retrieves the kind of the notification: OnNext, OnError, OnCompleted
     * 
     * @return the kind of the notification: OnNext, OnError, OnCompleted
     */
    public Kind getKind() {
        return kind;
    }

    public boolean isOnError() {
        return getKind() == Kind.OnError;
    }

    public boolean isOnCompleted() {
        return getKind() == Kind.OnCompleted;
    }

    public boolean isOnNext() {
        return getKind() == Kind.OnNext;
    }

    public void accept(Observer<? super T> observer) {
        if (isOnNext()) {
            observer.onNext(getValue());
        } else if (isOnCompleted()) {
            observer.onCompleted();
        } else if (isOnError()) {
            observer.onError(getThrowable());
        }
    }

    /**
     * Call the appropriate action based on the type of this notification.
     * @param onNext the action to call if this notification is of kind OnNext.
     * @param onError the action to call if this notification is of kind OnError.
     * @param onCompleted the action to call if this notification is of kind OnCompleted
     */
    public void accept(Action1<? super T> onNext, Action1<? super Throwable> onError, Action0 onCompleted) {
        if (isOnNext()) {
            onNext.call(getValue());
        } else
        if (isOnCompleted()) {
            onCompleted.call();
        } else
        if (isOnError()) {
            onError.call(getThrowable());
        }
        throw new IllegalStateException("This Notification is neither onNext, onError nor onCompleted?!");
    }
    /**
     * Call the appropriate function based on the type of this notification and
     * return the function's result.
     * @param <R> the result type of the functions
     * @param onNext the function to call if this notification is of kind OnNext.
     * @param onError the function to call if this notification is of kind OnError.
     * @param onCompleted the function to call if this notification is of kind OnCompleted
     * @return the result of the function call
     */
    public <R> R accept(Func1<? super T, ? extends R> onNext, Func1<? super Throwable, ? extends R> onError, Func0<? extends R> onCompleted) {
        if (isOnNext()) {
            return onNext.call(getValue());
        } else
        if (isOnCompleted()) {
            return onCompleted.call();
        } else
        if (isOnError()) {
            return onError.call(getThrowable());
        }
        throw new IllegalStateException("This Notification is neither onNext, onError nor onCompleted?!");
    }
    
    public static enum Kind {
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
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (obj.getClass() != getClass())
            return false;
        Notification<?> notification = (Notification<?>) obj;
        if (notification.getKind() != getKind())
            return false;
        if (hasValue() && !getValue().equals(notification.getValue()))
            return false;
        if (hasThrowable() && !getThrowable().equals(notification.getThrowable()))
            return false;
        return true;
    }
}
