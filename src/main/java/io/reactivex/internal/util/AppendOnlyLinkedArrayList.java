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

package io.reactivex.internal.util;

import org.reactivestreams.Subscriber;

import io.reactivex.Observer;
import io.reactivex.functions.*;

/**
 * A linked-array-list implementation that only supports appending and consumption.
 *
 * @param <T> the value type
 */
public class AppendOnlyLinkedArrayList<T> {
    final int capacity;
    final Object[] head;
    Object[] tail;
    int offset;

    /**
     * Constructs an empty list with a per-link capacity.
     * @param capacity the capacity of each link
     */
    public AppendOnlyLinkedArrayList(int capacity) {
        this.capacity = capacity;
        this.head = new Object[capacity + 1];
        this.tail = head;
    }

    /**
     * Append a non-null value to the list.
     * <p>Don't add null to the list!
     * @param value the value to append
     */
    public void add(T value) {
        final int c = capacity;
        int o = offset;
        if (o == c) {
            Object[] next = new Object[c + 1];
            tail[c] = next;
            tail = next;
            o = 0;
        }
        tail[o] = value;
        offset = o + 1;
    }

    /**
     * Set a value as the first element of the list.
     * @param value the value to set
     */
    public void setFirst(T value) {
        head[0] = value;
    }

    /**
     * Predicate interface suppressing the exception.
     *
     * @param <T> the value type
     */
    public interface NonThrowingPredicate<T> extends Predicate<T> {
        @Override
        boolean test(T t);
    }

    /**
     * Loops over all elements of the array until a null element is encountered or
     * the given predicate returns true.
     * @param consumer the consumer of values that returns true if the forEach should terminate
     */
    @SuppressWarnings("unchecked")
    public void forEachWhile(NonThrowingPredicate<? super T> consumer) {
        Object[] a = head;
        final int c = capacity;
        while (a != null) {
            for (int i = 0; i < c; i++) {
                Object o = a[i];
                if (o == null) {
                    break;
                }
                if (consumer.test((T)o)) {
                    break;
                }
            }
            a = (Object[])a[c];
        }
    }

    /**
     * Interprets the contents as NotificationLite objects and calls
     * the appropriate Subscriber method.
     * 
     * @param <U> the target type
     * @param subscriber the subscriber to emit the events to
     * @return true if a terminal event has been reached
     */
    public <U> boolean accept(Subscriber<? super U> subscriber) {
        Object[] a = head;
        final int c = capacity;
        while (a != null) {
            for (int i = 0; i < c; i++) {
                Object o = a[i];
                if (o == null) {
                    break;
                }

                if (NotificationLite.acceptFull(o, subscriber)) {
                    return true;
                }
            }
            a = (Object[])a[c];
        }
        return false;
    }


    /**
     * Interprets the contents as NotificationLite objects and calls
     * the appropriate Observer method.
     * 
     * @param <U> the target type
     * @param observer the observer to emit the events to
     * @return true if a terminal event has been reached
     */
    public <U> boolean accept(Observer<? super U> observer) {
        Object[] a = head;
        final int c = capacity;
        while (a != null) {
            for (int i = 0; i < c; i++) {
                Object o = a[i];
                if (o == null) {
                    break;
                }

                if (NotificationLite.acceptFull(o, observer)) {
                    return true;
                }
            }
            a = (Object[])a[c];
        }
        return false;
    }

    /**
     * Loops over all elements of the array until a null element is encountered or
     * the given predicate returns true.
     * @param <S> the extra state type
     * @param state the extra state passed into the consumer
     * @param consumer the consumer of values that returns true if the forEach should terminate
     * @throws Exception if the predicate throws
     */
    @SuppressWarnings("unchecked")
    public <S> void forEachWhile(S state, BiPredicate<? super S, ? super T> consumer) throws Exception {
        Object[] a = head;
        final int c = capacity;
        for (;;) {
            for (int i = 0; i < c; i++) {
                Object o = a[i];
                if (o == null) {
                    return;
                }
                if (consumer.test(state, (T)o)) {
                    return;
                }
            }
            a = (Object[])a[c];
        }
    }
}
