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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class FlowableAmb<T> extends Flowable<T> {
    final Publisher<? extends T>[] sources;
    final Iterable<? extends Publisher<? extends T>> sourcesIterable;

    public FlowableAmb(Publisher<? extends T>[] sources, Iterable<? extends Publisher<? extends T>> sourcesIterable) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribeActual(Subscriber<? super T> s) {
        Publisher<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new Publisher[8];
            try {
                for (Publisher<? extends T> p : sourcesIterable) {
                    if (p == null) {
                        EmptySubscription.error(new NullPointerException("One of the sources is null"), s);
                        return;
                    }
                    if (count == sources.length) {
                        Publisher<? extends T>[] b = new Publisher[count + (count >> 2)];
                        System.arraycopy(sources, 0, b, 0, count);
                        sources = b;
                    }
                    sources[count++] = p;
                }
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                EmptySubscription.error(e, s);
                return;
            }
        } else {
            count = sources.length;
        }

        if (count == 0) {
            EmptySubscription.complete(s);
            return;
        } else
        if (count == 1) {
            sources[0].subscribe(s);
            return;
        }

        AmbCoordinator<T> ac = new AmbCoordinator<>(s, count);
        ac.subscribe(sources);
    }

    static final class AmbCoordinator<T> implements Subscription {
        final Subscriber<? super T> downstream;
        final AmbInnerSubscriber<T>[] subscribers;

        final AtomicInteger winner = new AtomicInteger();

        @SuppressWarnings("unchecked")
        AmbCoordinator(Subscriber<? super T> actual, int count) {
            this.downstream = actual;
            this.subscribers = new AmbInnerSubscriber[count];
        }

        public void subscribe(Publisher<? extends T>[] sources) {
            AmbInnerSubscriber<T>[] as = subscribers;
            int len = as.length;
            for (int i = 0; i < len; i++) {
                as[i] = new AmbInnerSubscriber<>(this, i + 1, downstream);
            }
            winner.lazySet(0); // release the contents of 'as'
            downstream.onSubscribe(this);

            for (int i = 0; i < len; i++) {
                if (winner.get() != 0) {
                    return;
                }

                sources[i].subscribe(as[i]);
            }
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                int w = winner.get();
                if (w > 0) {
                    subscribers[w - 1].request(n);
                } else
                if (w == 0) {
                    for (AmbInnerSubscriber<T> a : subscribers) {
                        a.request(n);
                    }
                }
            }
        }

        public boolean win(int index) {
            int w = winner.get();
            if (w == 0) {
                if (winner.compareAndSet(0, index)) {
                    AmbInnerSubscriber<T>[] a = subscribers;
                    int n = a.length;
                    for (int i = 0; i < n; i++) {
                        if (i + 1 != index) {
                            a[i].cancel();
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public void cancel() {
            if (winner.get() != -1) {
                winner.lazySet(-1);

                for (AmbInnerSubscriber<T> a : subscribers) {
                    a.cancel();
                }
            }
        }
    }

    static final class AmbInnerSubscriber<T> extends AtomicReference<Subscription> implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -1185974347409665484L;
        final AmbCoordinator<T> parent;
        final int index;
        final Subscriber<? super T> downstream;

        boolean won;

        final AtomicLong missedRequested = new AtomicLong();

        AmbInnerSubscriber(AmbCoordinator<T> parent, int index, Subscriber<? super T> downstream) {
            this.parent = parent;
            this.index = index;
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this, missedRequested, s);
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(this, missedRequested, n);
        }

        @Override
        public void onNext(T t) {
            if (won) {
                downstream.onNext(t);
            } else {
                if (parent.win(index)) {
                    won = true;
                    downstream.onNext(t);
                } else {
                    get().cancel();
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (won) {
                downstream.onError(t);
            } else {
                if (parent.win(index)) {
                    won = true;
                    downstream.onError(t);
                } else {
                    get().cancel();
                    RxJavaPlugins.onError(t);
                }
            }
        }

        @Override
        public void onComplete() {
            if (won) {
                downstream.onComplete();
            } else {
                if (parent.win(index)) {
                    won = true;
                    downstream.onComplete();
                } else {
                    get().cancel();
                }
            }
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
        }

    }
}
