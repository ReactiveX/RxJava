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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.BackpressureHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subscribers.*;

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
        final Subscriber<? super T> downstream;
        final Function<? super T, ? extends Publisher<U>> debounceSelector;

        Subscription upstream;

        final AtomicReference<Disposable> debouncer = new AtomicReference<Disposable>();

        volatile long index;

        boolean done;

        DebounceSubscriber(Subscriber<? super T> actual,
                Function<? super T, ? extends Publisher<U>> debounceSelector) {
            this.downstream = actual;
            this.debounceSelector = debounceSelector;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
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
                p = Objects.requireNonNull(debounceSelector.apply(t), "The publisher supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                cancel();
                downstream.onError(e);
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
            downstream.onError(t);
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
                if (dis != null) {
                    dis.emit();
                }
                DisposableHelper.dispose(debouncer);
                downstream.onComplete();
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
            upstream.cancel();
            DisposableHelper.dispose(debouncer);
        }

        void emit(long idx, T value) {
            if (idx == index) {
                long r = get();
                if (r != 0L) {
                    downstream.onNext(value);
                    BackpressureHelper.produced(this, 1);
                } else {
                    cancel();
                    downstream.onError(new MissingBackpressureException("Could not deliver value due to lack of requests"));
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
