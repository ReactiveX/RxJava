/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.subjects;

import io.reactivex.*;

/**
 * Represents a NbpSubscriber and a NbpObservable at the same time, allowing
 * multicasting events from a single source to multiple child Subscribers.
 * <p>All methods except the onSubscribe, onNext, onError and onComplete are thread-safe.
 * Use {@link #toSerialized()} to make these methods thread-safe as well.
 *
 * @param <T> the source value type
 * @param <R> the emission value type
 */
public abstract class Subject<T, R> extends Observable<R> implements Observer<T> {
    
    protected Subject(NbpOnSubscribe<R> onSubscribe) {
        super(onSubscribe);
    }
    
    /**
     * Returns true if the subject has subscribers.
     * <p>The method is thread-safe.
     * @return true if the subject has subscribers
     */
    public abstract boolean hasSubscribers();
    
    /**
     * Returns true if the subject has reached a terminal state through an error event.
     * <p>The method is thread-safe.
     * @return true if the subject has reached a terminal state through an error event
     * @see #getThrowable()
     * &see {@link #hasComplete()}
     */
    public boolean hasThrowable() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns true if the subject has reached a terminal state through a complete event.
     * <p>The method is thread-safe.
     * @return true if the subject has reached a terminal state through a complete event
     * @see #hasThrowable()
     */
    public boolean hasComplete() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns true if the subject has any value.
     * <p>The method is thread-safe.
     * @return true if the subject has any value
     */
    public boolean hasValue() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns the error that caused the Subject to terminate or null if the Subject
     * hasn't terminated yet.
     * <p>The method is thread-safe.
     * @return the error that caused the Subject to terminate or null if the Subject
     * hasn't terminated yet
     */
    public Throwable getThrowable() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns a single value the Subject currently has or null if no such value exists.
     * <p>The method is thread-safe.
     * @return a single value the Subject currently has or null if no such value exists
     */
    public R getValue() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Wraps this Subject and serializes the calls to the onSubscribe, onNext, onError and
     * onComplete methods, making them thread-safe.
     * <p>The method is thread-safe.
     * @return the wrapped and serialized subject
     */
    public final Subject<T, R> toSerialized() {
        if (this instanceof SerializedSubject) {
            return this;
        }
        return new SerializedSubject<T, R>(this);
    }
    
    /** An empty array to avoid allocation in getValues(). */
    private static final Object[] EMPTY = new Object[0];
    
    /**
     * Returns an Object array containing snapshot all values of the Subject.
     * <p>The method is thread-safe.
     * @return the array containing the snapshot of all values of the Subject
     */
    public Object[] getValues() {
        @SuppressWarnings("unchecked")
        R[] a = (R[])EMPTY;
        R[] b = getValues(a);
        if (b == EMPTY) {
            return new Object[0];
        }
        return b;
            
    }
    
    /**
     * Returns a typed array containing a snapshot of all values of the Subject.
     * <p>The method follows the conventions of Collection.toArray by setting the array element
     * after the last value to null (if the capacity permits).
     * <p>The method is thread-safe.
     * @param array the target array to copy values into if it fits
     * @return the given array if the values fit into it or a new array containing all values
     */
    public R[] getValues(R[] array) {
        throw new UnsupportedOperationException();
    }
}
