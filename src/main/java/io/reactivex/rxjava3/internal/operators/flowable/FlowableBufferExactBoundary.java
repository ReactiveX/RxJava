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

import java.util.Collection;
import java.util.Objects;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.queue.MpscLinkedQueue;
import io.reactivex.rxjava3.internal.subscribers.QueueDrainSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.QueueDrainHelper;
import io.reactivex.rxjava3.subscribers.*;

public final class FlowableBufferExactBoundary<T, U extends Collection<? super T>, B>
extends AbstractFlowableWithUpstream<T, U> {
    final Publisher<B> boundary;
    final Supplier<U> bufferSupplier;

    public FlowableBufferExactBoundary(Flowable<T> source, Publisher<B> boundary, Supplier<U> bufferSupplier) {
        super(source);
        this.boundary = boundary;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        source.subscribe(new BufferExactBoundarySubscriber<T, U, B>(new SerializedSubscriber<U>(s), bufferSupplier, boundary));
    }

    static final class BufferExactBoundarySubscriber<T, U extends Collection<? super T>, B>
    extends QueueDrainSubscriber<T, U, U> implements FlowableSubscriber<T>, Subscription, Disposable {

        final Supplier<U> bufferSupplier;
        final Publisher<B> boundary;

        Subscription upstream;

        Disposable other;

        U buffer;

        BufferExactBoundarySubscriber(Subscriber<? super U> actual, Supplier<U> bufferSupplier,
                                             Publisher<B> boundary) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferSupplier = bufferSupplier;
            this.boundary = boundary;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (!SubscriptionHelper.validate(this.upstream, s)) {
                return;
            }
            this.upstream = s;

            U b;

            try {
                b = Objects.requireNonNull(bufferSupplier.get(), "The buffer supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                cancelled = true;
                s.cancel();
                EmptySubscription.error(e, downstream);
                return;
            }

            buffer = b;

            BufferBoundarySubscriber<T, U, B> bs = new BufferBoundarySubscriber<T, U, B>(this);
            other = bs;

            downstream.onSubscribe(this);

            if (!cancelled) {
                s.request(Long.MAX_VALUE);

                boundary.subscribe(bs);
            }
        }

        @Override
        public void onNext(T t) {
            synchronized (this) {
                U b = buffer;
                if (b == null) {
                    return;
                }
                b.add(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            cancel();
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            U b;
            synchronized (this) {
                b = buffer;
                if (b == null) {
                    return;
                }
                buffer = null;
            }
            queue.offer(b);
            done = true;
            if (enter()) {
                QueueDrainHelper.drainMaxLoop(queue, downstream, false, this, this);
            }
        }

        @Override
        public void request(long n) {
            requested(n);
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                other.dispose();
                upstream.cancel();

                if (enter()) {
                    queue.clear();
                }
            }
        }

        void next() {

            U next;

            try {
                next = Objects.requireNonNull(bufferSupplier.get(), "The buffer supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                cancel();
                downstream.onError(e);
                return;
            }

            U b;
            synchronized (this) {
                b = buffer;
                if (b == null) {
                    return;
                }
                buffer = next;
            }

            fastPathEmitMax(b, false, this);
        }

        @Override
        public void dispose() {
            cancel();
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        @Override
        public boolean accept(Subscriber<? super U> a, U v) {
            downstream.onNext(v);
            return true;
        }

    }

    static final class BufferBoundarySubscriber<T, U extends Collection<? super T>, B> extends DisposableSubscriber<B> {
        final BufferExactBoundarySubscriber<T, U, B> parent;

        BufferBoundarySubscriber(BufferExactBoundarySubscriber<T, U, B> parent) {
            this.parent = parent;
        }

        @Override
        public void onNext(B t) {
            parent.next();
        }

        @Override
        public void onError(Throwable t) {
            parent.onError(t);
        }

        @Override
        public void onComplete() {
            parent.onComplete();
        }
    }
}
