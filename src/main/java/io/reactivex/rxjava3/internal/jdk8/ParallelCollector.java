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

package io.reactivex.rxjava3.internal.jdk8;

import java.util.Objects;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.Collector;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Reduces all 'rails' into a single via a Java 8 {@link Collector} callback set.
 *
 * @param <T> the value type
 * @param <A> the accumulator type
 * @param <R> the result type
 * @since 3.0.0
 */
public final class ParallelCollector<T, A, R> extends Flowable<R> {

    final ParallelFlowable<? extends T> source;

    final Collector<T, A, R> collector;

    public ParallelCollector(ParallelFlowable<? extends T> source, Collector<T, A, R> collector) {
        this.source = source;
        this.collector = collector;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        ParallelCollectorSubscriber<T, A, R> parent;
        try {
            parent = new ParallelCollectorSubscriber<>(s, source.parallelism(), collector);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }
        s.onSubscribe(parent);

        source.subscribe(parent.subscribers);
    }

    static final class ParallelCollectorSubscriber<T, A, R> extends DeferredScalarSubscription<R> {

        private static final long serialVersionUID = -5370107872170712765L;

        final ParallelCollectorInnerSubscriber<T, A, R>[] subscribers;

        final AtomicReference<SlotPair<A>> current = new AtomicReference<>();

        final AtomicInteger remaining = new AtomicInteger();

        final AtomicThrowable error = new AtomicThrowable();

        final Function<A, R> finisher;

        ParallelCollectorSubscriber(Subscriber<? super R> subscriber, int n, Collector<T, A, R> collector) {
            super(subscriber);
            this.finisher = collector.finisher();
            @SuppressWarnings("unchecked")
            ParallelCollectorInnerSubscriber<T, A, R>[] a = new ParallelCollectorInnerSubscriber[n];
            for (int i = 0; i < n; i++) {
                a[i] = new ParallelCollectorInnerSubscriber<>(this, collector.supplier().get(), collector.accumulator(), collector.combiner());
            }
            this.subscribers = a;
            remaining.lazySet(n);
        }

        SlotPair<A> addValue(A value) {
            for (;;) {
                SlotPair<A> curr = current.get();

                if (curr == null) {
                    curr = new SlotPair<>();
                    if (!current.compareAndSet(null, curr)) {
                        continue;
                    }
                }

                int c = curr.tryAcquireSlot();
                if (c < 0) {
                    current.compareAndSet(curr, null);
                    continue;
                }
                if (c == 0) {
                    curr.first = value;
                } else {
                    curr.second = value;
                }

                if (curr.releaseSlot()) {
                    current.compareAndSet(curr, null);
                    return curr;
                }
                return null;
            }
        }

        @Override
        public void cancel() {
            for (ParallelCollectorInnerSubscriber<T, A, R> inner : subscribers) {
                inner.cancel();
            }
        }

        void innerError(Throwable ex) {
            if (error.compareAndSet(null, ex)) {
                cancel();
                downstream.onError(ex);
            } else {
                if (ex != error.get()) {
                    RxJavaPlugins.onError(ex);
                }
            }
        }

        void innerComplete(A value, BinaryOperator<A> combiner) {
            for (;;) {
                SlotPair<A> sp = addValue(value);

                if (sp != null) {

                    try {
                        value = combiner.apply(sp.first, sp.second);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        innerError(ex);
                        return;
                    }

                } else {
                    break;
                }
            }

            if (remaining.decrementAndGet() == 0) {
                SlotPair<A> sp = current.get();
                current.lazySet(null);

                R result;
                try {
                    result = Objects.requireNonNull(finisher.apply(sp.first), "The finisher returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    innerError(ex);
                    return;
                }

                complete(result);
            }
        }
    }

    static final class ParallelCollectorInnerSubscriber<T, A, R>
    extends AtomicReference<Subscription>
    implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -7954444275102466525L;

        final ParallelCollectorSubscriber<T, A, R> parent;

        final BiConsumer<A, T> accumulator;

        final BinaryOperator<A> combiner;

        A container;

        boolean done;

        ParallelCollectorInnerSubscriber(ParallelCollectorSubscriber<T, A, R> parent, A container, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner) {
            this.parent = parent;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.container = container;
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.setOnce(this, s, Long.MAX_VALUE);
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                try {
                    accumulator.accept(container, t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    get().cancel();
                    onError(ex);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            container = null;
            done = true;
            parent.innerError(t);
        }

        @Override
        public void onComplete() {
            if (!done) {
                A v = container;
                container = null;
                done = true;
                parent.innerComplete(v, combiner);
            }
        }

        void cancel() {
            SubscriptionHelper.cancel(this);
        }
    }

    static final class SlotPair<T> extends AtomicInteger {

        private static final long serialVersionUID = 473971317683868662L;

        T first;

        T second;

        final AtomicInteger releaseIndex = new AtomicInteger();

        int tryAcquireSlot() {
            for (;;) {
                int acquired = get();
                if (acquired >= 2) {
                    return -1;
                }

                if (compareAndSet(acquired, acquired + 1)) {
                    return acquired;
                }
            }
        }

        boolean releaseSlot() {
            return releaseIndex.incrementAndGet() == 2;
        }
    }
}
