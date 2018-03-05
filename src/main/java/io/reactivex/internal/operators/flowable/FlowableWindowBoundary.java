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
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.subscribers.DisposableSubscriber;

public final class FlowableWindowBoundary<T, B> extends AbstractFlowableWithUpstream<T, Flowable<T>> {
    final Publisher<B> other;
    final int capacityHint;

    public FlowableWindowBoundary(Flowable<T> source, Publisher<B> other, int capacityHint) {
        super(source);
        this.other = other;
        this.capacityHint = capacityHint;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Flowable<T>> subscriber) {
        WindowBoundaryMainSubscriber<T, B> parent = new WindowBoundaryMainSubscriber<T, B>(subscriber, capacityHint);

        subscriber.onSubscribe(parent);

        parent.innerNext();

        other.subscribe(parent.boundarySubscriber);

        source.subscribe(parent);
    }

    static final class WindowBoundaryMainSubscriber<T, B>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = 2233020065421370272L;

        final Subscriber<? super Flowable<T>> downstream;

        final int capacityHint;

        final WindowBoundaryInnerSubscriber<T, B> boundarySubscriber;

        final AtomicReference<Subscription> upstream;

        final AtomicInteger windows;

        final MpscLinkedQueue<Object> queue;

        final AtomicThrowable errors;

        final AtomicBoolean stopWindows;

        final AtomicLong requested;

        static final Object NEXT_WINDOW = new Object();

        volatile boolean done;

        UnicastProcessor<T> window;

        long emitted;

        WindowBoundaryMainSubscriber(Subscriber<? super Flowable<T>> downstream, int capacityHint) {
            this.downstream = downstream;
            this.capacityHint = capacityHint;
            this.boundarySubscriber = new WindowBoundaryInnerSubscriber<T, B>(this);
            this.upstream = new AtomicReference<Subscription>();
            this.windows = new AtomicInteger(1);
            this.queue = new MpscLinkedQueue<Object>();
            this.errors = new AtomicThrowable();
            this.stopWindows = new AtomicBoolean();
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription d) {
            SubscriptionHelper.setOnce(upstream, d, Long.MAX_VALUE);
        }

        @Override
        public void onNext(T t) {
            queue.offer(t);
            drain();
        }

        @Override
        public void onError(Throwable e) {
            boundarySubscriber.dispose();
            if (errors.addThrowable(e)) {
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            boundarySubscriber.dispose();
            done = true;
            drain();
        }

        @Override
        public void cancel() {
            if (stopWindows.compareAndSet(false, true)) {
                boundarySubscriber.dispose();
                if (windows.decrementAndGet() == 0) {
                    SubscriptionHelper.cancel(upstream);
                }
            }
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(requested, n);
        }

        @Override
        public void run() {
            if (windows.decrementAndGet() == 0) {
                SubscriptionHelper.cancel(upstream);
            }
        }

        void innerNext() {
            queue.offer(NEXT_WINDOW);
            drain();
        }

        void innerError(Throwable e) {
            SubscriptionHelper.cancel(upstream);
            if (errors.addThrowable(e)) {
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        void innerComplete() {
            SubscriptionHelper.cancel(upstream);
            done = true;
            drain();
        }

        @SuppressWarnings("unchecked")
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Subscriber<? super Flowable<T>> downstream = this.downstream;
            MpscLinkedQueue<Object> queue = this.queue;
            AtomicThrowable errors = this.errors;
            long emitted = this.emitted;

            for (;;) {

                for (;;) {
                    if (windows.get() == 0) {
                        queue.clear();
                        window = null;
                        return;
                    }

                    UnicastProcessor<T> w = window;

                    boolean d = done;

                    if (d && errors.get() != null) {
                        queue.clear();
                        Throwable ex = errors.terminate();
                        if (w != null) {
                            window = null;
                            w.onError(ex);
                        }
                        downstream.onError(ex);
                        return;
                    }

                    Object v = queue.poll();

                    boolean empty = v == null;

                    if (d && empty) {
                        Throwable ex = errors.terminate();
                        if (ex == null) {
                            if (w != null) {
                                window = null;
                                w.onComplete();
                            }
                            downstream.onComplete();
                        } else {
                            if (w != null) {
                                window = null;
                                w.onError(ex);
                            }
                            downstream.onError(ex);
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    if (v != NEXT_WINDOW) {
                        w.onNext((T)v);
                        continue;
                    }

                    if (w != null) {
                        window = null;
                        w.onComplete();
                    }

                    if (!stopWindows.get()) {
                        w = UnicastProcessor.create(capacityHint, this);
                        window = w;
                        windows.getAndIncrement();

                        if (emitted != requested.get()) {
                            emitted++;
                            downstream.onNext(w);
                        } else {
                            SubscriptionHelper.cancel(upstream);
                            boundarySubscriber.dispose();
                            errors.addThrowable(new MissingBackpressureException("Could not deliver a window due to lack of requests"));
                            done = true;
                        }
                    }
                }

                this.emitted = emitted;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

    static final class WindowBoundaryInnerSubscriber<T, B> extends DisposableSubscriber<B> {

        final WindowBoundaryMainSubscriber<T, B> parent;

        boolean done;

        WindowBoundaryInnerSubscriber(WindowBoundaryMainSubscriber<T, B> parent) {
            this.parent = parent;
        }

        @Override
        public void onNext(B t) {
            if (done) {
                return;
            }
            parent.innerNext();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            parent.innerError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.innerComplete();
        }
    }
}
