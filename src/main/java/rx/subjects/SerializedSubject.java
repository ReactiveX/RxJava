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

import rx.Subscriber;
import rx.observers.SerializedObserver;

/**
 * Wraps a {@link Subject} so that it is safe to call its various {@code on} methods from different threads.
 * <p>
 * When you use an ordinary {@link Subject} as a {@link Subscriber}, you must take care not to call its
 * {@link Subscriber#onNext} method (or its other {@code on} methods) from multiple threads, as this could lead
 * to non-serialized calls, which violates <a href="http://reactivex.io/documentation/contract.html">the
 * Observable contract</a> and creates an ambiguity in the resulting Subject.
 * <p>
 * To protect a {@code Subject} from this danger, you can convert it into a {@code SerializedSubject} with code
 * like the following:
 * <p><pre>{@code
 * mySafeSubject = new SerializedSubject( myUnsafeSubject );
 * }</pre>
 * 
 * @param <T> the input value type
 * @param <R> the output value type
 */
public class SerializedSubject<T, R> extends Subject<T, R> {
    private final SerializedObserver<T> observer;
    private final Subject<T, R> actual;

    public SerializedSubject(final Subject<T, R> actual) {
        super(new OnSubscribe<R>() {

            @Override
            public void call(Subscriber<? super R> child) {
                actual.unsafeSubscribe(child);
            }

        });
        this.actual = actual;
        this.observer = new SerializedObserver<T>(actual);
    }

    @Override
    public void onCompleted() {
        observer.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        observer.onError(e);
    }

    @Override
    public void onNext(T t) {
        observer.onNext(t);
    }

    @Override
    public boolean hasObservers() {
        return actual.hasObservers();
    }
}
