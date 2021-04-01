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

package io.reactivex.rxjava3.internal.operators.completable;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.queue.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class CompletableConcat extends Completable {
    final Publisher<? extends CompletableSource> sources;
    final int prefetch;

    public CompletableConcat(Publisher<? extends CompletableSource> sources, int prefetch) {
        this.sources = sources;
        this.prefetch = prefetch;
    }

    @Override
    public void subscribeActual(CompletableObserver observer) {
        sources.subscribe(new CompletableConcatSubscriber(observer, prefetch));
    }

    static final class CompletableConcatSubscriber
    extends AtomicInteger
    implements FlowableSubscriber<CompletableSource>, Disposable {
        private static final long serialVersionUID = 9032184911934499404L;

        final CompletableObserver downstream;

        final int prefetch;

        final int limit;

        final ConcatInnerObserver inner;

        final AtomicBoolean once;

        int sourceFused;

        int consumed;

        SimpleQueue<CompletableSource> queue;

        Subscription upstream;

        volatile boolean done;

        volatile boolean active;

        CompletableConcatSubscriber(CompletableObserver actual, int prefetch) {
            this.downstream = actual;
            this.prefetch = prefetch;
            this.inner = new ConcatInnerObserver(this);
            this.once = new AtomicBoolean();
            this.limit = prefetch - (prefetch >> 2);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                long r = prefetch == Integer.MAX_VALUE ? Long.MAX_VALUE : prefetch;

                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<CompletableSource> qs = (QueueSubscription<CompletableSource>) s;

                    int m = qs.requestFusion(QueueSubscription.ANY);

                    if (m == QueueSubscription.SYNC) {
                        sourceFused = m;
                        queue = qs;
                        done = true;
                        downstream.onSubscribe(this);
                        drain();
                        return;
                    }
                    if (m == QueueSubscription.ASYNC) {
                        sourceFused = m;
                        queue = qs;
                        downstream.onSubscribe(this);
                        s.request(r);
                        return;
                    }
                }

                if (prefetch == Integer.MAX_VALUE) {
                    queue = new SpscLinkedArrayQueue<>(Flowable.bufferSize());
                } else {
                    queue = new SpscArrayQueue<>(prefetch);
                }

                downstream.onSubscribe(this);

                s.request(r);
            }
        }

        @Override
        public void onNext(CompletableSource t) {
            if (sourceFused == QueueSubscription.NONE) {
                if (!queue.offer(t)) {
                    onError(new MissingBackpressureException());
                    return;
                }
            }
            drain();
        }

        @Override
        public void onError(Throwable t) {
            if (once.compareAndSet(false, true)) {
                DisposableHelper.dispose(inner);
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public void dispose() {
            upstream.cancel();
            DisposableHelper.dispose(inner);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(inner.get());
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            for (;;) {
                if (isDisposed()) {
                    return;
                }

                if (!active) {

                    boolean d = done;

                    CompletableSource cs;

                    try {
                        cs = queue.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        innerError(ex);
                        return;
                    }

                    boolean empty = cs == null;

                    if (d && empty) {
                        // errors never set done or call drain.
                        downstream.onComplete();
                        return;
                    }

                    if (!empty) {
                        active = true;
                        cs.subscribe(inner);
                        request();
                    }
                }

                if (decrementAndGet() == 0) {
                    break;
                }
            }
        }

        void request() {
            if (sourceFused != QueueSubscription.SYNC) {
                int p = consumed + 1;
                if (p == limit) {
                    consumed = 0;
                    upstream.request(p);
                } else {
                    consumed = p;
                }
            }
        }

        void innerError(Throwable e) {
            if (once.compareAndSet(false, true)) {
                upstream.cancel();
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        void innerComplete() {
            active = false;
            drain();
        }

        static final class ConcatInnerObserver extends AtomicReference<Disposable> implements CompletableObserver {
            private static final long serialVersionUID = -5454794857847146511L;

            final CompletableConcatSubscriber parent;

            ConcatInnerObserver(CompletableConcatSubscriber parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.replace(this, d);
            }

            @Override
            public void onError(Throwable e) {
                parent.innerError(e);
            }

            @Override
            public void onComplete() {
                parent.innerComplete();
            }
        }
    }
}
