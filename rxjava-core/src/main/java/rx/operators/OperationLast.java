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
package rx.operators;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;

/**
 * Emit an Observable<T> with the last emitted item
 * or onError(new IllegalArgumentException("Sequence contains no elements")) if no elements received.
 */
public class OperationLast {

    /**
     * Accepts a sequence and returns a sequence that is the last emitted item
     * or an error if no items are emitted (empty sequence).
     * 
     * @param sequence
     *            the input sequence.
     * @param <T>
     *            the type of the sequence.
     * @return a sequence containing the last emitted item or that has onError invoked on it if no items
     */
    public static <T> OnSubscribeFunc<T> last(final Observable<? extends T> sequence) {
        return new OnSubscribeFunc<T>() {
            final AtomicReference<T> last = new AtomicReference<T>();
            final AtomicBoolean hasLast = new AtomicBoolean(false);

            @Override
            public Subscription onSubscribe(final Observer<? super T> observer) {
                return sequence.subscribe(new Observer<T>() {

                    @Override
                    public void onCompleted() {
                        /*
                         * We don't need to worry about the following being non-atomic
                         * since an Observable sequence is serial so we will not receive
                         * concurrent executions.
                         */
                        if (hasLast.get()) {
                            observer.onNext(last.get());
                            observer.onCompleted();
                        } else {
                            observer.onError(new IllegalArgumentException("Sequence contains no elements"));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }

                    @Override
                    public void onNext(T value) {
                        last.set(value);
                        hasLast.set(true);
                    }
                });
            }

        };
    }

}
