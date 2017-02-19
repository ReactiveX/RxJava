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

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.QueueDrainSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.QueueDrainHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.*;

public final class FlowableBufferBoundary<T, U extends Collection<? super T>, Open, Close>
extends AbstractFlowableWithUpstream<T, U> {
    final Callable<U> bufferSupplier;
    final Publisher<? extends Open> bufferOpen;
    final Function<? super Open, ? extends Publisher<? extends Close>> bufferClose;

    public FlowableBufferBoundary(Flowable<T> source, Publisher<? extends Open> bufferOpen,
            Function<? super Open, ? extends Publisher<? extends Close>> bufferClose, Callable<U> bufferSupplier) {
        super(source);
        this.bufferOpen = bufferOpen;
        this.bufferClose = bufferClose;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        source.subscribe(new BufferBoundarySubscriber<T, U, Open, Close>(
                new SerializedSubscriber<U>(s),
                bufferOpen, bufferClose, bufferSupplier
            ));
    }

    static final class BufferBoundarySubscriber<T, U extends Collection<? super T>, Open, Close>
    extends QueueDrainSubscriber<T, U, U> implements Subscription, Disposable {
        final Publisher<? extends Open> bufferOpen;
        final Function<? super Open, ? extends Publisher<? extends Close>> bufferClose;
        final Callable<U> bufferSupplier;
        final CompositeDisposable resources;

        Subscription s;

        final List<U> buffers;

        final AtomicInteger windows = new AtomicInteger();

        BufferBoundarySubscriber(Subscriber<? super U> actual,
                Publisher<? extends Open> bufferOpen,
                Function<? super Open, ? extends Publisher<? extends Close>> bufferClose,
                        Callable<U> bufferSupplier) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferOpen = bufferOpen;
            this.bufferClose = bufferClose;
            this.bufferSupplier = bufferSupplier;
            this.buffers = new LinkedList<U>();
            this.resources = new CompositeDisposable();
        }
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                BufferOpenSubscriber<T, U, Open, Close> bos = new BufferOpenSubscriber<T, U, Open, Close>(this);
                resources.add(bos);

                actual.onSubscribe(this);

                windows.lazySet(1);
                bufferOpen.subscribe(bos);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            synchronized (this) {
                for (U b : buffers) {
                    b.add(t);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            cancel();
            cancelled = true;
            synchronized (this) {
                buffers.clear();
            }
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (windows.decrementAndGet() == 0) {
                complete();
            }
        }

        void complete() {
            List<U> list;
            synchronized (this) {
                list = new ArrayList<U>(buffers);
                buffers.clear();
            }

            SimplePlainQueue<U> q = queue;
            for (U u : list) {
                q.offer(u);
            }
            done = true;
            if (enter()) {
                QueueDrainHelper.drainMaxLoop(q, actual, false, this, this);
            }
        }

        @Override
        public void request(long n) {
            requested(n);
        }

        @Override
        public void dispose() {
            resources.dispose();
        }

        @Override
        public boolean isDisposed() {
            return resources.isDisposed();
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                dispose();
            }
        }

        @Override
        public boolean accept(Subscriber<? super U> a, U v) {
            a.onNext(v);
            return true;
        }

        void open(Open window) {
            if (cancelled) {
                return;
            }

            U b;

            try {
                b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The buffer supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                onError(e);
                return;
            }

            Publisher<? extends Close> p;

            try {
                p = ObjectHelper.requireNonNull(bufferClose.apply(window), "The buffer closing publisher is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                onError(e);
                return;
            }

            if (cancelled) {
                return;
            }

            synchronized (this) {
                if (cancelled) {
                    return;
                }
                buffers.add(b);
            }

            BufferCloseSubscriber<T, U, Open, Close> bcs = new BufferCloseSubscriber<T, U, Open, Close>(b, this);
            resources.add(bcs);

            windows.getAndIncrement();

            p.subscribe(bcs);
        }

        void openFinished(Disposable d) {
            if (resources.remove(d)) {
                if (windows.decrementAndGet() == 0) {
                    complete();
                }
            }
        }

        void close(U b, Disposable d) {

            boolean e;
            synchronized (this) {
                e = buffers.remove(b);
            }

            if (e) {
                fastPathOrderedEmitMax(b, false, this);
            }

            if (resources.remove(d)) {
                if (windows.decrementAndGet() == 0) {
                    complete();
                }
            }
        }
    }

    static final class BufferOpenSubscriber<T, U extends Collection<? super T>, Open, Close>
    extends DisposableSubscriber<Open> {
        final BufferBoundarySubscriber<T, U, Open, Close> parent;

        boolean done;

        BufferOpenSubscriber(BufferBoundarySubscriber<T, U, Open, Close> parent) {
            this.parent = parent;
        }
        @Override
        public void onNext(Open t) {
            if (done) {
                return;
            }
            parent.open(t);
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
            parent.openFinished(this);
        }
    }

    static final class BufferCloseSubscriber<T, U extends Collection<? super T>, Open, Close>
    extends DisposableSubscriber<Close> {
        final BufferBoundarySubscriber<T, U, Open, Close> parent;
        final U value;
        boolean done;
        BufferCloseSubscriber(U value, BufferBoundarySubscriber<T, U, Open, Close> parent) {
            this.parent = parent;
            this.value = value;
        }

        @Override
        public void onNext(Close t) {
            onComplete();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            parent.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.close(value, this);
        }
    }
}
