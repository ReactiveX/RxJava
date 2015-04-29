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
package rx.subjects;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.annotations.Experimental;

/**
 * Represents an object that is both an Observable and an Observer.
 */
public abstract class Subject<T, R> extends Observable<R> implements Observer<T> {
    protected Subject(OnSubscribe<R> onSubscribe) {
        super(onSubscribe);
    }

    /**
     * Indicates whether the {@link Subject} has {@link Observer Observers} subscribed to it.
     *
     * @return true if there is at least one Observer subscribed to this Subject, false otherwise
     */
    public abstract boolean hasObservers();
    
    /**
     * Wraps a {@link Subject} so that it is safe to call its various {@code on} methods from different threads.
     * <p>
     * When you use an ordinary {@link Subject} as a {@link Subscriber}, you must take care not to call its 
     * {@link Subscriber#onNext} method (or its other {@code on} methods) from multiple threads, as this could 
     * lead to non-serialized calls, which violates the Observable contract and creates an ambiguity in the resulting Subject.
     * <p>
     * To protect a {@code Subject} from this danger, you can convert it into a {@code SerializedSubject} with code
     * like the following:
     * <p><pre>{@code
     * mySafeSubject = myUnsafeSubject.toSerialized();
     * }</pre>
     * 
     * @return SerializedSubject wrapping the current Subject
     */
    public final SerializedSubject<T, R> toSerialized() {
        if (getClass() == SerializedSubject.class) {
            return (SerializedSubject<T, R>)this;
        }
        return new SerializedSubject<T, R>(this);
    }
    /**
     * Check if the Subject has terminated with an exception.
     * <p>The operation is threadsafe.
     *
     * @return {@code true} if the subject has received a throwable through {@code onError}.
     * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
     */
    @Experimental
    public boolean hasThrowable() {
        throw new UnsupportedOperationException();
    }
    /**
     * Check if the Subject has terminated normally.
     * <p>The operation is threadsafe.
     *
     * @return {@code true} if the subject completed normally via {@code onCompleted}
     * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
     */
    @Experimental
    public boolean hasCompleted() {
        throw new UnsupportedOperationException();
    }
    /**
     * Returns the Throwable that terminated the Subject.
     * <p>The operation is threadsafe.
     *
     * @return the Throwable that terminated the Subject or {@code null} if the subject hasn't terminated yet or
     *         if it terminated normally.
     * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
     */
    @Experimental
    public Throwable getThrowable() {
        throw new UnsupportedOperationException();
    }
    /**
     * Check if the Subject has any value.
     * <p>Use the {@link #getValue()} method to retrieve such a value.
     * <p>Note that unless {@link #hasCompleted()} or {@link #hasThrowable()} returns true, the value
     * retrieved by {@code getValue()} may get outdated.
     * <p>The operation is threadsafe.
     *
     * @return {@code true} if and only if the subject has some value but not an error
     * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
     */
    @Experimental
    public boolean hasValue() {
        throw new UnsupportedOperationException();
    }
    /**
     * Returns the current or latest value of the Subject if there is such a value and
     * the subject hasn't terminated with an exception.
     * <p>The method can return {@code null} for various reasons. Use {@link #hasValue()}, {@link #hasThrowable()}
     * and {@link #hasCompleted()} to determine if such {@code null} is a valid value, there was an
     * exception or the Subject terminated without receiving any value. 
     * <p>The operation is threadsafe.
     *
     * @return the current value or {@code null} if the Subject doesn't have a value, has terminated with an
     *         exception or has an actual {@code null} as a value.
     * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
     */
    @Experimental
    public T getValue() {
        throw new UnsupportedOperationException();
    }
    /** An empty array to trigger getValues() to return a new array. */
    private static final Object[] EMPTY_ARRAY = new Object[0];
    /**
     * Returns a snapshot of the currently buffered non-terminal events.
     * <p>The operation is threadsafe.
     *
     * @return a snapshot of the currently buffered non-terminal events.
     * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
     */
    @SuppressWarnings("unchecked")
    @Experimental
    public Object[] getValues() {
        T[] r = getValues((T[])EMPTY_ARRAY);
        if (r == EMPTY_ARRAY) {
            return new Object[0]; // don't leak the default empty array.
        }
        return r;
    }
    /**
     * Returns a snapshot of the currently buffered non-terminal events into 
     * the provided {@code a} array or creates a new array if it has not enough capacity.
     * <p>If the subject's values fit in the specified array with room to spare
     * (i.e., the array has more elements than the list), the element in
     * the array immediately following the end of the subject's values is set to
     * {@code null}.
     * <p>The operation is threadsafe.
     *
     * @param a the array to fill in
     * @return the array {@code a} if it had enough capacity or a new array containing the available values 
     * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
     */
    @Experimental
    public T[] getValues(T[] a) {
        throw new UnsupportedOperationException();
    }
}
