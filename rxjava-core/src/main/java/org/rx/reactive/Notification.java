package org.rx.reactive;

/**
 * An object representing a notification sent to a Watchable.
 * 
 * For the Microsoft Rx equivalent see: http://msdn.microsoft.com/en-us/library/hh229462(v=vs.103).aspx
 */
public class Notification<T> {

    private final Kind kind;
    private final Exception exception;
    private final T value;

    /**
     * A constructor used to represent an onNext notification.
     * 
     * @param value
     *            The data passed to the onNext method.
     */
    public Notification(T value) {
        this.value = value;
        this.exception = null;
        this.kind = Kind.OnNext;
    }

    /**
     * A constructor used to represent an onError notification.
     * 
     * @param exception
     *            The exception passed to the onError notification.
     */
    public Notification(Exception exception) {
        this.exception = exception;
        this.value = null;
        this.kind = Kind.OnError;
    }

    /**
     * A constructor used to represent an onCompleted notification.
     */
    public Notification() {
        this.exception = null;
        this.value = null;
        this.kind = Kind.OnCompleted;
    }

    /**
     * Retrieves the exception associated with an onError notification.
     * 
     * @return The exception associated with an onError notification.
     */
    public Exception getException() {
        return exception;
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
    public boolean hasException() {
        return isOnError() && exception != null;
    }

    /**
     * The kind of notification: OnNext, OnError, OnCompleted
     * 
     * @return
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

    public static enum Kind {
        OnNext, OnError, OnCompleted
    }
}
