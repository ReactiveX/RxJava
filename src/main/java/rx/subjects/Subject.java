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

import rx.*;

/**
 * Represents an object that is both an Observable and an Observer.
 * @param <T> the input value type
 * @param <R> the output value type
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
     * lead to non-serialized calls, which violates
     * <a href="http://reactivex.io/documentation/contract.html">the Observable contract</a> and creates an
     * ambiguity in the resulting Subject.
     * <p>
     * To protect a {@code Subject} from this danger, you can convert it into a {@code SerializedSubject} with
     * code like the following:
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
}
