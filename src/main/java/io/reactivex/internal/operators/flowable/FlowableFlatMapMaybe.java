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
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Maps upstream values into MaybeSources and merges their signals into one sequence.
 * @param <T> the source value type
 * @param <R> the result value type
 */
public final class FlowableFlatMapMaybe<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final Function<? super T, ? extends MaybeSource<? extends R>> mapper;

    final boolean delayErrors;

    final int maxConcurrency;

    public FlowableFlatMapMaybe(Flowable<T> source, Function<? super T, ? extends MaybeSource<? extends R>> mapper,
            boolean delayError, int maxConcurrency) {
        super(source);
        this.mapper = mapper;
        this.delayErrors = delayError;
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new FlatMapMaybeSubscriber<T, R>(s, mapper, delayErrors, maxConcurrency));
    }

    static final class FlatMapMaybeSubscriber<T, R>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = 8600231336733376951L;

        final Subscriber<? super R> actual;

        final boolean delayErrors;

        final int maxConcurrency;

        final AtomicLong requested;

        final CompositeDisposable set;

        final AtomicInteger active;

        final AtomicThrowable errors;

        final Function<? super T, ? extends MaybeSource<? extends R>> mapper;

        final AtomicReference<SpscLinkedArrayQueue<R>> queue;

        Subscription s;

        volatile boolean cancelled;

        FlatMapMaybeSubscriber(Subscriber<? super R> actual,
                Function<? super T, ? extends MaybeSource<? extends R>> mapper, boolean delayErrors, int maxConcurrency) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.maxConcurrency = maxConcurrency;
            this.requested = new AtomicLong();
            this.set = new CompositeDisposable();
            this.errors = new AtomicThrowable();
            this.active = new AtomicInteger(1);
            this.queue = new AtomicReference<SpscLinkedArrayQueue<R>>();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                int m = maxConcurrency;
                if (m == Integer.MAX_VALUE) {
                    s.request(Long.MAX_VALUE);
                } else {
                    s.request(maxConcurrency);
                }
            }
        }

        @Override
        public void onNext(T t) {
            MaybeSource<? extends R> ms;

            try {
                ms = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.cancel();
                onError(ex);
                return;
            }

            active.getAndIncrement();

            InnerObserver inner = new InnerObserver();

            if (!cancelled && set.add(inner)) {
                ms.subscribe(inner);
            }
        }

        @Override
        public void onError(Throwable t) {
            active.decrementAndGet();
            if (errors.addThrowable(t)) {
                if (!delayErrors) {
                    set.dispose();
                }
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            active.decrementAndGet();
            drain();
        }

        @Override
        public void cancel() {
            cancelled = true;
            s.cancel();
            set.dispose();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        void innerSuccess(InnerObserver inner, R value) {
            set.delete(inner);
            if (get() == 0 && compareAndSet(0, 1)) {
                boolean d = active.decrementAndGet() == 0;
                if (requested.get() != 0) {
                    actual.onNext(value);

                    SpscLinkedArrayQueue<R> q = queue.get();

                    if (d && (q == null || q.isEmpty())) {
                        Throwable ex = errors.terminate();
                        if (ex != null) {
                            actual.onError(ex);
                        } else {
                            actual.onComplete();
                        }
                        return;
                    }
                    BackpressureHelper.produced(requested, 1);
                    if (maxConcurrency != Integer.MAX_VALUE) {
                        s.request(1);
                    }
                } else {
                    SpscLinkedArrayQueue<R> q = getOrCreateQueue();
                    synchronized (q) {
                        q.offer(value);
                    }
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SpscLinkedArrayQueue<R> q = getOrCreateQueue();
                synchronized (q) {
                    q.offer(value);
                }
                active.decrementAndGet();
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        SpscLinkedArrayQueue<R> getOrCreateQueue() {
            for (;;) {
                SpscLinkedArrayQueue<R> current = queue.get();
                if (current != null) {
                    return current;
                }
                current = new SpscLinkedArrayQueue<R>(Flowable.bufferSize());
                if (queue.compareAndSet(null, current)) {
                    return current;
                }
            }
        }

        void innerError(InnerObserver inner, Throwable e) {
            set.delete(inner);
            if (errors.addThrowable(e)) {
                if (!delayErrors) {
                    s.cancel();
                    set.dispose();
                } else {
                    if (maxConcurrency != Integer.MAX_VALUE) {
                        s.request(1);
                    }
                }
                active.decrementAndGet();
                drain();
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        void innerComplete(InnerObserver inner) {
            set.delete(inner);

            if (get() == 0 && compareAndSet(0, 1)) {
                boolean d = active.decrementAndGet() == 0;
                SpscLinkedArrayQueue<R> q = queue.get();

                if (d && (q == null || q.isEmpty())) {
                    Throwable ex = errors.terminate();
                    if (ex != null) {
                        actual.onError(ex);
                    } else {
                        actual.onComplete();
                    }
                    return;
                }

                if (maxConcurrency != Integer.MAX_VALUE) {
                    s.request(1);
                }
                if (decrementAndGet() == 0) {
                    return;
                }
                drainLoop();
            } else {
                active.decrementAndGet();
                if (maxConcurrency != Integer.MAX_VALUE) {
                    s.request(1);
                }
                drain();
            }
        }

        void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }

        void clear() {
            SpscLinkedArrayQueue<R> q = queue.get();
            if (q != null) {
                q.clear();
            }
        }

        void drainLoop() {
            int missed = 1;
            Subscriber<? super R> a = actual;
            AtomicInteger n = active;
            AtomicReference<SpscLinkedArrayQueue<R>> qr = queue;

            for (;;) {
                long r = requested.get();
                long e = 0L;

                while (e != r) {
                    if (cancelled) {
                        clear();
                        return;
                    }

                    if (!delayErrors) {
                        Throwable ex = errors.get();
                        if (ex != null) {
                            ex = errors.terminate();
                            clear();
                            a.onError(ex);
                            return;
                        }
                    }

                    boolean d = n.get() == 0;
                    SpscLinkedArrayQueue<R> q = qr.get();
                    R v = q != null ? q.poll() : null;
                    boolean empty = v == null;

                    if (d && empty) {
                        Throwable ex = errors.terminate();
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onComplete();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(v);

                    e++;
                }

                if (e == r) {
                    if (cancelled) {
                        clear();
                        return;
                    }

                    if (!delayErrors) {
                        Throwable ex = errors.get();
                        if (ex != null) {
                            ex = errors.terminate();
                            clear();
                            a.onError(ex);
                            return;
                        }
                    }

                    boolean d = n.get() == 0;
                    SpscLinkedArrayQueue<R> q = qr.get();
                    boolean empty = q == null || q.isEmpty();

                    if (d && empty) {
                        Throwable ex = errors.terminate();
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onComplete();
                        }
                        return;
                    }
                }

                if (e != 0L) {
                    BackpressureHelper.produced(requested, e);
                    if (maxConcurrency != Integer.MAX_VALUE) {
                        s.request(e);
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        final class InnerObserver extends AtomicReference<Disposable>
        implements MaybeObserver<R>, Disposable {
            private static final long serialVersionUID = -502562646270949838L;

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onSuccess(R value) {
                innerSuccess(this, value);
            }

            @Override
            public void onError(Throwable e) {
                innerError(this, e);
            }

            @Override
            public void onComplete() {
                innerComplete(this);
            }

            @Override
            public boolean isDisposed() {
                return DisposableHelper.isDisposed(get());
            }

            @Override
            public void dispose() {
                DisposableHelper.dispose(this);
            }
        }
    }
}
