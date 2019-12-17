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

package io.reactivex.rxjava3.internal.util;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.exceptions.CompositeException;

/**
 * Terminal atomics for Throwable containers.
 */
public final class ExceptionHelper {

    /** Utility class. */
    private ExceptionHelper() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * If the provided Throwable is an Error this method
     * throws it, otherwise returns a RuntimeException wrapping the error
     * if that error is a checked exception.
     * @param error the error to wrap or throw
     * @return the (wrapped) error
     */
    public static RuntimeException wrapOrThrow(Throwable error) {
        if (error instanceof Error) {
            throw (Error)error;
        }
        if (error instanceof RuntimeException) {
            return (RuntimeException)error;
        }
        return new RuntimeException(error);
    }

    /**
     * A singleton instance of a Throwable indicating a terminal state for exceptions,
     * don't leak this.
     */
    public static final Throwable TERMINATED = new Termination();

    public static <T> boolean addThrowable(AtomicReference<Throwable> field, Throwable exception) {
        for (;;) {
            Throwable current = field.get();

            if (current == TERMINATED) {
                return false;
            }

            Throwable update;
            if (current == null) {
                update = exception;
            } else {
                update = new CompositeException(current, exception);
            }

            if (field.compareAndSet(current, update)) {
                return true;
            }
        }
    }

    public static <T> Throwable terminate(AtomicReference<Throwable> field) {
        Throwable current = field.get();
        if (current != TERMINATED) {
            current = field.getAndSet(TERMINATED);
        }
        return current;
    }

    /**
     * Returns a flattened list of Throwables from tree-like CompositeException chain.
     * @param t the starting throwable
     * @return the list of Throwables flattened in a depth-first manner
     */
    public static List<Throwable> flatten(Throwable t) {
        List<Throwable> list = new ArrayList<Throwable>();
        ArrayDeque<Throwable> deque = new ArrayDeque<Throwable>();
        deque.offer(t);

        while (!deque.isEmpty()) {
            Throwable e = deque.removeFirst();
            if (e instanceof CompositeException) {
                CompositeException ce = (CompositeException) e;
                List<Throwable> exceptions = ce.getExceptions();
                for (int i = exceptions.size() - 1; i >= 0; i--) {
                    deque.offerFirst(exceptions.get(i));
                }
            } else {
                list.add(e);
            }
        }

        return list;
    }

    /**
     * Workaround for Java 6 not supporting throwing a final Throwable from a catch block.
     * @param <E> the generic exception type
     * @param e the Throwable error to return or throw
     * @return the Throwable e if it is a subclass of Exception
     * @throws E the generic exception thrown
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> Exception throwIfThrowable(Throwable e) throws E {
        if (e instanceof Exception) {
            return (Exception)e;
        }
        throw (E)e;
    }

    public static String timeoutMessage(long timeout, TimeUnit unit) {
        return "The source did not signal an event for "
                + timeout
                + " "
                + unit.toString().toLowerCase()
                + " and has been terminated.";
    }

    static final class Termination extends Throwable {

        private static final long serialVersionUID = -4649703670690200604L;

        Termination() {
            super("No further exceptions");
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    /**
     * Composes a String with a null warning message.
     * @param prefix the prefix to add to the message.
     * @return the composed String
     * @since 3.0.0
     */
    public static String nullWarning(String prefix) {
        return prefix + " Null values are generally not allowed in 3.x operators and sources.";
    }

    /**
     * Creates a NullPointerException with a composed message via {@link #nullWarning(String)}.
     * @param prefix the prefix to add to the message.
     * @return the composed String
     * @since 3.0.0
     */
    public static NullPointerException createNullPointerException(String prefix) {
        return new NullPointerException(nullWarning(prefix));
    }

    /**
     * Similar to Objects.requireNonNull but composes the error message via
     * {@link #nullWarning(String)}.
     * @param <T> the value type
     * @param value the value to check
     * @param prefix the prefix to the error message
     * @return the value
     * @throws NullPointerException if value is null
     * @since 3.0.0
     */
    public static <T> T nullCheck(T value, String prefix) {
        if (value == null) {
            throw createNullPointerException(prefix);
        }
        return value;
    }
}
