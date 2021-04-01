/*
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

package io.reactivex.rxjava3.internal.operators.maybe;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Switches to the fallback Maybe if the other Publisher signals a success or completes, or
 * signals TimeoutException if fallback is null.
 * 
 * @param <T> the main value type
 * @param <U> the other value type
 */
public final class MaybeTimeoutPublisher<T, U> extends AbstractMaybeWithUpstream<T, T> {

    final Publisher<U> other;

    final MaybeSource<? extends T> fallback;

    public MaybeTimeoutPublisher(MaybeSource<T> source, Publisher<U> other, MaybeSource<? extends T> fallback) {
        super(source);
        this.other = other;
        this.fallback = fallback;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        TimeoutMainMaybeObserver<T, U> parent = new TimeoutMainMaybeObserver<>(observer, fallback);
        observer.onSubscribe(parent);

        other.subscribe(parent.other);

        source.subscribe(parent);
    }

    static final class TimeoutMainMaybeObserver<T, U>
    extends AtomicReference<Disposable>
    implements MaybeObserver<T>, Disposable {

        private static final long serialVersionUID = -5955289211445418871L;

        final MaybeObserver<? super T> downstream;

        final TimeoutOtherMaybeObserver<T, U> other;

        final MaybeSource<? extends T> fallback;

        final TimeoutFallbackMaybeObserver<T> otherObserver;

        TimeoutMainMaybeObserver(MaybeObserver<? super T> actual, MaybeSource<? extends T> fallback) {
            this.downstream = actual;
            this.other = new TimeoutOtherMaybeObserver<>(this);
            this.fallback = fallback;
            this.otherObserver = fallback != null ? new TimeoutFallbackMaybeObserver<>(actual) : null;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
            SubscriptionHelper.cancel(other);
            TimeoutFallbackMaybeObserver<T> oo = otherObserver;
            if (oo != null) {
                DisposableHelper.dispose(oo);
            }
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
            SubscriptionHelper.cancel(other);
            if (getAndSet(DisposableHelper.DISPOSED) != DisposableHelper.DISPOSED) {
                downstream.onSuccess(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            SubscriptionHelper.cancel(other);
            if (getAndSet(DisposableHelper.DISPOSED) != DisposableHelper.DISPOSED) {
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            SubscriptionHelper.cancel(other);
            if (getAndSet(DisposableHelper.DISPOSED) != DisposableHelper.DISPOSED) {
                downstream.onComplete();
            }
        }

        public void otherError(Throwable e) {
            if (DisposableHelper.dispose(this)) {
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        public void otherComplete() {
            if (DisposableHelper.dispose(this)) {
                if (fallback == null) {
                    downstream.onError(new TimeoutException());
                } else {
                    fallback.subscribe(otherObserver);
                }
            }
        }
    }

    static final class TimeoutOtherMaybeObserver<T, U>
    extends AtomicReference<Subscription>
    implements FlowableSubscriber<Object> {

        private static final long serialVersionUID = 8663801314800248617L;

        final TimeoutMainMaybeObserver<T, U> parent;

        TimeoutOtherMaybeObserver(TimeoutMainMaybeObserver<T, U> parent) {
            this.parent = parent;
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.setOnce(this, s, Long.MAX_VALUE);
        }

        @Override
        public void onNext(Object value) {
            get().cancel();
            parent.otherComplete();
        }

        @Override
        public void onError(Throwable e) {
            parent.otherError(e);
        }

        @Override
        public void onComplete() {
            parent.otherComplete();
        }
    }

    static final class TimeoutFallbackMaybeObserver<T>
    extends AtomicReference<Disposable>
    implements MaybeObserver<T> {

        private static final long serialVersionUID = 8663801314800248617L;

        final MaybeObserver<? super T> downstream;

        TimeoutFallbackMaybeObserver(MaybeObserver<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onSuccess(T value) {
            downstream.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
