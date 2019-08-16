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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.BackpressureHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class FlowableOnBackpressureDrop<T> extends AbstractFlowableWithUpstream<T, T> implements Consumer<T> {

    final Consumer<? super T> onDrop;

    public FlowableOnBackpressureDrop(Flowable<T> source) {
        super(source);
        this.onDrop = this;
    }

    public FlowableOnBackpressureDrop(Flowable<T> source, Consumer<? super T> onDrop) {
        super(source);
        this.onDrop = onDrop;
    }

    @Override
    public void accept(T t) {
        // deliberately ignoring
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        this.source.subscribe(new BackpressureDropSubscriber<T>(s, onDrop));
    }

    static final class BackpressureDropSubscriber<T>
    extends AtomicLong implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -6246093802440953054L;

        final Subscriber<? super T> downstream;
        final Consumer<? super T> onDrop;

        Subscription upstream;

        boolean done;

        BackpressureDropSubscriber(Subscriber<? super T> actual, Consumer<? super T> onDrop) {
            this.downstream = actual;
            this.onDrop = onDrop;
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
            long r = get();
            if (r != 0L) {
                downstream.onNext(t);
                BackpressureHelper.produced(this, 1);
            } else {
                try {
                    onDrop.accept(t);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();
                    onError(e);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            downstream.onComplete();
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
        }
    }
}
