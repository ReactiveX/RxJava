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

import java.util.Deque;
import java.util.LinkedList;

import rx.Observable.Operator;
import rx.Subscriber;

/**
 * Bypasses a specified number of elements at the end of an observable sequence.
 */
public class OperatorSkipLast<T> implements Operator<T, T> {

    private final int count;

    public OperatorSkipLast(int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("count could not be negative");
        }
        this.count = count;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {
            /**
             * Store the last count elements until now.
             */
            private final Deque<T> deque = new LinkedList<T>();

            @Override
            public void onCompleted() {
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onNext(T value) {
                if (count == 0) {
                    // If count == 0, we do not need to put value into deque
                    // and remove it at once. We can emit the value
                    // directly.
                    subscriber.onNext(value);
                    return;
                }
                deque.offerLast(value);
                if (deque.size() > count) {
                    // Now deque has count + 1 elements, so the first
                    // element in the deque definitely does not belong
                    // to the last count elements of the source
                    // sequence. We can emit it now.
                    subscriber.onNext(deque.removeFirst());
                }
            }

        };
    }

}
