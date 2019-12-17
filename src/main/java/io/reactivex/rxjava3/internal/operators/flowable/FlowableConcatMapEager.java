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
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.fuseable.SimpleQueue;
import io.reactivex.rxjava3.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.rxjava3.internal.subscribers.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;

public final class FlowableConcatMapEager<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    final int maxConcurrency;

    final int prefetch;

    final ErrorMode errorMode;

    public FlowableConcatMapEager(Flowable<T> source,
            Function<? super T, ? extends Publisher<? extends R>> mapper,
            int maxConcurrency,
            int prefetch,
            ErrorMode errorMode) {
        super(source);
        this.mapper = mapper;
        this.maxConcurrency = maxConcurrency;
        this.prefetch = prefetch;
        this.errorMode = errorMode;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new ConcatMapEagerDelayErrorSubscriber<T, R>(
                s, mapper, maxConcurrency, prefetch, errorMode));
    }

    static final class ConcatMapEagerDelayErrorSubscriber<T, R>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription, InnerQueuedSubscriberSupport<R> {

        private static final long serialVersionUID = -4255299542215038287L;

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends Publisher<? extends R>> mapper;

        final int maxConcurrency;

        final int prefetch;

        final ErrorMode errorMode;

        final AtomicThrowable errors;

        final AtomicLong requested;

        final SpscLinkedArrayQueue<InnerQueuedSubscriber<R>> subscribers;

        Subscription upstream;

        volatile boolean cancelled;

        volatile boolean done;

        volatile InnerQueuedSubscriber<R> current;

        ConcatMapEagerDelayErrorSubscriber(Subscriber<? super R> actual,
                Function<? super T, ? extends Publisher<? extends R>> mapper, int maxConcurrency, int prefetch,
                ErrorMode errorMode) {
            this.downstream = actual;
            this.mapper = mapper;
            this.maxConcurrency = maxConcurrency;
            this.prefetch = prefetch;
            this.errorMode = errorMode;
            this.subscribers = new SpscLinkedArrayQueue<InnerQueuedSubscriber<R>>(Math.min(prefetch, maxConcurrency));
            this.errors = new AtomicThrowable();
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);

                s.request(maxConcurrency == Integer.MAX_VALUE ? Long.MAX_VALUE : maxConcurrency);
            }

        }

        @Override
        public void onNext(T t) {
            Publisher<? extends R> p;

            try {
                p = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Publisher");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                onError(ex);
                return;
            }

            InnerQueuedSubscriber<R> inner = new InnerQueuedSubscriber<R>(this, prefetch);

            if (cancelled) {
                return;
            }

            subscribers.offer(inner);

            p.subscribe(inner);

            if (cancelled) {
                inner.cancel();
                drainAndCancel();
            }
        }

        @Override
        public void onError(Throwable t) {
            if (errors.tryAddThrowableOrReport(t)) {
                done = true;
                drain();
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            upstream.cancel();
            errors.tryTerminateAndReport();

            drainAndCancel();
        }

        void drainAndCancel() {
            if (getAndIncrement() == 0) {
                do {
                    cancelAll();
                } while (decrementAndGet() != 0);
            }
        }

        void cancelAll() {
            InnerQueuedSubscriber<R> inner = current;
            current = null;

            if (inner != null) {
                inner.cancel();
            }

            while ((inner = subscribers.poll()) != null) {
                inner.cancel();
            }
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public void innerNext(InnerQueuedSubscriber<R> inner, R value) {
            if (inner.queue().offer(value)) {
                drain();
            } else {
                inner.cancel();
                innerError(inner, new MissingBackpressureException());
            }
        }

        @Override
        public void innerError(InnerQueuedSubscriber<R> inner, Throwable e) {
            if (errors.tryAddThrowableOrReport(e)) {
                inner.setDone();
                if (errorMode != ErrorMode.END) {
                    upstream.cancel();
                }
                drain();
            }
        }

        @Override
        public void innerComplete(InnerQueuedSubscriber<R> inner) {
            inner.setDone();
            drain();
        }

        @Override
        public void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            InnerQueuedSubscriber<R> inner = current;
            Subscriber<? super R> a = downstream;
            ErrorMode em = errorMode;

            for (;;) {
                long r = requested.get();
                long e = 0L;

                if (inner == null) {

                    if (em != ErrorMode.END) {
                        Throwable ex = errors.get();
                        if (ex != null) {
                            cancelAll();

                            errors.tryTerminateConsumer(downstream);
                            return;
                        }
                    }

                    boolean outerDone = done;

                    inner = subscribers.poll();

                    if (outerDone && inner == null) {
                        errors.tryTerminateConsumer(downstream);
                        return;
                    }

                    if (inner != null) {
                        current = inner;
                    }
                }

                boolean continueNextSource = false;

                if (inner != null) {
                    SimpleQueue<R> q = inner.queue();
                    if (q != null) {
                        while (e != r) {
                            if (cancelled) {
                                cancelAll();
                                return;
                            }

                            if (em == ErrorMode.IMMEDIATE) {
                                Throwable ex = errors.get();
                                if (ex != null) {
                                    current = null;
                                    inner.cancel();
                                    cancelAll();

                                    errors.tryTerminateConsumer(downstream);
                                    return;
                                }
                            }

                            boolean d = inner.isDone();

                            R v;

                            try {
                                v = q.poll();
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                current = null;
                                inner.cancel();
                                cancelAll();
                                a.onError(ex);
                                return;
                            }

                            boolean empty = v == null;

                            if (d && empty) {
                                inner = null;
                                current = null;
                                upstream.request(1);
                                continueNextSource = true;
                                break;
                            }

                            if (empty) {
                                break;
                            }

                            a.onNext(v);

                            e++;

                            inner.requestOne();
                        }

                        if (e == r) {
                            if (cancelled) {
                                cancelAll();
                                return;
                            }

                            if (em == ErrorMode.IMMEDIATE) {
                                Throwable ex = errors.get();
                                if (ex != null) {
                                    current = null;
                                    inner.cancel();
                                    cancelAll();

                                    errors.tryTerminateConsumer(downstream);
                                    return;
                                }
                            }

                            boolean d = inner.isDone();

                            boolean empty = q.isEmpty();

                            if (d && empty) {
                                inner = null;
                                current = null;
                                upstream.request(1);
                                continueNextSource = true;
                            }
                        }
                    }
                }

                if (e != 0L && r != Long.MAX_VALUE) {
                    requested.addAndGet(-e);
                }

                if (continueNextSource) {
                    continue;
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
}
