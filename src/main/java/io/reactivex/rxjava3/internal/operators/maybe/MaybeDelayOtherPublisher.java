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

package io.reactivex.rxjava3.internal.operators.maybe;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Delay the emission of the main signal until the other signals an item or completes.
 * 
 * @param <T> the main value type
 * @param <U> the other value type
 */
public final class MaybeDelayOtherPublisher<T, U> extends AbstractMaybeWithUpstream<T, T> {

    final Publisher<U> other;

    public MaybeDelayOtherPublisher(MaybeSource<T> source, Publisher<U> other) {
        super(source);
        this.other = other;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new DelayMaybeObserver<T, U>(observer, other));
    }

    static final class DelayMaybeObserver<T, U>
    implements MaybeObserver<T>, Disposable {
        final OtherSubscriber<T> other;

        final Publisher<U> otherSource;

        Disposable upstream;

        DelayMaybeObserver(MaybeObserver<? super T> actual, Publisher<U> otherSource) {
            this.other = new OtherSubscriber<T>(actual);
            this.otherSource = otherSource;
        }

        @Override
        public void dispose() {
            upstream.dispose();
            upstream = DisposableHelper.DISPOSED;
            SubscriptionHelper.cancel(other);
        }

        @Override
        public boolean isDisposed() {
            return other.get() == SubscriptionHelper.CANCELLED;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                other.downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            upstream = DisposableHelper.DISPOSED;
            other.value = value;
            subscribeNext();
        }

        @Override
        public void onError(Throwable e) {
            upstream = DisposableHelper.DISPOSED;
            other.error = e;
            subscribeNext();
        }

        @Override
        public void onComplete() {
            upstream = DisposableHelper.DISPOSED;
            subscribeNext();
        }

        void subscribeNext() {
            otherSource.subscribe(other);
        }
    }

    static final class OtherSubscriber<T> extends
    AtomicReference<Subscription>
    implements FlowableSubscriber<Object> {

        private static final long serialVersionUID = -1215060610805418006L;

        final MaybeObserver<? super T> downstream;

        T value;

        Throwable error;

        OtherSubscriber(MaybeObserver<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.setOnce(this, s, Long.MAX_VALUE);
        }

        @Override
        public void onNext(Object t) {
            Subscription s = get();
            if (s != SubscriptionHelper.CANCELLED) {
                lazySet(SubscriptionHelper.CANCELLED);
                s.cancel();
                onComplete();
            }
        }

        @Override
        public void onError(Throwable t) {
            Throwable e = error;
            if (e == null) {
                downstream.onError(t);
            } else {
                downstream.onError(new CompositeException(e, t));
            }
        }

        @Override
        public void onComplete() {
            Throwable e = error;
            if (e != null) {
                downstream.onError(e);
            } else {
                T v = value;
                if (v != null) {
                    downstream.onSuccess(v);
                } else {
                    downstream.onComplete();
                }
            }
        }
    }
}
