/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.*;
import rx.Single.OnSubscribe;
import rx.plugins.RxJavaHooks;

/**
 * Relay the source signals if the other doesn't terminate before.
 *
 * @param <T> the value type
 * @param <U> the other's value type (not relevant)
 */
public final class SingleTakeUntilObservable<T, U> implements Single.OnSubscribe<T> {

    final Single.OnSubscribe<T> source;

    final Observable<? extends U> other;

    public SingleTakeUntilObservable(OnSubscribe<T> source, Observable<? extends U> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    public void call(SingleSubscriber<? super T> t) {
        TakeUntilSourceSubscriber<T, U> parent = new TakeUntilSourceSubscriber<T, U>(t);
        t.add(parent);

        other.subscribe(parent.other);
        source.call(parent);
    }

    static final class TakeUntilSourceSubscriber<T, U> extends SingleSubscriber<T> {

        final SingleSubscriber<? super T> actual;

        final AtomicBoolean once;

        final Subscriber<U> other;

        TakeUntilSourceSubscriber(SingleSubscriber<? super T> actual) {
            this.actual = actual;
            this.once = new AtomicBoolean();
            this.other = new OtherSubscriber();
            add(other);
        }

        @Override
        public void onSuccess(T value) {
            if (once.compareAndSet(false, true)) {
                unsubscribe();

                actual.onSuccess(value);
            }
        }

        @Override
        public void onError(Throwable error) {
            if (once.compareAndSet(false, true)) {
                unsubscribe();
                actual.onError(error);
            } else {
                RxJavaHooks.onError(error);
            }
        }

        final class OtherSubscriber extends Subscriber<U> {
            @Override
            public void onNext(U value) {
                onCompleted();
            }

            @Override
            public void onError(Throwable error) {
                TakeUntilSourceSubscriber.this.onError(error);
            }

            @Override
            public void onCompleted() {
                onError(new CancellationException("Single::takeUntil(Observable) - Stream was canceled before emitting a terminal event."));
            }
        }
    }
}
