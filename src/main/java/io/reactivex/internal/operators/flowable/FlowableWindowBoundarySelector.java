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
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.QueueDrainSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.subscribers.*;

public final class FlowableWindowBoundarySelector<T, B, V> extends AbstractFlowableWithUpstream<T, Flowable<T>> {
    final Publisher<B> open;
    final Function<? super B, ? extends Publisher<V>> close;
    final int bufferSize;

    public FlowableWindowBoundarySelector(
            Flowable<T> source,
            Publisher<B> open, Function<? super B, ? extends Publisher<V>> close,
            int bufferSize) {
        super(source);
        this.open = open;
        this.close = close;
        this.bufferSize = bufferSize;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Flowable<T>> s) {
        source.subscribe(new WindowBoundaryMainSubscriber<T, B, V>(
                new SerializedSubscriber<Flowable<T>>(s),
                open, close, bufferSize));
    }

    static final class WindowBoundaryMainSubscriber<T, B, V>
    extends QueueDrainSubscriber<T, Object, Flowable<T>>
    implements Subscription {
        final Publisher<B> open;
        final Function<? super B, ? extends Publisher<V>> close;
        final int bufferSize;
        final CompositeDisposable resources;

        Subscription s;

        final AtomicReference<Disposable> boundary = new AtomicReference<Disposable>();

        final List<UnicastProcessor<T>> ws;

        final AtomicLong windows = new AtomicLong();

        WindowBoundaryMainSubscriber(Subscriber<? super Flowable<T>> actual,
                Publisher<B> open, Function<? super B, ? extends Publisher<V>> close, int bufferSize) {
            super(actual, new MpscLinkedQueue<Object>());
            this.open = open;
            this.close = close;
            this.bufferSize = bufferSize;
            this.resources = new CompositeDisposable();
            this.ws = new ArrayList<UnicastProcessor<T>>();
            windows.lazySet(1);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                if (cancelled) {
                    return;
                }

                OperatorWindowBoundaryOpenSubscriber<T, B> os = new OperatorWindowBoundaryOpenSubscriber<T, B>(this);

                if (boundary.compareAndSet(null, os)) {
                    windows.getAndIncrement();
                    s.request(Long.MAX_VALUE);
                    open.subscribe(os);
                }
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (fastEnter()) {
                for (UnicastProcessor<T> w : ws) {
                    w.onNext(t);
                }
                if (leave(-1) == 0) {
                    return;
                }
            } else {
                queue.offer(NotificationLite.next(t));
                if (!enter()) {
                    return;
                }
            }
            drainLoop();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;

            if (enter()) {
                drainLoop();
            }

            if (windows.decrementAndGet() == 0) {
                resources.dispose();
            }

            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            if (enter()) {
                drainLoop();
            }

            if (windows.decrementAndGet() == 0) {
                resources.dispose();
            }

            actual.onComplete();
        }

        void error(Throwable t) {
            s.cancel();
            resources.dispose();
            DisposableHelper.dispose(boundary);

            actual.onError(t);
        }

        @Override
        public void request(long n) {
            requested(n);
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        void dispose() {
            resources.dispose();
            DisposableHelper.dispose(boundary);
        }

        void drainLoop() {
            final SimplePlainQueue<Object> q = queue;
            final Subscriber<? super Flowable<T>> a = actual;
            final List<UnicastProcessor<T>> ws = this.ws;
            int missed = 1;

            for (;;) {

                for (;;) {
                    boolean d = done;
                    Object o = q.poll();

                    boolean empty = o == null;

                    if (d && empty) {
                        dispose();
                        Throwable e = error;
                        if (e != null) {
                            for (UnicastProcessor<T> w : ws) {
                                w.onError(e);
                            }
                        } else {
                            for (UnicastProcessor<T> w : ws) {
                                w.onComplete();
                            }
                        }
                        ws.clear();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    if (o instanceof WindowOperation) {
                        @SuppressWarnings("unchecked")
                        WindowOperation<T, B> wo = (WindowOperation<T, B>) o;

                        UnicastProcessor<T> w = wo.w;
                        if (w != null) {
                            if (ws.remove(wo.w)) {
                                wo.w.onComplete();

                                if (windows.decrementAndGet() == 0) {
                                    dispose();
                                    return;
                                }
                            }
                            continue;
                        }

                        if (cancelled) {
                            continue;
                        }


                        w = UnicastProcessor.<T>create(bufferSize);

                        long r = requested();
                        if (r != 0L) {
                            ws.add(w);
                            a.onNext(w);
                            if (r != Long.MAX_VALUE) {
                                produced(1);
                            }
                        } else {
                            cancelled = true;
                            a.onError(new MissingBackpressureException("Could not deliver new window due to lack of requests"));
                            continue;
                        }

                        Publisher<V> p;

                        try {
                            p = ObjectHelper.requireNonNull(close.apply(wo.open), "The publisher supplied is null");
                        } catch (Throwable e) {
                            cancelled = true;
                            a.onError(e);
                            continue;
                        }

                        OperatorWindowBoundaryCloseSubscriber<T, V> cl = new OperatorWindowBoundaryCloseSubscriber<T, V>(this, w);

                        if (resources.add(cl)) {
                            windows.getAndIncrement();

                            p.subscribe(cl);
                        }

                        continue;
                    }

                    for (UnicastProcessor<T> w : ws) {
                        w.onNext(NotificationLite.<T>getValue(o));
                    }
                }

                missed = leave(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public boolean accept(Subscriber<? super Flowable<T>> a, Object v) {
            // not used by this operator
            return false;
        }

        void open(B b) {
            queue.offer(new WindowOperation<T, B>(null, b));
            if (enter()) {
                drainLoop();
            }
        }

        void close(OperatorWindowBoundaryCloseSubscriber<T, V> w) {
            resources.delete(w);
            queue.offer(new WindowOperation<T, B>(w.w, null));
            if (enter()) {
                drainLoop();
            }
        }
    }

    static final class WindowOperation<T, B> {
        final UnicastProcessor<T> w;
        final B open;
        WindowOperation(UnicastProcessor<T> w, B open) {
            this.w = w;
            this.open = open;
        }
    }

    static final class OperatorWindowBoundaryOpenSubscriber<T, B> extends DisposableSubscriber<B> {
        final WindowBoundaryMainSubscriber<T, B, ?> parent;

        boolean done;

        OperatorWindowBoundaryOpenSubscriber(WindowBoundaryMainSubscriber<T, B, ?> parent) {
            this.parent = parent;
        }

        @Override
        public void onNext(B t) {
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
            parent.error(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.onComplete();
        }
    }

    static final class OperatorWindowBoundaryCloseSubscriber<T, V> extends DisposableSubscriber<V> {
        final WindowBoundaryMainSubscriber<T, ?, V> parent;
        final UnicastProcessor<T> w;

        boolean done;

        OperatorWindowBoundaryCloseSubscriber(WindowBoundaryMainSubscriber<T, ?, V> parent, UnicastProcessor<T> w) {
            this.parent = parent;
            this.w = w;
        }

        @Override
        public void onNext(V t) {
            if (done) {
                return;
            }
            done = true;
            cancel();
            parent.close(this);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            parent.error(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.close(this);
        }
    }
}
