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

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.QueueDrainSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.subscribers.*;

public final class FlowableWindowBoundary<T, B> extends AbstractFlowableWithUpstream<T, Flowable<T>> {
    final Publisher<B> other;
    final int bufferSize;

    public FlowableWindowBoundary(Flowable<T> source, Publisher<B> other, int bufferSize) {
        super(source);
        this.other = other;
        this.bufferSize = bufferSize;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Flowable<T>> s) {
        source.subscribe(
                new WindowBoundaryMainSubscriber<T, B>(
                        new SerializedSubscriber<Flowable<T>>(s), other, bufferSize));
    }

    static final class WindowBoundaryMainSubscriber<T, B>
    extends QueueDrainSubscriber<T, Object, Flowable<T>>
    implements Subscription {

        final Publisher<B> other;
        final int bufferSize;

        Subscription s;

        final AtomicReference<Disposable> boundary = new AtomicReference<Disposable>();

        UnicastProcessor<T> window;

        static final Object NEXT = new Object();

        final AtomicLong windows = new AtomicLong();

        WindowBoundaryMainSubscriber(Subscriber<? super Flowable<T>> actual, Publisher<B> other,
                int bufferSize) {
            super(actual, new MpscLinkedQueue<Object>());
            this.other = other;
            this.bufferSize = bufferSize;
            windows.lazySet(1);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                Subscriber<? super Flowable<T>> a = actual;
                a.onSubscribe(this);

                if (cancelled) {
                    return;
                }

                UnicastProcessor<T> w = UnicastProcessor.<T>create(bufferSize);

                long r = requested();
                if (r != 0L) {
                    a.onNext(w);
                    if (r != Long.MAX_VALUE) {
                        produced(1);
                    }
                } else {
                    a.onError(new MissingBackpressureException("Could not deliver first window due to lack of requests"));
                    return;
                }

                window = w;

                WindowBoundaryInnerSubscriber<T, B> inner = new WindowBoundaryInnerSubscriber<T, B>(this);

                if (boundary.compareAndSet(null, inner)) {
                    windows.getAndIncrement();
                    s.request(Long.MAX_VALUE);
                    other.subscribe(inner);
                }
            }
        }

        @Override
        public void onNext(T t) {
            if (fastEnter()) {
                UnicastProcessor<T> w = window;

                w.onNext(t);

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
                DisposableHelper.dispose(boundary);
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
                DisposableHelper.dispose(boundary);
            }

            actual.onComplete();

        }

        @Override
        public void request(long n) {
            requested(n);
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        void drainLoop() {
            final SimplePlainQueue<Object> q = queue;
            final Subscriber<? super Flowable<T>> a = actual;
            int missed = 1;
            UnicastProcessor<T> w = window;
            for (;;) {

                for (;;) {
                    boolean d = done;

                    Object o = q.poll();

                    boolean empty = o == null;

                    if (d && empty) {
                        DisposableHelper.dispose(boundary);
                        Throwable e = error;
                        if (e != null) {
                            w.onError(e);
                        } else {
                            w.onComplete();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    if (o == NEXT) {
                        w.onComplete();

                        if (windows.decrementAndGet() == 0) {
                            DisposableHelper.dispose(boundary);
                            return;
                        }

                        if (cancelled) {
                            continue;
                        }

                        w = UnicastProcessor.<T>create(bufferSize);

                        long r = requested();
                        if (r != 0L) {
                            windows.getAndIncrement();

                            a.onNext(w);
                            if (r != Long.MAX_VALUE) {
                                produced(1);
                            }
                        } else {
                            // don't emit new windows
                            cancelled = true;
                            a.onError(new MissingBackpressureException("Could not deliver new window due to lack of requests"));
                            continue;
                        }

                        window = w;
                        continue;
                    }

                    w.onNext(NotificationLite.<T>getValue(o));
                }

                missed = leave(-missed);
                if (missed == 0) {
                    return;
                }
            }
        }

        void next() {
            queue.offer(NEXT);
            if (enter()) {
                drainLoop();
            }
        }

        @Override
        public boolean accept(Subscriber<? super Flowable<T>> a, Object v) {
            // not used by this operator
            return false;
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
            parent.next();
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
            parent.onComplete();
        }
    }
}
