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

package io.reactivex.internal.operators.flowable;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.*;

public final class FlowableDebounce<T, U> extends AbstractFlowableWithUpstream<T, T> {
    final Function<? super T, ? extends Publisher<U>> debounceSelector;

    public FlowableDebounce(Flowable<T> source, Function<? super T, ? extends Publisher<U>> debounceSelector) {
        super(source);
        this.debounceSelector = debounceSelector;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DebounceSubscriber<T, U>(new SerializedSubscriber<T>(s), debounceSelector));
    }

    static final class DebounceSubscriber<T, U> extends AtomicLong
    implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = 6725975399620862591L;
        final Subscriber<? super T> actual;
        final Function<? super T, ? extends Publisher<U>> debounceSelector;

        Subscription s;

        final AtomicReference<Disposable> debouncer = new AtomicReference<Disposable>();

        volatile long index;

        boolean done;

        DebounceSubscriber(Subscriber<? super T> actual,
                Function<? super T, ? extends Publisher<U>> debounceSelector) {
            this.actual = actual;
            this.debounceSelector = debounceSelector;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            long idx = index + 1;
            index = idx;

            Disposable d = debouncer.get();
            if (d != null) {
                d.dispose();
            }

            Publisher<U> p;

            try {
                p = ObjectHelper.requireNonNull(debounceSelector.apply(t), "The publisher supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                cancel();
                actual.onError(e);
                return;
            }

            DebounceInnerSubscriber<T, U> dis = new DebounceInnerSubscriber<T, U>(this, idx, t);

            if (debouncer.compareAndSet(d, dis)) {
                p.subscribe(dis);
            }
        }

        @Override
        public void onError(Throwable t) {
            DisposableHelper.dispose(debouncer);
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            Disposable d = debouncer.get();
            if (!DisposableHelper.isDisposed(d)) {
                @SuppressWarnings("unchecked")
                DebounceInnerSubscriber<T, U> dis = (DebounceInnerSubscriber<T, U>)d;
                dis.emit();
                DisposableHelper.dispose(debouncer);
                actual.onComplete();
            }
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(this, n);
            }
        }

        @Override
        public void cancel() {
            s.cancel();
            DisposableHelper.dispose(debouncer);
        }

        void emit(long idx, T value) {
            if (idx == index) {
                long r = get();
                if (r != 0L) {
                    actual.onNext(value);
                    BackpressureHelper.produced(this, 1);
                } else {
                    cancel();
                    actual.onError(new MissingBackpressureException("Could not deliver value due to lack of requests"));
                }
            }
        }

        static final class DebounceInnerSubscriber<T, U> extends DisposableSubscriber<U> {
            final DebounceSubscriber<T, U> parent;
            final long index;
            final T value;

            boolean done;

            final AtomicBoolean once = new AtomicBoolean();

            DebounceInnerSubscriber(DebounceSubscriber<T, U> parent, long index, T value) {
                this.parent = parent;
                this.index = index;
                this.value = value;
            }

            @Override
            public void onNext(U t) {
                if (done) {
                    return;
                }
                done = true;
                cancel();
                emit();
            }

            void emit() {
                if (once.compareAndSet(false, true)) {
                    parent.emit(index, value);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (done) {
                    RxJavaPlugins.onError(t);
                    return;
                }
                done = true;
                parent.onError(t);
            }

            @Override
            public void onComplete() {
                if (done) {
                    return;
                }
                done = true;
                emit();
            }
        }
    }
}
