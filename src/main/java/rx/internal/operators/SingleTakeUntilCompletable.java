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
 */
public final class SingleTakeUntilCompletable<T> implements Single.OnSubscribe<T> {

    final Single.OnSubscribe<T> source;

    final Completable other;

    public SingleTakeUntilCompletable(OnSubscribe<T> source, Completable other) {
        this.source = source;
        this.other = other;
    }

    @Override
    public void call(SingleSubscriber<? super T> t) {
        TakeUntilSourceSubscriber<T> parent = new TakeUntilSourceSubscriber<T>(t);
        t.add(parent);

        other.subscribe(parent);
        source.call(parent);
    }

    static final class TakeUntilSourceSubscriber<T> extends SingleSubscriber<T>
    implements CompletableSubscriber {

        final SingleSubscriber<? super T> actual;

        final AtomicBoolean once;

        TakeUntilSourceSubscriber(SingleSubscriber<? super T> actual) {
            this.actual = actual;
            this.once = new AtomicBoolean();
        }

        @Override
        public void onSubscribe(Subscription d) {
            add(d);
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

        @Override
        public void onCompleted() {
            onError(new CancellationException("Single::takeUntil(Completable) - Stream was canceled before emitting a terminal event."));
        }
    }
}
