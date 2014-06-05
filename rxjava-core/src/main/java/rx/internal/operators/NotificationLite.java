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
package rx.internal.operators;

import java.io.Serializable;

import rx.Notification.Kind;
import rx.Observer;

/**
 * For use in internal operators that need something like materialize and dematerialize wholly within the
 * implementation of the operator but don't want to incur the allocation cost of actually creating
 * {@link rx.Notification} objects for every {@code onNext} and {@code onComplete}.
 * 
 * An object is allocated inside {@link #error(Throwable)} to wrap the {@link Throwable} but this shouldn't
 * affect performance because exceptions should be exceptionally rare.
 * 
 * It's implemented as a singleton to maintain some semblance of type safety that is completely non-existent.
 * 
 * @param <T>
 * @warn type param <T> undescribed
 */
public final class NotificationLite<T> {
    private NotificationLite() {
    }

    @SuppressWarnings("rawtypes")
    private static final NotificationLite INSTANCE = new NotificationLite();

    /**
     * @warn instance() undocumented
     */
    @SuppressWarnings("unchecked")
    public static <T> NotificationLite<T> instance() {
        return INSTANCE;
    }

    private static final Object ON_COMPLETED_SENTINEL = new Serializable() {
        private static final long serialVersionUID = 1;
    };

    private static final Object ON_NEXT_NULL_SENTINEL = new Serializable() {
        private static final long serialVersionUID = 2;
    };

    private static class OnErrorSentinel implements Serializable {
        private static final long serialVersionUID = 3;
        private final Throwable e;

        public OnErrorSentinel(Throwable e) {
            this.e = e;
        }
    }

    /**
     * Creates a lite {@code onNext} notification for the value passed in without doing any allocation. Can
     * be unwrapped and sent with the {@link #accept} method.
     * 
     * @param t
     * @warn parameter "t" undescribed
     * @return the value or a null token
     */
    public Object next(T t) {
        if (t == null)
            return ON_NEXT_NULL_SENTINEL;
        else
            return t;
    }

    /**
     * Creates a lite {@code onComplete} notification without doing any allocation. Can be unwrapped and
     * sent with the {@link #accept} method.
     * 
     * @return the completion token
     */
    public Object completed() {
        return ON_COMPLETED_SENTINEL;
    }

    /**
     * Create a lite {@code onError} notification. This call does new up an object to wrap the {@link Throwable}
     * but since there should only be one of these the performance impact should be small. Can be unwrapped and
     * sent with the {@link #accept} method.
     * 
     * @warn description doesn't parse in English ("This call does new up an object...")
     * @param e
     * @warn parameter "e" undescribed
     * @return an object encapsulating the exception
     */
    public Object error(Throwable e) {
        return new OnErrorSentinel(e);
    }

    /**
     * Unwraps the lite notification and calls the appropriate method on the {@link Observer}.
     * 
     * @param o
     *            the {@link Observer} to call onNext, onCompleted, or onError.
     * @warn parameter "n" undescribed
     * @param n
     * @return true if {@code n} was a termination event
     * @throws IllegalArgumentException
     *             if the notification is null.
     * @throws NullPointerException
     *             if the {@link Observer} is null.
     */
    @SuppressWarnings("unchecked")
    public boolean accept(Observer<? super T> o, Object n) {
        if (n == ON_COMPLETED_SENTINEL) {
            o.onCompleted();
            return true;
        } else if (n == ON_NEXT_NULL_SENTINEL) {
            o.onNext(null);
            return false;
        } else if (n != null) {
            if (n.getClass() == OnErrorSentinel.class) {
                o.onError(((OnErrorSentinel) n).e);
                return true;
            }
            o.onNext((T) n);
            return false;
        } else {
            throw new IllegalArgumentException("The lite notification can not be null");
        }
    }

    /**
     * @warn isCompleted() undocumented
     */
    public boolean isCompleted(Object n) {
        return n == ON_COMPLETED_SENTINEL;
    }

    /**
     * @warn isError() undocumented
     */
    public boolean isError(Object n) {
        return n instanceof OnErrorSentinel;
    }

    /**
     * If there is custom logic that isn't as simple as call the right method on an {@link Observer} then this
     * method can be used to get the {@link rx.Notification.Kind}.
     * 
     * @param n
     * @warn parameter "n" undescribed
     * @throws IllegalArgumentException
     *             if the notification is null.
     * @return the kind of the raw object
     */
    public Kind kind(Object n) {
        if (n == null)
            throw new IllegalArgumentException("The lite notification can not be null");
        else if (n == ON_COMPLETED_SENTINEL)
            return Kind.OnCompleted;
        else if (n instanceof OnErrorSentinel)
            return Kind.OnError;
        else
            // value or ON_NEXT_NULL_SENTINEL but either way it's an OnNext
            return Kind.OnNext;
    }

    /**
     * Returns the value passed in {@link #next(Object)} method call. Bad things happen if you call this
     * the {@code onComplete} or {@code onError} notification type. For performance you are expected to use this
     * when it is appropriate.
     * 
     * @param n
     * @warn parameter "n" undescribed
     * @return the unwrapped value, which can be null
     */
    @SuppressWarnings("unchecked")
    public T getValue(Object n) {
        return n == ON_NEXT_NULL_SENTINEL ? null : (T) n;
    }

    /**
     * Returns the {@link Throwable} passed to the {@link #error(Throwable)} method call. Bad things happen if
     * you call this on the {@code onComplete} or {@code onNext} notification type. For performance you are
     * expected to use this when it is appropriate.
     * 
     * @param n
     * @warn parameter "n" undescribed
     * @return the {@link Throwable} wrapped inside n
     */
    public Throwable getError(Object n) {
        return ((OnErrorSentinel) n).e;
    }
}
