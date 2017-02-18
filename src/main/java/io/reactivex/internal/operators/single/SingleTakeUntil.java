/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.single;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Signals the events of the source Single or signals a CancellationException if the
 * other Publisher signalled first.
 * @param <T> the main value type
 * @param <U> the other value type
 */
public final class SingleTakeUntil<T, U> extends Single<T> {

    final SingleSource<T> source;

    final Publisher<U> other;

    public SingleTakeUntil(SingleSource<T> source, Publisher<U> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> observer) {
        TakeUntilMainObserver<T> parent = new TakeUntilMainObserver<T>(observer);
        observer.onSubscribe(parent);

        other.subscribe(parent.other);

        source.subscribe(parent);
    }

    static final class TakeUntilMainObserver<T>
    extends AtomicReference<Disposable>
    implements SingleObserver<T>, Disposable {

        private static final long serialVersionUID = -622603812305745221L;

        final SingleObserver<? super T> actual;

        final TakeUntilOtherSubscriber other;

        TakeUntilMainObserver(SingleObserver<? super T> actual) {
            this.actual = actual;
            this.other = new TakeUntilOtherSubscriber(this);
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onSuccess(T value) {
            other.dispose();

            Disposable a = get();
            if (a != DisposableHelper.DISPOSED) {
                a = getAndSet(DisposableHelper.DISPOSED);
                if (a != DisposableHelper.DISPOSED) {
                    actual.onSuccess(value);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            other.dispose();

            Disposable a = get();
            if (a != DisposableHelper.DISPOSED) {
                a = getAndSet(DisposableHelper.DISPOSED);
                if (a != DisposableHelper.DISPOSED) {
                    actual.onError(e);
                    return;
                }
            }
            RxJavaPlugins.onError(e);
        }

        void otherError(Throwable e) {
            Disposable a = get();
            if (a != DisposableHelper.DISPOSED) {
                a = getAndSet(DisposableHelper.DISPOSED);
                if (a != DisposableHelper.DISPOSED) {
                    if (a != null) {
                        a.dispose();
                    }
                    actual.onError(e);
                    return;
                }
            }
            RxJavaPlugins.onError(e);
        }
    }

    static final class TakeUntilOtherSubscriber
    extends AtomicReference<Subscription>
    implements FlowableSubscriber<Object> {

        private static final long serialVersionUID = 5170026210238877381L;

        final TakeUntilMainObserver<?> parent;

        TakeUntilOtherSubscriber(TakeUntilMainObserver<?> parent) {
            this.parent = parent;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(Object t) {
            if (SubscriptionHelper.cancel(this)) {
                parent.otherError(new CancellationException());
            }
        }

        @Override
        public void onError(Throwable t) {
            parent.otherError(t);
        }

        @Override
        public void onComplete() {
            if (get() != SubscriptionHelper.CANCELLED) {
                lazySet(SubscriptionHelper.CANCELLED);
                parent.otherError(new CancellationException());
            }
        }

        public void dispose() {
            SubscriptionHelper.cancel(this);
        }
    }
}
