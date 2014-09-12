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
package rx.internal.operators;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.observers.SerializedSubscriber;

/**
 * Returns an Observable that emits the items from the source Observable until another Observable
 * emits an item.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/takeUntil.png" alt="">
 */
public final class OperatorTakeUntil<T, E> implements Operator<T, T> {

    private final Observable<? extends E> other;

    public OperatorTakeUntil(final Observable<? extends E> other) {
        this.other = other;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final Subscriber<T> parent = new SerializedSubscriber<T>(child);

        other.unsafeSubscribe(new Subscriber<E>(child) {

            @Override
            public void onCompleted() {
                parent.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                parent.onError(e);
            }

            @Override
            public void onNext(E t) {
                parent.onCompleted();
            }

        });

        return parent;
    }

}
