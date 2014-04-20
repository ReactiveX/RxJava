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

import java.util.NoSuchElementException;

import rx.Observable.Operator;
import rx.Subscriber;

/**
 * If the Observable completes after emitting a single item that matches a
 * predicate, return an Observable containing that item. If it emits more than
 * one such item or no item, throw an IllegalArgumentException.
 */
public final class OperatorSingle<T> implements Operator<T, T> {

    private final boolean hasDefaultValue;
    private final T defaultValue;

    public OperatorSingle() {
        this(false, null);
    }

    public OperatorSingle(T defaultValue) {
        this(true, defaultValue);
    }

    private OperatorSingle(boolean hasDefaultValue, final T defaultValue) {
        this.hasDefaultValue = hasDefaultValue;
        this.defaultValue = defaultValue;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {

            private T value;
            private boolean isNonEmpty = false;
            private boolean hasTooManyElements = false;

            @Override
            public void onNext(T value) {
                if (isNonEmpty) {
                    hasTooManyElements = true;
                    subscriber.onError(new IllegalArgumentException("Sequence contains too many elements"));
                } else {
                    this.value = value;
                    isNonEmpty = true;
                }
            }

            @Override
            public void onCompleted() {
                if (hasTooManyElements) {
                    // We have already sent an onError message
                } else {
                    if (isNonEmpty) {
                        subscriber.onNext(value);
                        subscriber.onCompleted();
                    } else {
                        if (hasDefaultValue) {
                            subscriber.onNext(defaultValue);
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new NoSuchElementException("Sequence contains no elements"));
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

        };
    }

}
