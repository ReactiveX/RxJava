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
package rx.util;

import rx.Notification;
import rx.Observer;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

/**
 * Record of a value including the virtual time it was produced on.
 * <p>
 * You can static import the class to access the convenient
 * onNext, onError and onCompleted factory methods to create timed events.
 * @param <T> the value type
 */
public final class Recorded<T> {
    private final long time;
    private final T value;
    /**
     * Returns the virtual time the record was produced on.
     * @return the virtual time
     */
    public long time() {
        return time;
    }
    /**
     * Returns the recorded value.
     * @return the recorded value
     */
    public T value() {
        return value;
    }
    /**
     * Constructs a Recorded instance with the time and value as contents.
     * @param time the virtual time the record was produced on
     * @param value  the recorded value
     */
    public Recorded(long time, T value) {
        this.time = time;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Recorded<?>) {
            Recorded<?> r = (Recorded<?>)obj;
            
            return time == r.time &&
                    (value == r.value || (value != null && value.equals(r.value)));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int)(((time >>> 32) ^ time) * 31 + (value != null ? value.hashCode() : 0));
    }

    @Override
    public String toString() {
        return value + "@" + time;
    }

    /**
     * Factory method to create an onNext notification at the specified time point.
     * @param <T> the value type
     * @param time the absolute time
     * @param value the value
     * @return a Recorded onNext notification
     */
    public static <T> Recorded<Notification<T>> onNext(long time, T value) {
        return new Recorded<Notification<T>>(time, new Notification<T>(value));
    }
    /**
     * Factory method to create an assert that checks for an OnNext notification record
     * at the given time, using the specified predicate to check the actual value.
     * @param <T> the value type
     * @param time the absolute time
     * @param predicate the predicate function to check the onNext value against an expected value
     * @return a Recorded onNext notification with predicate
     */
    public static <T> Recorded<Notification<T>> onNext(long time, Func1<Object, Boolean> predicate) {
        if (predicate == null) {
            throw new NullPointerException("predicate");
        }
        return new Recorded<Notification<T>>(time, new OnNextPredicate<T>(predicate));
    }
    /**
     * Factory method to create an assert that checks for an OnNext notification record
     * at the given time, using the specified predicate to check the actual value,
     * with the help of a type witness.
     * @param <T> the value type
     * @param time the absolute time
     * @param predicate the predicate function to check the onNext value against an expected value
     * @param typeWitness the type witness to help the compiler infer the type T
     * @return a Recorded onNext notification with predicate
     */
    public static <T> Recorded<Notification<T>> onNext(long time, Func1<Object, Boolean> predicate, T typeWitness) {
        if (predicate == null) {
            throw new NullPointerException("predicate");
        }
        return new Recorded<Notification<T>>(time, new OnNextPredicate<T>(predicate));
    }
    /**
     * Factory method to create an onCompleted notification at the specified time point.
     * @param <T> the value type
     * @param time the absolute time
     * @return a Recorded onCompleted notification
     */
    public static <T> Recorded<Notification<T>> onCompleted(long time) {
        return new Recorded<Notification<T>>(time, Notification.<T>createOnCompleted());
    }
    /**
     * Factory method to create an onCompleted notification at the specified time point,
     * with the help of a type witness.
     * @param <T> the value type
     * @param time the absolute time
     * @param typeWitness the type witness to help the compiler infer the type T
     * @return a Recorded onCompleted notification
     */
    public static <T> Recorded<Notification<T>> onCompleted(long time, T typeWitness) {
        return new Recorded<Notification<T>>(time, Notification.createOnCompleted(typeWitness));
    }
    /**
     * Factory method to create an onError notification at the specified time point.
     * @param <T> the value type
     * @param time the absolute time
     * @param error th exception
     * @return a Recorded onError notification
     */
    public static <T> Recorded<Notification<T>> onError(long time, Throwable error) {
        if (error == null) {
            throw new NullPointerException("error");
        }
        return new Recorded<Notification<T>>(time, new Notification<T>(error));
    }
    /**
     * Factory method to create an assert that checks for an OnError notification record
     * at the given time, using the specified predicate to check the actual Throwable value.
     * @param <T> the value type
     * @param time the absolute time
     * @param predicate the predicate function to check the onCompleted value against an expected value
     * @return  a Recorded onError notification with predicate
     */
    public static <T> Recorded<Notification<T>> onError(long time, Func1<? super Throwable, Boolean> predicate) {
        if (predicate == null) {
            throw new NullPointerException("predicate");
        }
        return new Recorded<Notification<T>>(time, new OnErrorPredicate<T>(predicate));
    }
    /**
     * Factory method to create an onError notification at the specified time point,
     * with the help of a type witness.
     * @param <T> the value type
     * @param time the absolute time
     * @param error th exception
     * @param typeWitness the type witness to help the compiler infer the type T
     * @return a Recorded onError notification
     */
    public static <T> Recorded<Notification<T>> onError(long time, Throwable error, T typeWitness) {
        if (error == null) {
            throw new NullPointerException("error");
        }
        return new Recorded<Notification<T>>(time, new Notification<T>(error));
    }
    /**
     * Factory method to create an assert that checks for an OnError notification record
     * at the given time, using the specified predicate to check the actual Throwable value,
     * with the help of a type witness.
     * @param <T> the value type
     * @param time the absolute time
     * @param predicate the predicate function to check the onCompleted value against an expected value
     * @param typeWitness the type witness to help the compiler infer the type T
     * @return  a Recorded onError notification with predicate
     */
    public static <T> Recorded<Notification<T>> onError(long time, Func1<? super Throwable, Boolean> predicate, T typeWitness) {
        if (predicate == null) {
            throw new NullPointerException("predicate");
        }
        return new Recorded<Notification<T>>(time, new OnErrorPredicate<T>(predicate));
    }
    /** A predicate based onNext notification. */
    private static final class OnNextPredicate<T> extends PredicateNotification<T> {
        final Func1<Object, Boolean> predicate;
        public OnNextPredicate(Func1<Object, Boolean> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Notification<?>) {
                Notification<?> other = (Notification<?>)obj;
                if (other.getKind() == Notification.Kind.OnNext) {
                    return predicate.call(other.getValue());
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException("Not implemented by design.");
        }
        
    }
    /** A predicate based onError notification. */
    private static final class OnErrorPredicate<T> extends PredicateNotification<T> {
        final Func1<? super Throwable, Boolean> predicate;
        public OnErrorPredicate(Func1<? super Throwable, Boolean> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Notification<?>) {
                Notification<?> other = (Notification<?>)obj;
                if (other.getKind() == Notification.Kind.OnError) {
                    return predicate.call(other.getThrowable());
                }
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            throw new UnsupportedOperationException("Not implemented by design.");
        }
    }
    /** Base class for predicated notification tests, deliberately not implemented. */
    private static abstract class PredicateNotification<T> extends Notification<T> {
        @Override
        public void accept(Observer<? super T> observer) {
            throw new UnsupportedOperationException("Not implemented by design.");
        }

        @Override
        public Kind getKind() {
            throw new UnsupportedOperationException("Not implemented by design.");
        }

        @Override
        public Throwable getThrowable() {
            throw new UnsupportedOperationException("Not implemented by design.");
        }

        @Override
        public T getValue() {
            throw new UnsupportedOperationException("Not implemented by design.");
        }

        @Override
        public boolean hasThrowable() {
            throw new UnsupportedOperationException("Not implemented by design.");
        }

        @Override
        public boolean hasValue() {
            throw new UnsupportedOperationException("Not implemented by design.");
        }

        @Override
        public boolean isOnCompleted() {
            throw new UnsupportedOperationException("Not implemented by design.");
        }

        @Override
        public boolean isOnError() {
            throw new UnsupportedOperationException("Not implemented by design.");
        }

        @Override
        public boolean isOnNext() {
            throw new UnsupportedOperationException("Not implemented by design.");
        }

        @Override
        public void accept(Action1<? super T> onNext, Action1<? super Throwable> onError, Action0 onCompleted) {
            throw new UnsupportedOperationException("Not implemented by design.");
        }

        @Override
        public <R> R accept(Func1<? super T, ? extends R> onNext, Func1<? super Throwable, ? extends R> onError, Func0<? extends R> onCompleted) {
            throw new UnsupportedOperationException("Not implemented by design.");
        }
    }
}
