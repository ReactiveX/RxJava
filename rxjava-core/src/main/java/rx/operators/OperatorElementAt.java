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

import rx.Observable.Operator;
import rx.Subscriber;

/**
 * Returns the element at a specified index in a sequence.
 */
public final class OperatorElementAt<T> implements Operator<T, T> {

    private final int index;
    private final boolean hasDefault;
    private final T defaultValue;

    public OperatorElementAt(int index) {
        this(index, null, false);
    }

    public OperatorElementAt(int index, T defaultValue) {
        this(index, defaultValue, true);
    }

    private OperatorElementAt(int index, T defaultValue, boolean hasDefault) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(index + " is out of bounds");
        }
        this.index = index;
        this.defaultValue = defaultValue;
        this.hasDefault = hasDefault;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {

            private int currentIndex = 0;

            @Override
            public void onNext(T value) {
                if (currentIndex == index) {
                    subscriber.onNext(value);
                    subscriber.onCompleted();
                }
                currentIndex++;
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onCompleted() {
                if (currentIndex <= index) {
                    // If "subscriber.onNext(value)" is called, currentIndex must be greater than index
                    if (hasDefault) {
                        subscriber.onNext(defaultValue);
                        subscriber.onCompleted();
                    } else {
                        subscriber.onError(new IndexOutOfBoundsException(index + " is out of bounds"));
                    }
                }
            }
        };
    }

}
