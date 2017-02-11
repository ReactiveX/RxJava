/**
 * Copyright 2017 Netflix, Inc.
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

import java.util.concurrent.atomic.*;

import rx.*;
import rx.exceptions.Exceptions;
import rx.functions.Func1;
import rx.internal.util.ExceptionsUtils;
import rx.plugins.RxJavaHooks;
import rx.subscriptions.CompositeSubscription;

/**
 * Maps upstream values to Completables and merges them, up to a given
 * number of them concurrently, optionally delaying errors.
 * @param <T> the upstream value type
 * @since 1.2.7 - experimental
 */
public final class OnSubscribeFlatMapCompletable<T> implements Observable.OnSubscribe<T> {

    final Observable<T> source;

    final Func1<? super T, ? extends Completable> mapper;

    final boolean delayErrors;

    final int maxConcurrency;

    public OnSubscribeFlatMapCompletable(Observable<T> source, Func1<? super T, ? extends Completable> mapper,
            boolean delayErrors, int maxConcurrency) {
        if (mapper == null) {
            throw new NullPointerException("mapper is null");
        }
        if (maxConcurrency <= 0) {
            throw new IllegalArgumentException("maxConcurrency > 0 required but it was " + maxConcurrency);
        }
        this.source = source;
        this.mapper = mapper;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    public void call(Subscriber<? super T> child) {
        FlatMapCompletableSubscriber<T> parent = new FlatMapCompletableSubscriber<T>(child, mapper, delayErrors, maxConcurrency);
        child.add(parent);
        child.add(parent.set);
        source.unsafeSubscribe(parent);
    }

    static final class FlatMapCompletableSubscriber<T> extends Subscriber<T> {

        final Subscriber<? super T> actual;

        final Func1<? super T, ? extends Completable> mapper;

        final boolean delayErrors;

        final int maxConcurrency;

        final AtomicInteger wip;

        final CompositeSubscription set;

        final AtomicReference<Throwable> errors;

        FlatMapCompletableSubscriber(Subscriber<? super T> actual, Func1<? super T, ? extends Completable> mapper,
                boolean delayErrors, int maxConcurrency) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.maxConcurrency = maxConcurrency;
            this.wip = new AtomicInteger(1);
            this.errors = new AtomicReference<Throwable>();
            this.set = new CompositeSubscription();
            this.request(maxConcurrency != Integer.MAX_VALUE ? maxConcurrency : Long.MAX_VALUE);
        }

        @Override
        public void onNext(T t) {
            Completable c;

            try {
                c = mapper.call(t);
                if (c == null) {
                    throw new NullPointerException("The mapper returned a null Completable");
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                unsubscribe();
                onError(ex);
                return;
            }

            InnerSubscriber inner = new InnerSubscriber();
            set.add(inner);
            wip.getAndIncrement();

            c.unsafeSubscribe(inner);
        }

        @Override
        public void onError(Throwable e) {
            if (delayErrors) {
                ExceptionsUtils.addThrowable(errors, e);
                onCompleted();
            } else {
                set.unsubscribe();
                if (errors.compareAndSet(null, e)) {
                    actual.onError(ExceptionsUtils.terminate(errors));
                } else {
                    RxJavaHooks.onError(e);
                }
            }
        }

        @Override
        public void onCompleted() {
            done();
        }

        boolean done() {
            if (wip.decrementAndGet() == 0) {
                Throwable ex = ExceptionsUtils.terminate(errors);
                if (ex != null) {
                    actual.onError(ex);
                } else {
                    actual.onCompleted();
                }
                return true;
            }
            return false;
        }

        public void innerError(InnerSubscriber inner, Throwable e) {
            set.remove(inner);
            if (delayErrors) {
                ExceptionsUtils.addThrowable(errors, e);
                if (!done() && maxConcurrency != Integer.MAX_VALUE) {
                    request(1);
                }
            } else {
                set.unsubscribe();
                unsubscribe();
                if (errors.compareAndSet(null, e)) {
                    actual.onError(ExceptionsUtils.terminate(errors));
                } else {
                    RxJavaHooks.onError(e);
                }
            }
        }

        public void innerComplete(InnerSubscriber inner) {
            set.remove(inner);
            if (!done() && maxConcurrency != Integer.MAX_VALUE) {
                request(1);
            }
        }

        final class InnerSubscriber
        extends AtomicReference<Subscription>
        implements CompletableSubscriber, Subscription {

            private static final long serialVersionUID = -8588259593722659900L;

            @Override
            public void unsubscribe() {
                Subscription s = getAndSet(this);
                if (s != null && s != this) {
                    s.unsubscribe();
                }
            }

            @Override
            public boolean isUnsubscribed() {
                return get() == this;
            }

            @Override
            public void onCompleted() {
                innerComplete(this);
            }

            @Override
            public void onError(Throwable e) {
                innerError(this, e);
            }

            @Override
            public void onSubscribe(Subscription d) {
                if (!compareAndSet(null, d)) {
                    d.unsubscribe();
                    if (get() != this) {
                        RxJavaHooks.onError(new IllegalStateException("Subscription already set!"));
                    }
                }
            }
        }
    }
}
