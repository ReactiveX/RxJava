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
package rx.operators;

import java.io.Serializable;

import rx.Notification.Kind;
import rx.Observer;

/**
 * For use in internal operators that need something like materialize and dematerialize wholly
 * within the implementation of the operator but don't want to incur the allocation cost of actually
 * creating {@link rx.Notification} objects for every onNext and onComplete.
 * 
 * An object is allocated inside {@link #error(Throwable)} to wrap the {@link Throwable} but this
 * shouldn't effect performance because exceptions should be exceptionally rare.
 * 
 * It's implemented as a singleton to maintain some semblance of type safety that is completely
 * non-existent.
 * 
 * @author gscampbell
 * 
 * @param <T>
 */
public final class NotificationLite<T> {
    private NotificationLite() {
    }

    @SuppressWarnings("rawtypes")
    private static final NotificationLite INSTANCE = new NotificationLite();

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
     * Creates a lite onNext notification for the value passed in without doing any allocation. Can
     * be unwrapped and sent with the {@link #accept} method.
     * 
     * @param t
     * @return the value or a null token
     */
    public Object next(T t) {
        if (t == null)
            return ON_NEXT_NULL_SENTINEL;
        else
            return t;
    }

    /**
     * Creates a lite onComplete notification without doing any allocation. Can be unwrapped and
     * sent with the {@link #accept} method.
     * 
     * @return the completion token
     */
    public Object completed() {
        return ON_COMPLETED_SENTINEL;
    }

    /**
     * Create a lite onError notification. This call does new up an object to wrap the
     * {@link Throwable} but since there should only be one of these the performance impact should
     * be small. Can be unwrapped and sent with the {@link #accept} method.
     * 
     * @param e
     * @return an object encapsulating the exception
     */
    public Object error(Throwable e) {
        return new OnErrorSentinel(e);
    }

    /**
     * Unwraps the lite notification and calls the appropriate method on the {@link Observer}.
     * 
     * @param o
     *            the {@link Observer} to call onNext, onCompleted or onError.
     * @param n
     * @throws IllegalArgumentException
     *             if the notification is null.
     * @throws NullPointerException
     *             if the {@link Observer} is null.
     */
    @SuppressWarnings("unchecked")
    public void accept(Observer<? super T> o, Object n) {
        if (n == ON_COMPLETED_SENTINEL) {
            o.onCompleted();
        } else
        if (n == ON_NEXT_NULL_SENTINEL) {
            o.onNext(null);
        } else
        if (n != null) {
            if (n.getClass() == OnErrorSentinel.class) {
                o.onError(((OnErrorSentinel)n).e);
            } else {
                o.onNext((T)n);
            }
        } else {
            throw new IllegalArgumentException("The lite notification can not be null");
        }
    }

    public boolean isCompleted(Object n) {
        return n == ON_COMPLETED_SENTINEL;
    }

    public boolean isError(Object n) {
        return n instanceof OnErrorSentinel;
    }

    /**
     * If there is custom logic that isn't as simple as call the right method on an {@link Observer}
     * then this method can be used to get the {@link rx.Notification.Kind}
     * 
     * @param n
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
     * returns value passed in {@link #next(Object)} method call. Bad things happen if you call this
     * the onComplete or onError notification type. For performance you are expected to use this
     * when it is appropriate.
     * 
     * @param n
     * @return the unwrapped value, which can be null
     */
    @SuppressWarnings("unchecked")
    public T getValue(Object n) {
        return n == ON_NEXT_NULL_SENTINEL ? null : (T) n;
    }

    /**
     * returns {@link Throwable} passed in {@link #error(Throwable)} method call. Bad things happen
     * if you
     * call this the onComplete or onNext notification type. For performance you are expected to use
     * this when it is appropriate.
     * 
     * @param n
     * @return The {@link Throwable} wrapped inside n
     */
    public Throwable getError(Object n) {
        return ((OnErrorSentinel) n).e;
    }
}
