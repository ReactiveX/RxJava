/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.operators;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import rx.*;
import rx.exceptions.Exceptions;
import rx.functions.Func1;
import rx.internal.operators.OnSubscribeTimeoutTimedWithFallback.FallbackSubscriber;
import rx.internal.producers.ProducerArbiter;
import rx.internal.subscriptions.SequentialSubscription;
import rx.plugins.RxJavaHooks;

/**
 * Switches to the fallback Observable if: the first upstream item doesn't arrive before
 * the first timeout Observable signals an item or completes; or the Observable generated from
 * the previous upstream item signals its item or completes before the upstream signals the next item
 * of its own.
 *
 * @param <T> the input and output value type
 * @param <U> the value type of the first timeout Observable
 * @param <V> the value type of the item-based timeout Observable
 *
 * @since 1.3.3
 */
public final class OnSubscribeTimeoutSelectorWithFallback<T, U, V> implements Observable.OnSubscribe<T> {

    final Observable<T> source;

    final Observable<U> firstTimeoutIndicator;

    final Func1<? super T, ? extends Observable<V>> itemTimeoutIndicator;

    final Observable<? extends T> fallback;

    public OnSubscribeTimeoutSelectorWithFallback(Observable<T> source,
            Observable<U> firstTimeoutIndicator,
            Func1<? super T, ? extends Observable<V>> itemTimeoutIndicator,
            Observable<? extends T> fallback) {
        this.source = source;
        this.firstTimeoutIndicator = firstTimeoutIndicator;
        this.itemTimeoutIndicator = itemTimeoutIndicator;
        this.fallback = fallback;
    }

    @Override
    public void call(Subscriber<? super T> t) {
        TimeoutMainSubscriber<T> parent = new TimeoutMainSubscriber<T>(t, itemTimeoutIndicator, fallback);
        t.add(parent.upstream);
        t.setProducer(parent.arbiter);
        parent.startFirst(firstTimeoutIndicator);
        source.subscribe(parent);
    }

    static final class TimeoutMainSubscriber<T> extends Subscriber<T> {

        final Subscriber<? super T> actual;

        final Func1<? super T, ? extends Observable<?>> itemTimeoutIndicator;

        final Observable<? extends T> fallback;

        final ProducerArbiter arbiter;

        final AtomicLong index;

        final SequentialSubscription task;

        final SequentialSubscription upstream;

        long consumed;

        TimeoutMainSubscriber(Subscriber<? super T> actual,
                Func1<? super T, ? extends Observable<?>> itemTimeoutIndicator,
                Observable<? extends T> fallback) {
            this.actual = actual;
            this.itemTimeoutIndicator = itemTimeoutIndicator;
            this.fallback = fallback;
            this.arbiter = new ProducerArbiter();
            this.index = new AtomicLong();
            this.task = new SequentialSubscription();
            this.upstream = new SequentialSubscription(this);
            this.add(task);
        }


        @Override
        public void onNext(T t) {
            long idx = index.get();
            if (idx == Long.MAX_VALUE || !index.compareAndSet(idx, idx + 1)) {
                return;
            }

            Subscription s = task.get();
            if (s != null) {
                s.unsubscribe();
            }

            actual.onNext(t);

            consumed++;

            Observable<?> timeoutObservable;

            try {
                timeoutObservable = itemTimeoutIndicator.call(t);
                if (timeoutObservable == null) {
                    throw new NullPointerException("The itemTimeoutIndicator returned a null Observable");
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                unsubscribe();
                index.getAndSet(Long.MAX_VALUE);
                actual.onError(ex);
                return;
            }

            TimeoutConsumer tc = new TimeoutConsumer(idx + 1);
            if (task.replace(tc)) {
                timeoutObservable.subscribe(tc);
            }

        }

        void startFirst(Observable<?> firstTimeoutIndicator) {
            if (firstTimeoutIndicator != null) {
                TimeoutConsumer tc = new TimeoutConsumer(0L);
                if (task.replace(tc)) {
                    firstTimeoutIndicator.subscribe(tc);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.unsubscribe();

                actual.onError(e);
            } else {
                RxJavaHooks.onError(e);
            }
        }

        @Override
        public void onCompleted() {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.unsubscribe();

                actual.onCompleted();
            }
        }

        @Override
        public void setProducer(Producer p) {
            arbiter.setProducer(p);
        }

        void onTimeout(long idx) {
            if (!index.compareAndSet(idx, Long.MAX_VALUE)) {
                return;
            }

            unsubscribe();

            if (fallback == null) {
                actual.onError(new TimeoutException());
            } else {
                long c = consumed;
                if (c != 0L) {
                    arbiter.produced(c);
                }

                FallbackSubscriber<T> fallbackSubscriber = new FallbackSubscriber<T>(actual, arbiter);

                if (upstream.replace(fallbackSubscriber)) {
                    fallback.subscribe(fallbackSubscriber);
                }
            }
        }

        void onTimeoutError(long idx, Throwable ex) {
            if (index.compareAndSet(idx, Long.MAX_VALUE)) {
                unsubscribe();

                actual.onError(ex);
            } else {
                RxJavaHooks.onError(ex);
            }

        }

        final class TimeoutConsumer extends Subscriber<Object> {

            final long idx;

            boolean done;

            TimeoutConsumer(long idx) {
                this.idx = idx;
            }

            @Override
            public void onNext(Object t) {
                if (!done) {
                    done = true;
                    unsubscribe();
                    onTimeout(idx);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!done) {
                    done = true;
                    onTimeoutError(idx, e);
                } else {
                    RxJavaHooks.onError(e);
                }
            }

            @Override
            public void onCompleted() {
                if (!done) {
                    done = true;
                    onTimeout(idx);
                }
            }
        }
    }
}
